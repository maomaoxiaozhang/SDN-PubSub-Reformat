package edu.bupt.wangfu.module.topology.rcver;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.route.RouteUtil;
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
				Group newGrpInfo = (Group) msg;
				if (!newGrpInfo.groupName.equals(localGroupName)) {
					Group localGrpInfo = allGroups.get(newGrpInfo.groupName);
					if (localGrpInfo == null) {
						allGroups.put(newGrpInfo.groupName, newGrpInfo);
					} else if (localGrpInfo.id < newGrpInfo.id) {
						allGroups.put(newGrpInfo.groupName, newGrpInfo);
						if (isNbrChanged(newGrpInfo, localGrpInfo)) {
							System.out.println("***收到的LSA不太妙，开始重新计算路由：" + System.currentTimeMillis());
							RouteUtil.reCalRoutes();
						}
					}
				}
			} /*else if (msg instanceof AllGrps) {
				AllGrps ags = (AllGrps) msg;
				for (Group group : ags.allGrps.values()) {
					if (allGroups.containsKey(group.groupName) && allGroups.get(group.groupName).id < group.id) {
						allGroups.put(group.groupName, group);
					} else if (!allGroups.containsKey(group.groupName)) {
						allGroups.put(group.groupName, group);
					}
				}
			}*/
		}
	}

	private boolean isNbrChanged(Group newGrpInfo, Group localGrpInfo) {
		for (String nbr : newGrpInfo.dist2NbrGrps.keySet()) {
			if (!localGrpInfo.dist2NbrGrps.containsKey(nbr)) {
				return true;
			}
		}
		return false;
	}
}