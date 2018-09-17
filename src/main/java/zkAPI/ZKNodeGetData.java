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
import java.util.concurrent.CountDownLatch;

//getData获取当前节点数据，并设置watcher。使用CountDownLatch让主线程main等待countDown.await()。
//用另外的客户端set修改数据，触发回调方法process，重新获取数据和更新后的dateVersion。
@Data
public class ZKNodeGetData implements Watcher {
    final static Logger log = LoggerFactory.getLogger(ZKNodeGetData.class);
    private ZooKeeper zookeeper = null;

    public static final String zkServerIpPort = "127.0.0.1:2181";
    public static final Integer sessionTimeout = 5000;
    private static Stat stat = new Stat();

    public ZKNodeGetData() {
    }

    public ZKNodeGetData(String zkServerIpPort) {
        try {
            zookeeper = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKNodeGetData());
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

        ZKNodeGetData zkServer = new ZKNodeGetData(zkServerIpPort);

        /**
         * 参数：
         * path：zNode节点路径
         * watch：true或者false，注册一个watch事件
         * stat：状态
         */
        byte[] resByte = zkServer.getZookeeper().getData("/testnode", true, stat);
        String result = new String(resByte);
        log.warn("Current data:" + result);
        //使用countDown.await先挂起线程别结束掉，这样后面改该节点数据时，watch捕获到event，process处理
        countDown.await();
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            if (event.getType() == Event.EventType.NodeDataChanged) {
                ZKNodeGetData zkServer = new ZKNodeGetData(zkServerIpPort);
                byte[] resByte = zkServer.getZookeeper().getData("/testnode", false, stat);
                String result = new String(resByte);
                log.warn("Current data has been changed to:" + result);
                log.warn("dateVersion has been changed to：" + stat.getVersion());
                //CountDownLatch减为0，countDown.await()的main线程继续执行，结束。
                countDown.countDown();
            } else if (event.getType() == Event.EventType.NodeCreated) {
                log.warn("NodeCreated");
            } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
                log.warn("NodeChildrenChanged");
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

