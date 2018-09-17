package zkAPI;

import lombok.Data;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//专门实践下，digest和ip的登录鉴权以及cdrwa的权限控制。
@Data
public class ZKNodeAcl implements Watcher {

	final static Logger log = LoggerFactory.getLogger(ZKNodeCreateSync.class);

	private ZooKeeper zookeeper = null;
	
	public static final String zkServerIpPort = "127.0.0.1:2181";
	public static final Integer sessionTimeout = 5000;
	
	public ZKNodeAcl() {}
	
	public ZKNodeAcl(String zkServerIpPort) {
		try {
			zookeeper = new ZooKeeper(zkServerIpPort, sessionTimeout, new ZKNodeAcl());
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
	
	public void createZKNode(String path, byte[] data, List<ACL> acls) {
		
		String result = "";
		try {
			/**
			 * 同步或者异步创建节点，都不支持子节点的递归创建，异步有一个callback函数
			 * 参数：
			 * path：创建的路径
			 * data：存储的数据的byte[]
			 * acl：控制权限策略
			 * 			Ids.OPEN_ACL_UNSAFE --> world:anyone:cdrwa
			 * 			CREATOR_ALL_ACL --> auth:user:password:cdrwa
			 * createMode：节点类型, 是一个枚举
			 * 			PERSISTENT：永久节点
			 * 			PERSISTENT_SEQUENTIAL：持久顺序节点
			 * 			EPHEMERAL：临时节点
			 * 			EPHEMERAL_SEQUENTIAL：临时顺序节点
			 */
			result = zookeeper.create(path, data, acls, CreateMode.PERSISTENT);
			log.warn("创建节点：\t" + result + "\t成功...");
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	
	public static void main(String[] args) throws Exception {
	
		ZKNodeAcl zkServer = new ZKNodeAcl(zkServerIpPort);
		
		/**
		 * ======================  创建node start  ======================  
		 */
		// acl 任何人都可以访问
		zkServer.createZKNode("/AclTest", "WorldAnyone".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE);
		
		// 自定义用户认证访问
		List<ACL> acls = new ArrayList<ACL>();
		//DigestAuthenticationProvider.generateDigest可以将"user:password"字符串转换成acl所需的Id类型
		Id ww1AclId = new Id("digest", DigestAuthenticationProvider.generateDigest("ww1:ww1"));
		Id ww2AclId = new Id("digest", DigestAuthenticationProvider.generateDigest("ww2:ww2"));

        /*
        cdrwa：有如下权限
        c-create子节点
        d-delete子节点
        r-get节点或子节点
        w-set当前节点数据
        a-设置权限
        */
		//ww1用户加cdrwa权限
		acls.add(new ACL(ZooDefs.Perms.ALL, ww1AclId));

		//ww2用户只加dr权限
		acls.add(new ACL(ZooDefs.Perms.DELETE|ZooDefs.Perms.READ, ww2AclId ));

		zkServer.createZKNode("/AclTest/TestDigest", "TestDigest".getBytes(), acls);
		
		// 注册过的用户必须通过addAuthInfo后才能操作节点，与ZooKeeper命令addauth相同
		zkServer.getZookeeper().addAuthInfo("digest", "ww2:ww2".getBytes());
		//zkServer.getZookeeper().addAuthInfo("digest", "ww1:ww2".getBytes());

		//ZooDefs.Ids.CREATOR_ALL_ACL --> auth:user:password:cdrwa
        //ww1可以create子节点，ww2没权限，不能create子节点
		zkServer.createZKNode("/AclTest/TestDigest/CreateChildTest", "CreateChildTest".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL);
		//NoAuth for /AclTest/TestDigest/CreateChildTest

		Stat statDigest = new Stat();
		byte[] dataDigest = zkServer.getZookeeper().getData("/AclTest/TestDigest", false, statDigest);
		log.warn(new String(dataDigest));
		//ww1可以setData当前节点数据，ww2没权限，不能setData当前节点数据
		zkServer.getZookeeper().setData("/AclTest/TestDigest", "OK".getBytes(), statDigest.getVersion());
		
		// ip方式的acl
		List<ACL> aclsIP = new ArrayList<ACL>();
		Id ipId1 = new Id("ip", "127.0.0.2");
		aclsIP.add(new ACL(ZooDefs.Perms.ALL, ipId1));
		zkServer.createZKNode("/AclTest/IPTest", "iptest".getBytes(), aclsIP);

		// 验证ip是否有权限
		zkServer.getZookeeper().setData("/IPTest/IPTest", "changed".getBytes(), 1);
		Stat statIP = new Stat();
		byte[] dataIP = zkServer.getZookeeper().getData("/IPTest/IPTest", false, statIP);
        log.warn(new String(dataIP));
        log.warn(String.valueOf(statIP.getVersion()));
	}


	@Override
	public void process(WatchedEvent event) {
		
	}
}

