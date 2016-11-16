package edu.bupt.wangfu.mgr.topology.rcver;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.msg.AllGrps;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.opendaylight.MultiHandler;

/**
 * Created by lenovo on 2016-6-23.
 */
public class LSAReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public LSAReceiver() {
		handler = new MultiHandler(uPort, "lsa", "sys");
	}

	@Override
	public void run() {
		while (true) {
			Object msg = handler.v6Receive();
			if (msg instanceof Group) {
				Group lsa = (Group) msg;
				Group g = allGroups.get(lsa.groupName);
				if (g == null || g.updateTime < lsa.updateTime) {
					allGroups.put(lsa.groupName, lsa);
				}
			} else if (msg instanceof AllGrps) {
				AllGrps ags = (AllGrps) msg;
				for (Group group : ags.allGrps.values()) {
					if (allGroups.containsKey(group.groupName)
							&& allGroups.get(group.groupName).updateTime < group.updateTime) {
						allGroups.put(group.groupName, group);
					} else if (!allGroups.containsKey(group.groupName)) {
						allGroups.put(group.groupName, group);
					}
				}
			}
		}
	}
}