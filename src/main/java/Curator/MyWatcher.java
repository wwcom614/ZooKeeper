package Curator;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class MyWatcher implements Watcher {

	@Override
	public void process(WatchedEvent event) {
		System.out.println("usingWatcher触发ZooKeeper的原生watcher，节点路径为：" + event.getPath());
	}


}
