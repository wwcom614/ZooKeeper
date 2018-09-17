学习了ZooKeeper的相关特性、原理、应用场景，搭建并启动ZooKeeper服务端(单机、伪分布式、集群)并熟悉了其配置，
客户端连接，学习了其常用操作命令及参数(create、get、set、delete)、
数据版本号(乐观锁)、watcher、Acl、四字命令(需安装nc)等，开始动手编码实践，先实践了ZooKeeper原生的API。

## zkAPI 
学习并实践了ZooKeeper原生的API的常用操作。  
maven引入ZooKeeper的相关Jar包。  
ZooKeeper的conf/log4j.properties拷贝到resources文件夹，
修改zookeeper.root.logger=WARN, CONSOLE，这样看日志只有自己打印的，清爽。  

- ZKConnect.java：     
客户端和zk服务端链接是一个异步的过程，当连接成功后后，客户端会收的一个watcher通知事件，
客户端必须重写对该事件的处理--process方法。   
客户端开始连接zookeeper服务器...  
连接状态：CONNECTING  
收到连接成功watcher通知：WatchedEvent state:SyncConnected type:None path:null，开始逻辑处理！  
连接状态：CONNECTED  

- ZKReconnectBySession.java：  
在ZKConnect.java基础上，尝试下会话重连：  
在连接服务端时，获取并记录sessionId和sessionPassword(生产环境中，这应该保存到session或者redis中)。
下一次连接时，作为new ZooKeeper方法的入参，这样就会话重连了。  
sessionId = 0x0  
开始会话重连...  
重新连接状态zkSession：CONNECTING  
收到连接成功watcher通知：WatchedEvent state:SyncConnected type:None path:null，开始逻辑处理！  
重新连接状态zkSession：CONNECTED  

- ZKNodeCreateSync.java  
create创建zNode节点，同步方式：请求后要等服务端响应，创建的节点名称直接返回到result。  
Acl参数先使用Ids.OPEN_ACL_UNSAFE --> world:anyone:cdrwa，生产环境不能用，后面单独尝试Acl。  
createMode参数尝试了EPHEMERAL：临时节点，程序结束后客户端断链，查看服务端，该节点被自动删除。  
Sync Create Znode: 	/SyncNode	Success...  

- ZKNodeCreateAsync.java, NodeCreateCallback.java  
create创建zNode节点，异步方式：请求后不等服务端响应，调用callback函数NodeCreateCallback.java，可传递ctx对象给callback函数处理。  
异步方式要等一下，否则程序很快执行结束了，而服务端异步回调还没返回。  
createMode参数尝试了PERSISTENT：永久节点，程序结束后客户端断链，查看服务端，该节点还在。   
Async Create Znode: /AsyncNode  
{'Async Create Znode': 'Success'}  
  
-  ZKNodeModifySync.java  
setData修改zNode节点，同样也有同步和异步2种方式，和Create方法差不多，所以仅尝试同步方式了，主要想尝试下乐观锁(数据version)。  
setData的version参数对应ZooKeeper服务端的dataVersion字段，两种要一致才能修改数据成功。  
修改数据成功后，ZooKeeper服务端的dataVersion字段自增1。  
节点的各种属性都可以通过status获取，status.getVersion()是修改后已自增1的版本号。  
setData Znode: Success， dataVersion=2

- ZKNodeDeleteAsync.java, NodeDeleteCallback.java  
delete删除zNode节点，同步方式的方法是没有返回值的。 尝试异步方式：请求后不等服务端响应，调用callback函数NodeCreateCallback.java，可传递ctx对象给callback函数处理。 
delete的version参数对应ZooKeeper服务端的dataVersion字段，两种要一致才能删除数据成功。  

- ZKNodeGetData.java  
getData获取当前节点数据，并设置watcher。使用CountDownLatch让主线程main等待countDown.await()。  
用另外的客户端set修改数据，触发回调方法process，重新获取数据和更新后的dateVersion。  
Current data:test1  
Current data has been changed to:test2  
dateVersion has been changed to：2

- ZKGetChildrenList.java, NodeChildrenListCallBack.java  
getChildren获取当前节点的子节点列表，并设置watcher。使用CountDownLatch让主线程main等待countDown.await()。   
用另外的客户端增加、删除子节点，触发回调方法process，重新获取数据和更新后的dateVersion。  
getChildren同步方式，直接返回子节点列表。  
getChildren异步方式，回调函数NodeChildrenListCallBack implements Children2Callback，
可以获取到stat的信息很丰富。    

- ZKNodeExist.java  
exists查询zNode节点是否存在exists。  
zNode exists，dataVersion：2  

- ZKNodeAcl.java  
专门实践下auth，digest和ip的登录鉴权以及cdrwa的权限控制。  
1.Digest用户鉴权方式：  
DigestAuthenticationProvider.generateDigest可以将"user:password"字符串转换成acl所需的Id类型。  
Id ww1AclId = new Id("digest", DigestAuthenticationProvider.generateDigest("ww1:ww1"));    
2.IP鉴权方式：  
Id ipId1 = new Id("ip", "127.0.0.2");  
3.cdr权限：  
acls.add(new ACL(ZooDefs.Perms.READ |ZooDefs.Perms.DELETE, ww2AclId));  
4.注册过的用户必须通过addAuthInfo后才能操作节点，与ZooKeeper命令addauth相同。    
zkServer.getZookeeper().addAuthInfo("digest", "ww2:ww2".getBytes());


## Curator  
实践zk原生API后感觉有几处不便：  
1.超时的情况下，不支持自动重连，需要手动操作。  
2.和命令行一样，Watcher注册一次就失效了。  
3.不支持递归创建节点，得单独先建立父节点。 
4.原生API感觉写起来还是挺繁琐。   
网上搜索Apache的开源项目Curator，能解决上述问题。而且还提供了很多解决方案，例如分布式锁。  

-  CuratorOperate.java  
基于Curator的API，实践基本的连接、增删改查、事件监听的操作。  
**1.连接:很清爽简洁，支持多种断链自动重连策略，给力!**   
namespace("ww_workspace")是操作的空间目录名，后续所有建立节点等操作都是在此基础上进行，而不是zk的根节点。  
retryPolicy设置断链重连策略，有很多种可供选择。  
client = CuratorFrameworkFactory.builder()  
                .connectString(zkServerIpPort)  
                .sessionTimeoutMs(10000).retryPolicy(retryPolicy2)  
                .namespace("ww_workspace").build();  
        client.start();  
**2.创建节点create：**    
creatingParentsIfNeeded，可以递归创建节点，很好。  
		cto.client.create().creatingParentsIfNeeded()  
			.withMode(CreateMode.PERSISTENT)  
			.withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)  
			.forPath(nodePath, createData);       
**3.更新节点数据setData：**    
cto.client.setData().withVersion(0).forPath(nodePath, setData);  
**4.判断节点是否存在checkExists,如果不存在为null；如果存在返回节点信息。**   
Stat statNotExist = cto.client.checkExists().forPath(nodePath + "/abc");  
**5.查询节点数据getData：**    
storingStatIn可以在get节点数据时，把节点信息获取到。  
byte[] getData = cto.client.getData().storingStatIn(stat).forPath(nodePath);   
**6.查询子节点getChildren：**    
List<String> childNodes = cto.client.getChildren().forPath(nodePath);  
**7.使用usingWatcher，监听watcher事件只会触发一次，监听完毕后就销毁，这和Zk原生API一样，不好用。**    
cto.client.getData().usingWatcher(new MyCuratorWatcher()).forPath(nodePath);  
**8.curator的NodeCache可以实现：为节点添加1次watcher一次，后面可以一直监听该节点的变更！**    
NodeCache的最后一个参数datasCompressed是对节点数据是否压缩的意思。  
NodeCache的缺点是无法区分create、set和delete操作，都是nodeChanged。  
start方法的参数buildInitial一般置为true，这样初始化的时候，可以获取到该nodePath的值并且缓存。  
从nodeCache使用getListenable()获取监听器list，然后将自己的监听器NodeCacheListener使用addListener加进去；然后实现nodeChanged方法。  
**9.curator的PathChildrenCache可以实现：为子节点添加watcher。**    
PathChildrenCache: 子节点监听，并且可以区分子节点的增、删、改操作，会触发事件。  
cacheData: 是否缓存该节点的数据状态信息stat。  
StartMode: 3种初始化方式：  
POST_INITIALIZED_EVENT：常用，异步初始化，初始化之后会触发PathChildrenCacheEvent.Type.INITIALIZED事件。  
NORMAL：仅异步初始化，不会触发PathChildrenCacheEvent.Type.INITIALIZED事件。  
上述2种异步方式，childrenCache不会获取到节点信息，而是靠addListener的childEvent。  
BUILD_INITIAL_CACHE：同步初始化，childrenCache会立即获取到节点信息。

- Config_Sync_Client.java, Config_Bean.java, Config_Sync_Test_Command.txt   
基于ZooKeeper实战了分布式配置同步功能。  
1.基于curator的PathChildrenCache实现：为子节点添加watcher。   
2.childrenCache.getListenable().addListener添加监听事件childEvent。  
3.监听指定znode节点path下的“子节点”变化PathChildrenCacheEvent.Type.CHILD_UPDATED。    
4.“子节点”变化时，Config_Sync_Client.java收到event事件通知，并获取变动后的数据event.getData().getData()。    
5.该数据是一个json串，见Config_Sync_Test_Command.txt，与Config_Bean.java的定义对应。 
6.使用fastjson将String转换为Config_Bean格式的对象，然后逐一获取属性值。  
7.其中一个属性值change_type是本次变动的增删改类型，分支判断，进行相应逻辑处理，并记录日志，以备后续运维查询和统计。  
8.特点在于上述client可以启动多个，每个都可以收到事件通知，然后各自处理。  

- CuratorAcl.java  
尝试使用Curator设置ACL并测试验证。  
CuratorFrameworkFactory连接时，支持authorization登录schema+用户，也可以是AuthInfos的List。  
List<ACL>的使用与ZooKeeper的原生API一样，create()的时候，可以withACL(acls)赋予ACL权限。  
withACL的第2个参数为false，表明仅仅对本次创建的节点设置ACL，一般这样用即可，防止多人使用时因权限设置不当造成生产事故。  
withACL的第2个参数为true，表明对创建的父节点也要设置相同的ACL。安全性要求高的系统使用。
此时需注意CuratorFrameworkFactory连接时，需authorization登录有create创建权限的用户才能操作。
withACL的第2个参数为true，已存在的父节点不会被更新权限，不受影响。  
继续使用getData、setData、delete分别测试r/w/d权限。  
赋值ACL权限时如果节点不存在，会抛NoNodeException，cdrw操作节点时如果没权限会抛NoAuthException，生产环境记得catch。  

- Distributed_Lock.java  
基于ZooKeeper实战了分布式锁功能。    
init(): 客户端调用者使用该方法连接ZooKeeper服务端，判断所有分布式锁的父节点如果不存在则创建，在该父节点addWatcherToLock()监听。  
addWatcherToLock(): 在所有分布式锁的父节点parent_lock_path上，基于PathChildrenCache加Watcher监听，
其子节点，该项目的分布式锁被删除时，能监听到该事件。释放锁计数器waitLockLatch，本次请求尝试获取分布式锁。  
getLock():客户端使用该方法循环尝试获得分布式锁，直到成功获得。注意：每个项目的分布式锁需要设置成临时节点withMode(CreateMode.EPHEMERAL)，
这样该连接断链后，ZooKeeper服务端可以自动删除该分布式锁节点，避免死锁。
如果没获取到分布式锁，检查CountDownLatch是否需要重新设置。阻塞本线程，等待其他请求释放分布式锁后的watcher通知。  
releaseLock():获取分布式锁的客户端，当完成业务处理后，或者各异常的finally中，务必需要调用本方法释放分布式锁，避免死锁。

 
 


  


