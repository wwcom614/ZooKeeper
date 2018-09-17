package zkAPI;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//首次尝试使用ZooKeeper的API连接ZooKeeper服务端
public class ZKConnect implements Watcher {

    final static Logger log = LoggerFactory.getLogger(ZKConnect.class);

    public static final String zkServerIpPort = "127.0.0.1:2181";
    //ZooKeeper分布式集群连接
    //public static final String zkServerIpPort = "192.168.1.1:2181,192.168.1.2:2182,192.168.3.111:2183";
    public static final Integer sessionTimeout = 5000;

    public static void main(String[] args) throws Exception {
        /**
         * 客户端和zk服务端链接是一个异步的过程
         * 当连接成功后后，客户端会收的一个watcher通知事件，客户端必须重写对该事件的处理(下方process方法)
         *
         * 参数：
         * connectString：连接服务器的ip字符串，
         * 		比如: "192.168.1.1:2181,192.168.1.2:2181,192.168.1.3:2181"
         * 		可以是一个ip，也可以是多个ip，一个ip代表单机，多个ip代表集群
         * 		也可以在ip后加路径
         * sessionTimeout：超时时间，服务端多久收不到客户端心跳，就超时
         * watcher：通知事件，如果有对应的事件触发，则会收到一个通知；如果不需要，那就设置为null
         * canBeReadOnly：可只读，当这个物理机节点断开后，还是可以读到数据的，只是不能写，
         * 					       此时数据被读取到的可能是旧数据，此处建议设置为false，不推荐使用
         * sessionId：会话的id
         * sessionPasswd：会话密码	当会话丢失后，可以依据 sessionId 和 sessionPasswd 重新获取会话
         */
        ZooKeeper zk = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKConnect());
        //watcher直接设置成自己，这样可以在本类中process方法获取到事件

        log.warn("客户端开始连接zookeeper服务器...");
        log.warn("连接状态：{}", zk.getState());

        //sleep是因为连接服务端需要时间，可能1.5秒之类的，也就是先connecting到最后是connected需要时间
        new Thread().sleep(2000);

        log.warn("连接状态：{}", zk.getState());
    }

    @Override
    public void process(WatchedEvent event) {
        log.warn("收到连接成功watcher通知：{}，开始逻辑处理！", event);
    }
}
