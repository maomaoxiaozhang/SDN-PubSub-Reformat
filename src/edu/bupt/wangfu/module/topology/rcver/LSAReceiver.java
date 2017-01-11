package edu.bupt.wangfu.module.topology.rcver;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.msg.AllGrps;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.opendaylight.MultiHandler;

/**
 * Created by lenovo on 2016-6-23.
 */
public class LSAReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public LSAReceiver() {
		System.out.println("LSA监听线程启动");
		handler = new MultiHandler(sysPort, "lsa", "sys");
	}

	@Override
	public void run() {
		while (true) {
			Object msg = handler.v6Receive();
			if (msg instanceof Group) {
				Group lsa = (Group) msg;
				System.out.println("收到单条LSA");
				Group localGrpInfo = allGroups.get(lsa.groupName);
				if (localGrpInfo == null || localGrpInfo.id < lsa.id) {
					allGroups.put(lsa.groupName, lsa);
				}
			} else if (msg instanceof AllGrps) {
				AllGrps ags = (AllGrps) msg;
				System.out.print("收到LSDB");
				for (Group group : ags.allGrps.values()) {
					if (allGroups.containsKey(group.groupName) && allGroups.get(group.groupName).id < group.id) {
						allGroups.put(group.groupName, group);
					} else if (!allGroups.containsKey(group.groupName)) {
						allGroups.put(group.groupName, group);
					}
				}
			}
			//测试
			/*System.out.println("当前网络中集群信息为：");
			for (Group group : allGroups.values()) {
				System.out.println(group.toString());
			}*/
		}
	}
}