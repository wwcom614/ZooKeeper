package zkAPI;

import lombok.Data;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

//exists查询zNode节点是否存在exists
@Data
public class ZKNodeExist implements Watcher {

    final static Logger log = LoggerFactory.getLogger(ZKNodeCreateSync.class);

    private ZooKeeper zookeeper = null;

    public static final String zkServerIpPort = "127.0.0.1:2181";
    public static final Integer sessionTimeout = 5000;

    public ZKNodeExist() {
    }

    public ZKNodeExist(String zkServerIpPort) {
        try {
            zookeeper = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKNodeExist());
        } catch (IOException e) {
            e.printStackTrace();
            if (zookeeper != null) {
                try {
                    zookeeper.close();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private static CountDownLatch countDown = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {

        ZKNodeExist zkServer = new ZKNodeExist(zkServerIpPort);

        /**
         * 参数：
         * path：节点路径
         * watch：watch
         */
        Stat stat = zkServer.getZookeeper().exists("/testnode", true);
        if (stat != null) {
            log.warn("zNode exists，dataVersion：" + stat.getVersion());
        } else {
            log.warn("zNode doesn't exist...");
        }

        countDown.await();
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == EventType.NodeCreated) {
            log.warn("NodeCreated");
            countDown.countDown();
        } else if (event.getType() == EventType.NodeDataChanged) {
            log.warn("NodeDataChanged");
            countDown.countDown();
        } else if (event.getType() == EventType.NodeDeleted) {
            log.warn("NodeDeleted");
            countDown.countDown();
        }
    }


}

