package zkAPI;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//delete删除zNode节点，同步方式的方法没返回结果。
// 尝试异步方式：请求后不等服务端响应，调用callback函数NodeCreateCallback.java，可传递ctx对象给callback函数处理
public class ZKNodeDeleteAsync implements Watcher {
    final static Logger log = LoggerFactory.getLogger(ZKNodeDeleteAsync.class);

    public static final String zkServerIpPort = "127.0.0.1:2181";
    public static final Integer sessionTimeout = 5000;

    public static void main(String[] args) throws Exception {

        ZooKeeper zk = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKNodeDeleteAsync());

        try {


            String ctx = "{'Async Delete Znode': 'VoidCallbak'}";//回调函数传参，不限于String，可以传对象
            zk.delete("/AsyncNode", 1, new NodeDeleteCallback(), ctx);
            // 异步方式要等一下，否则程序很快执行结束了，而服务端异步回调还没返回
            new Thread().sleep(2000);
        } catch (Exception e) {
            log.warn("Async Delete Znode: Fail...");
        }
    }

    @Override
    public void process(WatchedEvent event) {

    }
}