package edu.bupt.wangfu.module.subpub;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.msg.NotifyObj;
import edu.bupt.wangfu.info.msg.SPInfo;
import edu.bupt.wangfu.module.base.Config;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.route.RouteUtil;
import edu.bupt.wangfu.module.subpub.rcver.PubReceiver;
import edu.bupt.wangfu.module.subpub.rcver.SubReceiver;
import edu.bupt.wangfu.module.subpub.ws.WsnSPRegister;
import edu.bupt.wangfu.module.topology.GroupUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static edu.bupt.wangfu.module.base.WsnMgr.cloneSetMap;

/**
 * Created by LCW on 2016-7-19.
 */
public class SubPubMgr extends SysInfo {
	private static CheckSplit splitTask = new CheckSplit(splitThreshold);
	private static Timer splitTimer = new Timer();
	private static String address;

	public SubPubMgr() {
		System.out.println("SubPubMgr启动");
//		new Thread(new SocketSPRegister(tPort)).start();//接收新发布者和订阅者的注册
		new Thread(new WsnSPRegister()).start(); // webservice方式接收新发布者或订阅者的注册

		new Thread(new SubReceiver()).start();
		new Thread(new PubReceiver()).start();
		splitTimer.schedule(splitTask, checkSplitPeriod, checkSplitPeriod);
	}

	public static void main(String[] args) {
		Config.configure();
		notifyTopicAddrMap.put("All", "ff0e:0080:0000:0000:0000:0000:1111:0006");
		notifyTopicAddrMap.put("All:a", "ff0e:0080:0000:0000:0000:0000:2222:0006");
		notifyTopicAddrMap.put("All:a:1", "ff0e:0080:0000:0000:0000:0000:3333:0006");
		notifyTopicAddrMap.put("All:a:2", "ff0e:0080:0000:0000:0000:0000:4444:0006");
		notifyTopicAddrMap.put("All:a:3", "ff0e:0080:0000:0000:0000:0000:5555:0006");

		localSubscribe("All", false, address);
//		localSubscribe("All:a:2", false);
//		localSubscribe("All:a:3", false);
	}

	//本地有新订阅
	public static boolean localSubscribe(String topic, boolean isJoinedSub, String subRcvServiceAddr) {
		System.out.println("判定是否为聚合订阅：" + isJoinedSub);
		if (!isJoinedSub) {
			//查看是否已订阅该主题的父主题或更高层的主题
			System.out.println("搜索当前订阅表中有无订阅过" + topic + "主题的父亲主题");
			String[] topicPath = topic.split(":");
			String cur = topicPath[0];
			for (int i = 1; i < topicPath.length; i++) {
				if (localSubTopics.keySet().contains(cur)) {
					System.out.println("有订阅过更早期的主题，取消订阅流程");
					return false;
				} else
					cur += ":" + topicPath[i];
			}
		}
		//更新节点上的订阅信息
		System.out.println("更新本地订阅表localSubTopics");
		localSubTopics.put(topic, subRcvServiceAddr);
		//再判断是否需要聚合
		System.out.println("判定新订阅是否需要聚合");
		if (needJoin(topic) && !isJoinedSub) {
			System.out.println("新订阅需要聚合");
			String father = getTopicFather(topic);

			joinedSubTopics.add(father);
			localSubscribe(father, true, address);

			joinedUnsubTopics.add(topic);
			unsubAllSons(father);
			return true;
		} else {
			System.out.println("新订阅不需要聚合，判定新订阅是否为之前聚合而成的订阅");

			if (joinedSubTopics.contains(topic) && !isJoinedSub) {
				System.out.println("新订阅是聚合而成的订阅，更新订阅状态");

				joinedSubTopics.remove(topic);
				return true;
			}
			System.out.println("新订阅是全新订阅，更新本集群订阅信息groupSubMap");

			//更新本集群订阅信息
			Set<String> groupSub = groupSubMap.get(topic) == null ? new HashSet<String>() : groupSubMap.get(topic);
			groupSub.add(localSwtId + ":" + portWsn2Swt);
			groupSubMap.put(topic, groupSub);
			//全网广播
			spreadSPInfo(topic, "sub", Action.SUB);
			//更新全网所有集群信息
			Group g = allGroups.get(localGroupName);//实际使用
//			Group g = new Group("g1");//测试
			g.subMap = cloneSetMap(groupSubMap);
			g.id += 1;
			g.updateTime = System.currentTimeMillis();
			GroupUtil.spreadLocalGrp();

			new Thread(new SubMsgReciver(topic)).start();

			RouteUtil.newSuber(localGroupName, localSwtId, portWsn2Swt, topic);
			return true;
		}

	}

	//本地有取消订阅
	public static boolean localUnsubscribe(String topic) {
		if (joinedSubTopics.contains(topic))//若这个订阅是聚合而成的，那么不能取消，因为并不是真实订阅
			return false;
		if (!localSubTopics.keySet().contains(topic))
			return false;//本地没有这个订阅

//		localSubTopics.remove(topic);

		Set<String> groupSub = groupSubMap.get(topic);
		groupSub.remove(localSwtId + ":" + portWsn2Swt);

		spreadSPInfo(topic, "sub", Action.UNSUB);

		Group g = allGroups.get(localGroupName);//真实使用
//		Group g = new Group("g1");//测试
		g.subMap = cloneSetMap(groupSubMap);
		g.id += 1;
		g.updateTime = System.currentTimeMillis();
		GroupUtil.spreadLocalGrp();

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
		groupPubMap.put(topic, groupPub);

		spreadSPInfo(topic, "pub", Action.PUB);

		Group g = allGroups.get(localGroupName);
		g.pubMap = cloneSetMap(groupPubMap);
		g.id += 1;
		g.updateTime = System.currentTimeMillis();
		GroupUtil.spreadLocalGrp();

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
		g.id += 1;
		g.updateTime = System.currentTimeMillis();
		GroupUtil.spreadLocalGrp();

		//TODO 应该删除集群内流表和过路流表，重新计算新的流表；但是太复杂了，需要再讨论

		return true;
	}

	private static void unsubAllSons(String father) {
		Iterator<String> iterator = localSubTopics.keySet().iterator();
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

	private static boolean needJoin(String topic) {
		if (topic.toLowerCase().equals("all"))
			return false;

		int subBros = 0;
		int level = topic.split(":").length;
		String father = getTopicFather(topic);
		int totalDirectSons = totalDirectSons(father);
		for (String lst : localSubTopics.keySet()) {
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
		System.out.println("全网广播集群内订阅情况变更，变更类型为" + action.toString() + "，相关主题是" + topic);
	}

	private static class CheckSplit extends TimerTask {
		int splitThreshold = 1;//TODO 需要动态设置？

		public CheckSplit(int splitThreshold) {
			this.splitThreshold = splitThreshold;
		}

		@Override
		public void run() {
			System.out.println("检查订阅表是否需要进行分裂");
			for (String father : joinedSubTopics) {
				if (getCurFlowStatus(father) > splitThreshold) {
					localUnsubscribe(father);
					joinedSubTopics.remove(father);
					for (String son : joinedUnsubTopics) {
						if (son.contains(father)) {
							localSubscribe(son, false, address);
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
			System.out.println("新监听启动中，监听主题为" + topic);
			this.topic = topic;
			this.handler = new MultiHandler(notifyPort, topic.toLowerCase(), "notify");
		}

		@Override
		public void run() {
			NotifyObj obj = (NotifyObj) handler.v6Receive();
			processNotifyObj(obj);
		}

		private void processNotifyObj(NotifyObj obj) {
			if (localSubTopics.keySet().contains(obj.topic)
					|| joinedUnsubTopics.contains(obj.topic)) {

				//查找本地订阅者，把这条消息用原来的webservice或者什么东东，发给本地订阅者
				URL wsdlUrl = null;
				try {
					wsdlUrl = new URL(localSubTopics.get(obj.topic) + "?wsdl");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				Service s = Service.create(wsdlUrl, new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterService"));
				WsnSPRegister hs = s.getPort(new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterPort"), WsnSPRegister.class);

				hs.wsnServerMethod(obj.content);

			}
		}
	}
}
