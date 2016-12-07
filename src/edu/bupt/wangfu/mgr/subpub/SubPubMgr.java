package edu.bupt.wangfu.mgr.subpub;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.msg.NotifyObj;
import edu.bupt.wangfu.info.msg.SPInfo;
import edu.bupt.wangfu.mgr.base.Config;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.route.RouteUtil;
import edu.bupt.wangfu.mgr.subpub.rcver.PubReceiver;
import edu.bupt.wangfu.mgr.subpub.rcver.SubReceiver;
import edu.bupt.wangfu.mgr.subpub.ws.WsnSPRegister;
import edu.bupt.wangfu.mgr.topology.GroupUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by LCW on 2016-7-19.
 */
public class SubPubMgr extends SysInfo {
	private static CheckSplit splitTask = new CheckSplit(splitThreshold);
	private static Timer splitTimer = new Timer();

	public SubPubMgr() {
		System.out.println("sub pub mgr start");
//		new Thread(new SocketSPRegister(tPort)).start();//接收新发布者和订阅者的注册
		new Thread(new WsnSPRegister()).start(); // webservice方式接收新发布者或订阅者的注册

		new Thread(new SubReceiver()).start();
		new Thread(new PubReceiver()).start();
		splitTimer.schedule(splitTask, checkSplitPeriod, checkSplitPeriod);
	}

	public static void main(String[] args) {
		Config.configure();
		notifyTopicAddrMap.put("All", "1");
		notifyTopicAddrMap.put("All:a", "2");
		notifyTopicAddrMap.put("All:a:1", "3");
		notifyTopicAddrMap.put("All:a:2", "4");
		notifyTopicAddrMap.put("All:a:3", "5");

		localSubscribe("All:a:1", false);
		localSubscribe("All:a:2", false);
		localSubscribe("All:a:3", false);
	}

	//本地有新订阅
	public static boolean localSubscribe(String topic, boolean isJoinedSub) {
		if (!isJoinedSub) {
			//查看是否已订阅该主题的父主题或更高层的主题
			System.out.println("searching for subscribed father topic");
			String[] topicPath = topic.split(":");
			String cur = topicPath[0];
			for (int i = 1; i < topicPath.length; i++) {
				if (localSubTopics.contains(cur))
					return false;
				else
					cur += ":" + topicPath[i];
			}
		}
		//更新节点上的订阅信息
		localSubTopics.add(topic);
		//再判断是否需要聚合
		if (needjoined(topic) && !isJoinedSub) {
			System.out.println("new sub topic need to be joined");
			String father = getTopicFather(topic);

			joinedSubTopics.add(father);
			localSubscribe(father, true);

			joinedUnsubTopics.add(topic);
			unsubAllSons(father);
			return true;
		} else {
			if (joinedSubTopics.contains(topic) && !isJoinedSub) {
				joinedSubTopics.remove(topic);
				return true;
			}
			//更新本集群订阅信息
			System.out.println("refresh local group sub map");
			Set<String> groupSub = groupSubMap.get(topic) == null ? new HashSet<String>() : groupSubMap.get(topic);
			groupSub.add(localSwtId + ":" + portWsn2Swt);
			//全网广播
			spreadSPInfo(topic, "sub", Action.SUB);
			//更新全网所有集群信息
//			Group g = allGroups.get(localGroupName);!!!
			Group g = new Group("g1");
			g.subMap = cloneSetMap(groupSubMap);
			g.updateTime = System.currentTimeMillis();
			allGroups.put(g.groupName, g);
			GroupUtil.spreadLocalGrp(g);

			new Thread(new SubMsgReciver(topic)).start();

			RouteUtil.newSuber(localGroupName, localSwtId, portWsn2Swt, topic);
			return true;
		}

	}

	public static Map<String, Set<String>> cloneSetMap(Map<String, Set<String>> map) {
		Map<String, Set<String>> res = new ConcurrentHashMap<>();
		for (String key : map.keySet()) {
			Set<String> set = new HashSet<>();
			for (String inSet : map.get(key)) {
				set.add(inSet);
			}
			res.put(key, set);
		}
		return res;
	}

	//本地有取消订阅
	public static boolean localUnsubscribe(String topic) {
		if (joinedSubTopics.contains(topic))//若这个订阅是聚合而成的，那么不能取消，因为并不是真实订阅
			return false;
		if (!localSubTopics.contains(topic))
			return false;//本地没有这个订阅

//		localSubTopics.remove(topic);

		Set<String> groupSub = groupSubMap.get(topic);
		groupSub.remove(localSwtId + ":" + portWsn2Swt);

		spreadSPInfo(topic, "sub", Action.UNSUB);

//		Group g = allGroups.get(localGroupName);!!!
		Group g = new Group("g1");
		g.subMap = cloneSetMap(groupSubMap);
		g.updateTime = System.currentTimeMillis();
		allGroups.put(g.groupName, g);
		GroupUtil.spreadLocalGrp(g);

		//TODO 应该删除集群内流表和过路流表，重新计算新的流表；但是太复杂了，需要再讨论

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

		spreadSPInfo(topic, "pub", Action.PUB);

		Group g = allGroups.get(localGroupName);
		g.pubMap = cloneSetMap(groupPubMap);
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

		spreadSPInfo(topic, "pub", Action.UNPUB);

		Group g = allGroups.get(localGroupName);
		g.pubMap = cloneSetMap(groupPubMap);
		g.updateTime = System.currentTimeMillis();
		allGroups.put(g.groupName, g);
		GroupUtil.spreadLocalGrp(g);

		//TODO 应该删除集群内流表和过路流表，重新计算新的流表；但是太复杂了，需要再讨论

		return true;
	}

	private static void unsubAllSons(String father) {
		Iterator<String> iterator = localSubTopics.iterator();
		while (iterator.hasNext()) {
			String topic = iterator.next();
			if (topic.contains(father) && topic.length() > father.length()) {
				iterator.remove();//用这个删除localSubTopics中的值，而不是localSubTopics.remove(topic)，因为在遍历时会出现ConcurrentModificationException
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

	private static boolean needjoined(String topic) {
		int subBros = 0;
		int level = topic.split(":").length;
		String father = getTopicFather(topic);
		int totalDirectSons = totalDirectSons(father);
		for (String lst : localSubTopics) {
			if (lst.contains(father) && lst.split(":").length == level && !lst.equals(topic)) {//lst是topic的兄弟主题
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
		SPInfo nsp = new SPInfo();
		MultiHandler handler = new MultiHandler(sysPort, type, "sys");

		nsp.action = action;
		nsp.group = localGroupName;
		nsp.swtId = localSwtId;
		nsp.hostMac = localMac;
		nsp.hostIP = localAddr;
		nsp.port = portWsn2Swt;

		nsp.topic = topic;

		handler.v6Send(nsp);
		System.out.println("spread updated sub/pub info");
	}

	private static class CheckSplit extends TimerTask {
		int splitThreshold = 1;//TODO 需要动态设置？

		public CheckSplit(int splitThreshold) {
			System.out.println("checking split");
			this.splitThreshold = splitThreshold;
		}

		@Override
		public void run() {
			for (String father : joinedSubTopics) {
				if (getCurFlowStatus(father) > splitThreshold) {
					localUnsubscribe(father);
					joinedSubTopics.remove(father);
					for (String son : joinedUnsubTopics) {
						if (son.contains(father)) {
							localSubscribe(son, false);
							joinedUnsubTopics.remove(son);
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

	private static class SubMsgReciver implements Runnable {
		String topic;
		MultiHandler handler;

		public SubMsgReciver(String topic) {
			this.topic = topic;
			this.handler = new MultiHandler(notifyPort, topic, "notify");
		}

		@Override
		public void run() {
			NotifyObj obj = (NotifyObj) handler.v6Receive();
			processNotifyObj(obj);
		}

		private void processNotifyObj(NotifyObj obj) {
			if (localSubTopics.contains(obj.topic)
					|| joinedUnsubTopics.contains(obj.topic)) {

				//TODO 查找本地订阅者，把这条消息用原来的webservice或者什么东东，发给本地订阅者
			}
		}
	}
}
