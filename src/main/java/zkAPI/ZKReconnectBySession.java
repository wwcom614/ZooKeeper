package zkAPI;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZKReconnectBySession implements Watcher{
    //在ZKConnect.java的基础上，尝试下会话重连服务端

    final static Logger log = LoggerFactory.getLogger(ZKReconnectBySession.class);

    public static final String zkServerIpPort = "127.0.0.1:2181";
    public static final Integer sessionTimeout = 5000;

    public static void main(String[] args) throws Exception {

        ZooKeeper zk = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKReconnectBySession());

        //我想生产环境中，获取到的sessionId和sessionPassword，应该保存到session或者redis中
        long sessionId = zk.getSessionId();
        byte[] sessionPassword = zk.getSessionPasswd();

        //如下只是为了转码打印sessionId看看
        //服务端使用四字命令 echo dump | nc localhost 2181 看到sessionId与此一致
        String ssid = "0x" + Long.toHexString(sessionId);
        log.warn("sessionId = {}",ssid);

        log.warn("客户端开始连接zookeeper服务器...");
        log.warn("连接状态：{}", zk.getState());
        new Thread().sleep(2000);
        log.warn("连接状态：{}", zk.getState());

        new Thread().sleep(2000);

        // 开始会话重连
        log.warn("开始会话重连...");

        ZooKeeper zkSession = new ZooKeeper(zkServerIpPort,
                sessionTimeout,
                new ZKReconnectBySession(),
                sessionId,
                sessionPassword);
        log.warn("重新连接状态zkSession：{}", zkSession.getState());
        new Thread().sleep(2000);
        log.warn("重新连接状态zkSession：{}", zkSession.getState());
    }

    @Override
    public void process(WatchedEvent event) {
        log.warn("收到连接成功watcher通知：{}，开始逻辑处理！", event);
    }
}
