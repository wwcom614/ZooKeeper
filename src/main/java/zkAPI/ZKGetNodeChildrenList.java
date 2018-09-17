package zkAPI;

import lombok.Data;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

//getChildren获取当前节点的子节点列表
@Data
public class ZKGetNodeChildrenList implements Watcher {
    final static Logger log = LoggerFactory.getLogger(ZKGetNodeChildrenList.class);
    private ZooKeeper zookeeper = null;

    public static final String zkServerIpPort = "127.0.0.1:2181";
    public static final Integer sessionTimeout = 5000;
    private static Stat stat = new Stat();

    public ZKGetNodeChildrenList() {
    }

    public ZKGetNodeChildrenList(String zkServerIpPort) {
        try {
            zookeeper = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKGetNodeChildrenList());
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

    //为了能看到get放置的watch的效果，使用下CountDownLatch。
    // 另外一个客户端改动数据时，会触发watch事件，到process处理
    private static CountDownLatch countDown = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {

        ZKGetNodeChildrenList zkServer = new ZKGetNodeChildrenList(zkServerIpPort);

        /**
         * 参数：
         * path：父节点路径
         * watch：true或者false，注册一个watch事件
         */

/*        //同步方式，直接返回子节点列表
        List<String> strChildList = zkServer.getZookeeper().getChildren("/testnode", true);
        for (String childNode : strChildList) {
            log.warn("Current children list:"+ childNode);
        }*/

        // 异步方式
        String ctx = "{'callback':'NodeChildrenListCallBack'}";
		zkServer.getZookeeper().getChildren("/testnode", true, new NodeChildrenListCallBack(), ctx);

        countDown.await();
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            if (event.getType() == Event.EventType.NodeChildrenChanged) {
                log.warn("NodeChildrenChanged");
                ZKGetNodeChildrenList zkServer = new ZKGetNodeChildrenList(zkServerIpPort);
                List<String> strChildList = zkServer.getZookeeper().getChildren(event.getPath(), false);
                log.warn("Children list has been changed to:");
                for (String child : strChildList) {
                    log.warn(child);
                }
                countDown.countDown();
            } else if (event.getType() == Event.EventType.NodeCreated) {
                log.warn("NodeCreated");
            } else if (event.getType() == Event.EventType.NodeDataChanged) {
                log.warn("NodeDataChanged");
            } else if (event.getType() == Event.EventType.NodeDeleted) {
                log.warn("NodeDeleted");
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
