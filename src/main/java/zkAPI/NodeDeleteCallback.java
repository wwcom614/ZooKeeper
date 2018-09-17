package zkAPI;

import org.apache.zookeeper.AsyncCallback.VoidCallback;

//ZKNodeDeleteAsync的回调函数
public class NodeDeleteCallback implements VoidCallback{
    @Override
    public void processResult(int rc, String path, Object ctx) {
        System.out.println("Async Delete Znode: " + path);
        System.out.println((String)ctx);
    }
}


