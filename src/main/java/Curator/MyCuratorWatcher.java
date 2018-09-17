package Curator;

import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;

public class MyCuratorWatcher implements CuratorWatcher {

	@Override
	public void process(WatchedEvent event) throws Exception {
		System.out.println("usingWatcher触发CuratorWatcher，节点路径为：" + event.getPath());
	}

}
