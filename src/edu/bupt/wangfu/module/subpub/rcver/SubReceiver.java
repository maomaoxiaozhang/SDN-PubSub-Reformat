package edu.bupt.wangfu.module.subpub.rcver;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.msg.SPInfo;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.route.RouteUtil;
import edu.bupt.wangfu.module.subpub.Action;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lenovo on 2016-10-27.
 */
public class SubReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public SubReceiver() {
		System.out.println("订阅及取消订阅消息（sub）监听线程启动");
		handler = new MultiHandler(sysPort, "sub", "sys");
	}

	@Override
	public void run() {
		while (true) {
			Object msg = handler.v6Receive();
			SPInfo ns = (SPInfo) msg;
			new Thread(new SubHandler(ns)).start();
		}
	}

	private class SubHandler implements Runnable {
		private SPInfo sub;

		SubHandler(SPInfo sub) {
			this.sub = sub;
		}

		@Override
		public void run() {
			if (sub.group.equals(localGroupName)) {//本集群内节点产生的订阅
				if (sub.action.equals(Action.SUB)) {
					System.out.println("new suber in group, sub topic is " + sub.topic);

					Set<String> groupSub = groupSubMap.get(sub.topic) == null ? new HashSet<String>() : groupSubMap.get(sub.topic);
					groupSub.add(sub.swtId + ":" + sub.port);
				} else if (sub.action.equals(Action.UNSUB)) {
					System.out.println("new unsub from group, topic is " + sub.topic);

					Set<String> groupSub = groupSubMap.get(sub.topic);
					groupSub.remove(sub.swtId + ":" + sub.port);

					//TODO 删除Route路由流表
				}
			} else {//邻居集群产生的订阅
				if (sub.action.equals(Action.SUB)) {
					System.out.println("new suber from neighbor, sub topic is " + sub.topic);

					Set<String> outerSub = outerSubMap.get(sub.topic) == null ? new HashSet<String>() : outerSubMap.get(sub.topic);
					outerSub.add(sub.group);
					outerSubMap.put(sub.topic, outerSub);

					Group g = allGroups.get(sub.group);
					g.subMap.get(sub.topic).add(sub.swtId + ":" + sub.port);
					g.updateTime = System.currentTimeMillis();
					allGroups.put(g.groupName, g);

					if (localCtl.url.equals(groupCtl.url)) {//因为sub信息会全网广播，集群中只要有一个人计算本集群该做什么就可以了
						RouteUtil.newSuber(sub.group, "", "", sub.topic);
					}
				} else if (sub.action.equals(Action.UNSUB)) {
					System.out.println("new unsub from neighbor, topic is " + sub.topic);

					if (allGroups.get(sub.group).subMap.get(sub.topic).size() == 1) {//如果发来取消订阅信息的集群内
						// 有不止一个订阅节点，那么就不需要修改outerSubMap
						Set<String> outerSub = outerSubMap.get(sub.topic);
						outerSub.remove(sub.group);
						outerSubMap.put(sub.topic, outerSub);

						if (localCtl.url.equals(groupCtl.url)) {//因为sub信息会全网广播，集群中只要有一个人计算本集群该做什么就可以了
							RouteUtil.updateNbrChange(sub.topic);
						}
					}
				}
			}

		}
	}
}
