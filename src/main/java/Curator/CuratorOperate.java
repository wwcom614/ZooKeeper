package Curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.retry.*;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CuratorOperate {

    final static Logger log = LoggerFactory.getLogger(CuratorOperate.class);

    public CuratorFramework client = null;
    public static final String zkServerIpPort = "127.0.0.1:2181";

    /**
     * 构造方法，实例化zk客户端
     */
    public CuratorOperate() {
        /**
         * 同步创建zk，原生zkAPI是异步的
         * 原生zkAPI超时的情况下，不支持自动重连，需要手动操作
         * Curator解决了该问题，提供了多种RetryPolicy重连策略供选择使用
         */

		/*------------------------------------------------------------------------------------------*/
        /* curator连接zookeeper的策略类型1:ExponentialBackoffRetry，常用
         * baseSleepTimeMs：初始sleep的毫秒数
		 * maxRetries：最大重试次数
		 * maxSleepMs：最大重试毫秒数
		 */
        RetryPolicy retryPolicy1 = new ExponentialBackoffRetry(1000, 3, 9000);
		/*------------------------------------------------------------------------------------------*/

        /**
         * curator连接zookeeper的策略类型2:RetryNTimes，常用
         * n：最大重试次数
         * sleepMsBetweenRetries：每次重试间隔的毫秒数
         */
        RetryPolicy retryPolicy2 = new RetryNTimes(3, 3000);
		/*------------------------------------------------------------------------------------------*/

        /**
         * curator连接zookeeper的策略类型3:RetryUntilElapsed
         * maxElapsedTimeMs:最大重试的毫秒数，重试时间超过maxElapsedTimeMs后，就不再重试
         * sleepMsBetweenRetries:每次重试间隔时间		 *
         */
        RetryPolicy retryPolicy3 = new RetryUntilElapsed(9000, 3000);
		/*------------------------------------------------------------------------------------------*/

        /**
         * curator连接zookeeper的策略类型4:RetryOneTime，就重试1次，这应该不常用
         * sleepMsBetweenRetry:重试间隔的毫秒数
         */
        RetryPolicy retryPolicy4 = new RetryOneTime(3000);
        /*------------------------------------------------------------------------------------------*/

        /**
         * curator连接zookeeper的策略类型5：RetryForever，永远重试，生产环境肯定不推荐使用
         * retryIntervalMs:重试间隔的毫秒数
         */
		RetryPolicy retryPolicy5 = new RetryForever(3000);

        //namespace("ww_workspace")是操作的空间目录名，后续所有建立节点等操作都是在此基础上进行，而不是zk的根节点。
        //retryPolicy设置断链重连策略，有很多种可供选择
        client = CuratorFrameworkFactory.builder()
                .connectString(zkServerIpPort)
                .sessionTimeoutMs(10000).retryPolicy(retryPolicy2)
                .namespace("ww_workspace").build();
        client.start();
    }

    /**
     * @Description: 关闭zk客户端连接
     */
    public void closeZKClient() {
        if (client != null) {
            this.client.close();
        }
    }

    public static void main(String[] args) throws Exception {
        // 实例化
        CuratorOperate cto = new CuratorOperate();
        boolean isZkCuratorStarted = cto.client.isStarted();
        log.warn("当前客户端的连接状态：" + (isZkCuratorStarted ? "连接中" : "已关闭"));

        // 创建节点create
        String nodePath = "/ww1/a";
        String childNodePath1 = "/ww1/a/child1";
        String childNodePath2 = "/ww1/a/child2";
		byte[] createData = "create".getBytes();
		//creatingParentsIfNeeded，可以递归创建节点，很好。
		cto.client.create().creatingParentsIfNeeded()
			.withMode(CreateMode.PERSISTENT)
			.withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
			.forPath(nodePath, createData);

		//创建2个子节点。
        cto.client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                .forPath(childNodePath1, createData);

        cto.client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                .forPath(childNodePath2, createData);

        // 更新节点数据setData。
		byte[] setData = "setData".getBytes();
		cto.client.setData().withVersion(0).forPath(nodePath, setData);

        // 判断节点是否存在checkExists,如果不存在为null；如果存在返回节点信息。
        Stat statNotExist = cto.client.checkExists().forPath(nodePath + "/abc");
        log.warn(String.valueOf(statNotExist));

        Stat statExist = cto.client.checkExists().forPath(childNodePath1);
        log.warn(String.valueOf(statExist));

        // 查询节点数据getData。
        Stat stat = new Stat();
        // storingStatIn可以在get节点数据时，把节点信息获取到。
        byte[] getData = cto.client.getData().storingStatIn(stat).forPath(nodePath);
        log.warn("节点" + nodePath + "的数据为: " + new String(getData));
        log.warn("该节点的版本号为: " + stat.getVersion());

        // 查询子节点getChildren。
        List<String> childNodes = cto.client.getChildren()
                .forPath(nodePath);
        log.warn(nodePath + "的子节点：");
        for (String child : childNodes) {
            log.warn(child);
        }

        // 删除节点delete。
		cto.client.delete()
				  .guaranteed()					// 网络不好情况下，如果删除请求已抵达服务端，服务端会删除，直到删除成功
				  .deletingChildrenIfNeeded()	// 递归删除，如果有子节点，也一并删除
				  .withVersion(stat.getVersion())
				  .forPath(nodePath);

/*********************************************************************************************/
        // 使用usingWatcher，监听watcher事件只会触发一次，监听完毕后就销毁，这和Zk原生API一样，不好用
        //下面两者的区别是MyCuratorWatcher()可以抛异常，Zk原生的Watcher()不抛异常
		cto.client.getData().usingWatcher(new MyCuratorWatcher()).forPath(nodePath);
		cto.client.getData().usingWatcher(new MyWatcher()).forPath(nodePath);

/*********************************************************************************************/
        // curator的NodeCache可以实现：为节点添加1次watcher一次，后面可以一直监听该节点的变更！
        //NodeCache的最后一个参数datasCompressed是对节点数据是否压缩的意思
        //NodeCache的缺点是无法区分create、set和delete操作，都是nodeChanged
		final NodeCache nodeCache = new NodeCache(cto.client, nodePath,false);
		// start方法的参数buildInitial一般置为true，这样初始化的时候，可以获取到该nodePath的值并且缓存。
        // 如果设置为false将不会获取该nodePath的值。
        nodeCache.start(true);
		if (nodeCache.getCurrentData() != null) {
			log.warn("节点初始化时，获取到的数据为：" + new String(nodeCache.getCurrentData().getData()));
		} else {
            log.warn("节点初始化，buildInitial为false未获取数据。或buildInitial为true，获取数据为空...");
		}
		//从nodeCache使用getListenable()获取监听器list，然后将自己的监听器NodeCacheListener
        // 使用addListener加进去；然后实现nodeChanged方法。
        //PS：也可以使用removeListener()删除nodeCache中的监听器。
		nodeCache.getListenable().addListener(new NodeCacheListener() {
			public void nodeChanged() throws Exception {
				//考虑到delete节点时getCurrentData获取数据为null，增加判断保护避免空指针异常。
			    if (nodeCache.getCurrentData() == null) {
					log.warn("节点被删除或该数据数据为空");
					return;
				}
				// create当前节点、set当前节点数据时，nodeCache.getCurrentData().getData()可以获取到最新数据
				String data = new String(nodeCache.getCurrentData().getData());
                log.warn("节点路径：" + nodeCache.getCurrentData().getPath() + "最新数据：" + data);
			}
		});
/*********************************************************************************************/
        // curator的PathChildrenCache可以实现：为子节点添加watcher
        // PathChildrenCache: 子节点监听，并且可以区分子节点的增、删、改操作，会触发事件
        String childNodePathCache = nodePath;
        // cacheData: 是否缓存该节点的数据状态信息stat
        final PathChildrenCache childrenCache = new PathChildrenCache(cto.client, childNodePathCache, true);
        /**
         * StartMode: 3种初始化方式
         * POST_INITIALIZED_EVENT：常用，异步初始化，初始化之后会触发PathChildrenCacheEvent.Type.INITIALIZED事件。
         * NORMAL：仅异步初始化，不会触发PathChildrenCacheEvent.Type.INITIALIZED事件。
         * 上述2种异步方式，childrenCache不会获取到节点信息，而是靠addListener的childEvent。
         * BUILD_INITIAL_CACHE：同步初始化，childrenCache会立即获取到节点信息
         */
        childrenCache.start(StartMode.POST_INITIALIZED_EVENT);

        List<ChildData> childDataList = childrenCache.getCurrentData();
        log.warn("当前数据节点的子节点数据列表：");
        for (ChildData childData : childDataList) {
            log.warn(new String(childData.getData()));
        }

        childrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                //childrenCache的初始化方式为StartMode.POST_INITIALIZED_EVENT时，能监听到该事件
                if (event.getType().equals(PathChildrenCacheEvent.Type.INITIALIZED)) {
                    log.warn("子节点初始化ok...");
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                    String eventPath = event.getData().getPath();
                    //如果想只对指定子节点事件响应，事件触发的子节点path和想要监听的子节点path对比是否相同
                    if (eventPath.equals(SPECIFIC_PATH)) {
                        log.warn("添加子节点:" + event.getData().getPath());
                        log.warn("子节点数据:" + new String(event.getData().getData()));
                    } else if (eventPath.equals("/ww1/a/child2")) {
                        log.warn("添加不正确！");
                    }

                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                    log.warn("删除子节点:" + event.getData().getPath());
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
                    log.warn("被修改的子节点的路径是:" + event.getData().getPath());
                    log.warn("子节点数据被修改为:" + new String(event.getData().getData()));
                }
            }
        });

        Thread.sleep(9999999);
        cto.closeZKClient();
    }
    //想要监听的节点path，当该节点增、删、改时，与事件event路径比对是否一致
    public final static String SPECIFIC_PATH = "/ww1/a/child1";

}
