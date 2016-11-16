package edu.bupt.wangfu.mgr.subpub;

import edu.bupt.wangfu.info.device.Flow;
import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.device.Switch;
import edu.bupt.wangfu.info.msg.SubPubInfo;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.route.RouteUtil;
import edu.bupt.wangfu.mgr.subpub.rcver.PubReceiver;
import edu.bupt.wangfu.mgr.subpub.rcver.SubReceiver;
import edu.bupt.wangfu.mgr.topology.GroupUtil;
import edu.bupt.wangfu.opendaylight.FlowUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by LCW on 2016-7-19.
 */
public class SubPubMgr extends SysInfo {
	private static CheckSplit splitTask = new CheckSplit(splitThreshold);
	private static Timer splitTimer = new Timer();

	public SubPubMgr() {
		new Thread(new SubPubRegister(tPort)).start();//接收新发布者和订阅者的注册

		new Thread(new SubReceiver()).start();
		new Thread(new PubReceiver()).start();
		splitTimer.schedule(splitTask, checkSplitPeriod, checkSplitPeriod);
	}

	//本地有新订阅
	public static boolean localSubscribe(String topic) {
		//查看是否已订阅该主题的父主题
		String[] topicPath = topic.split(":");
		String cur = topicPath[0];
		for (int i = 1; i < topicPath.length; i++) {
			if (localSubTopic.contains(cur))
				return false;
			else
				cur += ":" + topicPath[i];
		}
		//再判断是否需要聚合
		if (needUnite(topic)) {
			String father = getTopicFather(topic);
			joinedSubTopics.add(father);
			localSubscribe(father);
			unsubAllSons(father);
			return true;
		} else {
			//更新wsn节点上的订阅信息
			localSubTopic.add(cur);
			if (joinedSubTopics.contains(cur))
				joinedSubTopics.remove(cur);
			//更新本集群订阅信息
			Set<String> groupSub = groupSubMap.get(cur) == null ? new HashSet<String>() : groupSubMap.get(cur);
			groupSub.add(localSwtId + ":" + portWsn2Swt);
			groupSubMap.put(topic, groupSub);
			//全网广播
			spreadSPInfo(cur, "sub", Action.SUB);
			//更新全网所有集群信息
			Group g = allGroups.get(localGroupName);
			g.subMap = groupSubMap;
			g.updateTime = System.currentTimeMillis();
			allGroups.put(g.groupName, g);
			GroupUtil.spreadLocalGrp(g);

			RouteUtil.newSuber(localGroupName, localSwtId, portWsn2Swt, cur);
			return true;
		}
	}

	//本地有取消订阅
	public static boolean localUnsubscribe(String topic) {
		if (joinedSubTopics.contains(topic))//若这个订阅是聚合而成的，那么不能取消，因为并不是真实订阅
			return false;
		if (!localSubTopic.contains(topic))
			return false;//本地没有这个订阅

		localSubTopic.remove(topic);

		Set<String> groupSub = groupSubMap.get(topic);
		groupSub.remove(localSwtId + ":" + portWsn2Swt);
		groupSubMap.put(topic, groupSub);

		spreadSPInfo(topic, "sub", Action.UNSUB);

		Group g = allGroups.get(localGroupName);
		g.subMap = groupSubMap;
		g.updateTime = System.currentTimeMillis();
		allGroups.put(g.groupName, g);
		GroupUtil.spreadLocalGrp(g);

		//TODO 应该删除集群内流表和过路流表，重新计算新的流表；但是太复杂了，需要再讨论
		/*if (groupSubMap.get(topic).size() == 0) {
			Set<Flow> flows = notifyFlows.get(topic);
			for (Flow f : flows) {
				FlowUtil.deleteFlow(groupCtl, f);
			}
		}*/

		return true;
	}

	//本地有新发布
	public static boolean localPublish(String topic) {
		if (localPubTopic.contains(topic))
			return false;//本地已有这个发布
		localPubTopic.add(topic);

		Set<String> groupPub = groupPubMap.get(topic) == null ? new HashSet<String>() : groupPubMap.get(topic);
		if (groupPub.contains(localSwtId + ":" + portWsn2Swt))
			return false;
		groupPub.add(localSwtId + ":" + portWsn2Swt);
		groupPubMap.put(topic, groupPub);

		spreadSPInfo(topic, "pub", Action.PUB);

		Group g = allGroups.get(localGroupName);
		g.subMap = groupPubMap;
		g.updateTime = System.currentTimeMillis();
		allGroups.put(g.groupName, g);
		GroupUtil.spreadLocalGrp(g);

		RouteUtil.newPuber(localGroupName, localSwtId, portWsn2Swt, topic);
		return true;
	}

	//本地有取消发布
	public static boolean localUnpublish(String topic) {
		if (!localPubTopic.contains(topic))
			return false;
		localPubTopic.remove(topic);

		Set<String> groupPub = groupPubMap.get(topic);
		groupPub.remove(localSwtId + ":" + portWsn2Swt);
		groupPubMap.put(topic, groupPub);

		spreadSPInfo(topic, "pub", Action.UNPUB);

		Group g = allGroups.get(localGroupName);
		g.subMap = groupPubMap;
		g.updateTime = System.currentTimeMillis();
		allGroups.put(g.groupName, g);
		GroupUtil.spreadLocalGrp(g);

		//TODO 应该删除集群内流表和过路流表，重新计算新的流表；但是太复杂了，需要再讨论

		return true;
	}

	private static void unsubAllSons(String father) {
		for (String topic : localSubTopic) {
			if (topic.contains(father) && topic.length() > father.length()) {
				localUnsubscribe(topic);
				joinedUnsubTopics.add(topic);
			}
		}
	}

	//取得该主题的父主题
	private static String getTopicFather(String topic) {
		String[] topicPath = topic.split(":");
		String fatherTopic = topicPath[0];
		for (int i = 1; i < topicPath.length - 1; i++) {
			fatherTopic += ":" + topicPath[i];
		}
		return fatherTopic;
	}

	private static boolean needUnite(String topic) {
		int subBros = 0;
		int level = topic.split(":").length;
		String father = getTopicFather(topic);
		int totalDirectSons = totalDirectSons(father);
		for (String lst : localSubTopic) {
			if (lst.contains(father) && lst.split(":").length == level) {//lst是topic的兄弟主题
				subBros++;
			}
		}
		return subBros > totalDirectSons / 2;
	}

	private static int totalDirectSons(String father) {
		int res = 0;
		int fatherLevel = father.split(":").length;
		for (String topic : notifyTopicAddrMap.keySet()) {
			int topicLevel = topic.split(":").length;
			if (topic.contains(father) && topicLevel == fatherLevel + 1) {
				res++;
			}
		}
		return res;
	}

	private static void spreadSPInfo(String topic, String type, Action action) {
		SubPubInfo nsp = new SubPubInfo();
		MultiHandler handler = new MultiHandler(uPort, type, "sys");

		nsp.action = action;
		nsp.group = localGroupName;
		nsp.swtId = localSwtId;
		nsp.hostMac = localMac;
		nsp.hostIP = localAddr;
		nsp.port = portWsn2Swt;

		nsp.topic = topic;

		handler.v6Send(nsp);
	}

	//下发注册流表，有了这个流表，之后如果wsn要产生什么订阅或者发布，就可以通过这个流表扩散到全网
	//这里也是不需要定义in_port，只需要出现这样的消息，就全网flood
	public static void downSubPubFlow() {
		for (Switch swt : switchMap.values()) {
			Flow floodFlow = FlowUtil.getInstance().generateFlow(swt.id, "flood", "sub", "sys", 1, 10);//TODO 优先级是越大越靠后吗？
			FlowUtil.downFlow(groupCtl, floodFlow, "add");
			floodFlow = FlowUtil.getInstance().generateFlow(swt.id, "flood", "pub", "sys", 1, 10);
			FlowUtil.downFlow(groupCtl, floodFlow, "add");
		}
	}
//	{
//		for (Switch swt : switchMap.values()) {
//			for (String port : swt.neighbors.keySet()) {//swt上连接着集群内其他swt或者host的端口
//				Flow floodFlow = FlowUtil.getInstance().generateFlow(swt.id, port, "flood", "sub", "sys", 1, 10);//TODO 优先级是越大越靠后吗？
//				FlowUtil.downFlow(localCtl, floodFlow, "add");
//				floodFlow = FlowUtil.getInstance().generateFlow(swt.id, port, "flood", "pub", "sys", 1, 10);
//				FlowUtil.downFlow(localCtl, floodFlow, "add");
//			}
//			for (String port : swt.portSet) {
//				if (!port.equals("LOCAL")) {//swt上连接着集群外swt的端口
//					Flow floodFlow = FlowUtil.getInstance().generateFlow(swt.id, port, "flood", "sub", "sys", 1, 10);
//					FlowUtil.downFlow(localCtl, floodFlow, "add");
//					floodFlow = FlowUtil.getInstance().generateFlow(swt.id, port, "flood", "pub", "sys", 1, 10);
//					FlowUtil.downFlow(localCtl, floodFlow, "add");
//				}
//			}
//		}
//	}

	private static class CheckSplit extends TimerTask {
		int splitThreshold = 1;//TODO 需要动态设置？

		public CheckSplit(int splitThreshold) {
			this.splitThreshold = splitThreshold;
		}

		@Override
		public void run() {
			for (String father : joinedSubTopics) {
				if (getCurFlowStatus(father) > splitThreshold) {
					localUnsubscribe(father);
					for (String son : joinedUnsubTopics) {
						if (son.contains(father)) {
							localSubscribe(son);
						}
					}
				}
			}
		}

		private int getCurFlowStatus(String father) {
			//TODO 需要查询groupCtl，逻辑要问牛琳琳
			return 1;//TODO 返回的是百分比？20/100就返回20？
		}
	}
}
