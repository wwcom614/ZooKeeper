package zkAPI;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//setData修改zNode节点，同样也有同步和异步2种方式，和Create方法差不多，所以先仅尝试同步方式了，主要想尝试下乐观锁(数据version)
public class ZKNodeModifySync implements Watcher {
    final static Logger log = LoggerFactory.getLogger(ZKNodeModifySync.class);

    public static final String zkServerIpPort = "127.0.0.1:2181";
    public static final Integer sessionTimeout = 5000;

    public static void main(String[] args) throws Exception {

        ZooKeeper zk = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKNodeModifySync());

        try {
            //setData的version参数对应ZooKeeper服务端的dataVersion字段，两种要一致才能修改数据成功。
            // 修改数据成功后，ZooKeeper服务端的dataVersion字段自增1
            Stat status = zk.setData("/AsyncNode", "modify1".getBytes(), 1);
            // 节点的各种属性都可以通过status获取，取了个数据版本号尝试一下，是修改后已自增1的版本号
            log.warn("setData Znode: Success， dataVersion=" + status.getVersion() );
        } catch (Exception e) {
            log.warn("setData Znode: Fail...");
        }
    }

    @Override
    public void process(WatchedEvent event) {

    }
}