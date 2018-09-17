package zkAPI;

import org.apache.zookeeper.AsyncCallback.Children2Callback;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class NodeChildrenListCallBack implements Children2Callback {
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat) {
        System.out.println("Current children list:");
        for (String child : children) {
            System.out.println(child);
        }
        System.out.println("NodeChildrenListCallBack:" + path);
        System.out.println("ctx:" + ctx);
        System.out.println("dataVersion:" + stat.getVersion());
    }
}
