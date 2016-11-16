package edu.bupt.wangfu.mgr.subpub.rcver;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.msg.SubPubInfo;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.route.RouteUtil;
import edu.bupt.wangfu.mgr.subpub.Action;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lenovo on 2016-10-27.
 */
public class PubReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public PubReceiver() {
		handler = new MultiHandler(uPort, "pub", "sys");
	}

	@Override
	public void run() {
		while (true) {
			Object msg = handler.v6Receive();
			SubPubInfo np = (SubPubInfo) msg;
			new Thread(new PubHandler(np)).start();
		}
	}

	private class PubHandler implements Runnable {
		private SubPubInfo pub;

		PubHandler(SubPubInfo pub) {
			this.pub = pub;
		}

		@Override
		public void run() {
			if (pub.group.equals(localGroupName)) {
				if (pub.action.equals(Action.PUB)) {
					Set<String> groupPub = groupPubMap.get(pub.topic) == null ? new HashSet<String>() : groupPubMap.get(pub.topic);
					groupPub.add(pub.swtId + ":" + pub.port);
					groupPubMap.put(pub.topic, groupPub);
				} else if (pub.action.equals(Action.UNPUB)) {
					Set<String> groupPub = groupPubMap.get(pub.topic);
					groupPub.remove(pub.swtId + ":" + pub.port);
					groupPubMap.put(pub.topic, groupPub);
				}
			} else {
				if (pub.action.equals(Action.PUB)) {
					Set<String> outerPub = outerPubMap.get(pub.topic) == null ? new HashSet<String>() : outerPubMap.get(pub.topic);
					outerPub.add(pub.group);
					outerPubMap.put(pub.topic, outerPub);

					Group g = allGroups.get(pub.group);
					g.pubMap.get(pub.topic).add(pub.swtId + ":" + pub.port);
					g.updateTime = System.currentTimeMillis();
					allGroups.put(g.groupName, g);

					if (localCtl.equals(groupCtl)) {
						RouteUtil.newPuber(pub.group, "", "", pub.topic);
					}
				} else if (pub.action.equals(Action.UNPUB)) {
					if (allGroups.get(pub.group).pubMap.get(pub.topic).size() == 1) {
						Set<String> outerPub = outerPubMap.get(pub.topic);
						outerPub.remove(pub.group);
						outerPubMap.put(pub.topic, outerPub);

						if (localCtl.equals(groupCtl)) {
							RouteUtil.reCalGraph(pub.topic);
						}
					}
				}
			}
		}
	}
}
