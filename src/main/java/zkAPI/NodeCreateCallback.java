package zkAPI;

import org.apache.zookeeper.AsyncCallback.StringCallback;

//ZKNodeCreateAsync的回调函数
public class NodeCreateCallback implements StringCallback{
    @Override
    public void processResult(int rc, String path, Object ctx, String name) {
        System.out.println("Async Create Znode: " + path);
        System.out.println((String)ctx);
    }
}

