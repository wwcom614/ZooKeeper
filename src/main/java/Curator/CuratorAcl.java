package Curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CuratorAcl {

    final static Logger log = LoggerFactory.getLogger(CuratorAcl.class);

    public CuratorFramework client = null;
    public static final String zkServerIpPort = "127.0.0.1:2181";

    public CuratorAcl() {
        RetryPolicy retryPolicy = new RetryNTimes(3, 5000);
        //CuratorFrameworkFactory连接时，支持authorization登录schema+用户，也可以是AuthInfos的List
        client = CuratorFrameworkFactory.builder().authorization("digest", "ww2:ww2".getBytes())
                .connectString(zkServerIpPort)
                .sessionTimeoutMs(10000).retryPolicy(retryPolicy)
                .namespace("workspace").build();
        client.start();
    }

    public void closeZKClient() {
        if (client != null) {
            this.client.close();
        }
    }

    public static void main(String[] args) throws Exception {
        // 实例化
        CuratorAcl cto = new CuratorAcl();
        boolean isZkCuratorStarted = cto.client.isStarted();
        log.warn("当前客户端状态：" + (isZkCuratorStarted ? "连接中" : "已关闭"));

        String nodePath = "/acl/father/child";

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

        //ww2用户只加cdr权限
        acls.add(new ACL(ZooDefs.Perms.CREATE | ZooDefs.Perms.DELETE | ZooDefs.Perms.READ, ww2AclId));



        // 创建节点
		byte[] dataCreate = "create1".getBytes();
		cto.client.create().creatingParentsIfNeeded()
				.withMode(CreateMode.PERSISTENT)
				// withACL的第2个参数为false，表明仅仅对本次创建的节点设置ACL，一般这样用即可，防止多人使用时因权限设置不当造成生产事故
                //withACL的第2个参数为true，表明对创建的父节点也要设置相同的ACL。安全性要求高的系统使用
                // 此时需注意CuratorFrameworkFactory连接时，需authorization登录有create创建权限的用户才能操作。
                //withACL的第2个参数为true，已存在的父节点不会被更新权限，不受影响
                .withACL(acls, true)
				.forPath(nodePath, dataCreate);

		cto.client.setACL().withACL(acls).forPath(nodePath);

        // 读取节点数据
        Stat stat = new Stat();
        byte[] dataGet = cto.client.getData().storingStatIn(stat).forPath(nodePath);
        log.warn("创建的节点" + nodePath + "的数据为: " + new String(dataGet));
        log.warn("创建的节点的版本号为: " + stat.getVersion());

        // 更新节点数据
		byte[] dataUpdate = "update2".getBytes();
		cto.client.setData().withVersion(stat.getVersion()).forPath(nodePath, dataUpdate);

        dataGet = cto.client.getData().storingStatIn(stat).forPath(nodePath);
        log.warn("更新的节点" + nodePath + "的数据为: " + new String(dataGet));
        log.warn("更新的版本号为: " + stat.getVersion());

        // 删除节点
		cto.client.delete().guaranteed().deletingChildrenIfNeeded().withVersion(stat.getVersion()).forPath(nodePath);
        log.warn("节点" + nodePath + "已删除！");

        cto.closeZKClient();
    }


}
