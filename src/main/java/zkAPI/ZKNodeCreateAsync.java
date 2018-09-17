package zkAPI;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//create创建zNode节点，异步方式：请求后不等服务端响应，调用callback函数NodeCreateCallback.java，可传递ctx对象给callback函数处理
public class ZKNodeCreateAsync implements Watcher {
    final static Logger log = LoggerFactory.getLogger(ZKNodeCreateAsync.class);

    public static final String zkServerIpPort = "127.0.0.1:2181";
    public static final Integer sessionTimeout = 5000;

    public static void main(String[] args) throws Exception {

        ZooKeeper zk = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKNodeCreateAsync());

        try {
            /**
             * 不支持子节点的递归创建，异步比同步多一个callback函数
             * 参数：
             * path：创建的路径
             * data：存储的数据的byte[]
             * acl：控制权限策略
             * 			Ids.OPEN_ACL_UNSAFE --> world:anyone:cdrwa，生产环境不能用
             * 			CREATOR_ALL_ACL --> auth:user:password:cdrwa
             * createMode：节点类型, 是一个枚举
             * 			PERSISTENT：持久节点
             * 			PERSISTENT_SEQUENTIAL：持久顺序节点
             * 			EPHEMERAL：临时节点，程序结束后客户端断链，查看服务端，该节点被自动删除
             * 			EPHEMERAL_SEQUENTIAL：临时顺序节点
             */
            String ctx = "{'Async Create Znode': 'Success'}";//回调函数传参，不限于String，可以传对象
            zk.create("/AsyncNode", "AsyncNode".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, new NodeCreateCallback(), ctx);
            // 异步方式要等一下，否则程序很快执行结束了，而服务端异步回调还没返回
            new Thread().sleep(2000);
        } catch (Exception e) {
            log.warn("Async Create Znode: Fail...");
        }
    }

    @Override
    public void process(WatchedEvent event) {

    }
}