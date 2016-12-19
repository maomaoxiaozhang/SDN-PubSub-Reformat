package edu.bupt.wangfu.module.base;

import edu.bupt.wangfu.info.device.Controller;
import edu.bupt.wangfu.info.msg.Route;
import edu.bupt.wangfu.opendaylight.WsnUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lenovo on 2016-6-22.
 */
public class Config extends SysInfo {
	public static void configure() {
		setParams();
//		localSwtId = getLinkedSwtId(localMac);
		printParams();

		//初始化topic和对应的编码
		WsnUtil.initSysTopicMap();
//		WsnUtil.initNotifyTopicMap();//TODO 现在测试的时候没有admin，因此无法连接
		//测试
		notifyTopicAddrMap.put("all", "ff0e:0080:0000:0000:0000:0000:1111:0006");
		notifyTopicAddrMap.put("all:a", "ff0e:0080:0000:0000:0000:0000:2222:0006");
		notifyTopicAddrMap.put("all:a:1", "ff0e:0080:0000:0000:0000:0000:3333:0006");
		notifyTopicAddrMap.put("all:a:2", "ff0e:0080:0000:0000:0000:0000:4444:0006");
		notifyTopicAddrMap.put("all:a:3", "ff0e:0080:0000:0000:0000:0000:5555:0006");

		System.out.println("参数及主题树配置完成\n");
	}

	private static void printParams() {
		System.out.println("管理员地址：" + adminAddr + "，管理员端口：");
		System.out.println("集群名：" + localGroupName);
		System.out.println("本地OpenFlow交换机ID：" + localSwtId);
		System.out.println("控制消息端口号的起点：" + sysPort + "，转发消息端口：" + notifyPort);
	}

	//TODO 本机连接的swt无法groupCtl获取，当前实验环境的问题
	/*private static String getLinkedSwtId(String hostMac) {
		//返回wsn程序所在主机所连Switch的odl_id
		String url = groupCtl.url + "/restconf/operational/network-topology:network-topology/";
		String body = RestProcess.doClientGet(url);
		JSONObject json = new JSONObject(body);
		JSONObject net_topology = json.getJSONObject("network-topology");
		JSONArray topology = net_topology.getJSONArray("topology");

		for (int i = 0; i < topology.length(); i++) {
			JSONArray link = topology.getJSONObject(i).getJSONArray("link");
			for (int j = 0; j < link.length(); j++) {
				String link_id = link.getJSONObject(j).getString("link-id");
				if (link_id.contains(hostMac)) {
					String[] ps = link_id.split("/");
					for (String p : ps) {
						if (p.contains("openflow")) {
							String[] qs = p.split(":");
							portWsn2Swt = qs[2];
							return qs[1];
						}
					}
				}
			}
		}
		return null;
	}*/

	private static void setParams() {
		Properties props = new Properties();
		String propertiesPath = "./resources/RtConfig.properties";
		try {
			props.load(new FileInputStream(propertiesPath));
		} catch (FileNotFoundException e) {
			System.out.println("找不到公共配置文件");
		} catch (IOException e) {
			System.out.println("读取公共配置文件时发生IOException");
		}

		adminAddr = props.getProperty("adminAddress");
		adminPort = Integer.valueOf(props.getProperty("adminPort"));
		localGroupName = props.getProperty("localGroupName");
		groupCtl = new Controller(props.getProperty("groupCtl"));

		localSwtId = props.getProperty("localSwtId");
		portWsn2Swt = props.getProperty("portWsn2Swt");

		localAddr = props.getProperty("localAddress");
		localMac = props.getProperty("localMac");
		tPort = Integer.valueOf(props.getProperty("tPort"));
		sysPort = Integer.valueOf(props.getProperty("sysPort"));
		notifyPort = Integer.valueOf(props.getProperty("notifyPort"));

		refreshPeriod = Long.parseLong(props.getProperty("refreshPeriod"));
		checkSplitPeriod = Long.parseLong(props.getProperty("checkSplitPeriod"));
		splitThreshold = Integer.parseInt(props.getProperty("splitThreshold"));

		groupEdges = new HashSet<>();
		outSwitches = new ConcurrentHashMap<>();
		hostMap = new ConcurrentHashMap<>();
		switchMap = new ConcurrentHashMap<>();
		nbrGrpLinks = new ConcurrentHashMap<>();
		allGroups = new ConcurrentHashMap<>();
		id2NameMap = new ConcurrentHashMap<>();

		sysTopicAddrMap = new ConcurrentHashMap<>();
		notifyTopicAddrMap = new ConcurrentHashMap<>();

		localSubTopics = new HashSet<>();
		localPubTopic = new HashSet<>();
		groupSubMap = new ConcurrentHashMap<>();
		outerSubMap = new ConcurrentHashMap<>();
		groupPubMap = new ConcurrentHashMap<>();
		outerPubMap = new ConcurrentHashMap<>();
		joinedSubTopics = new HashSet<>();
		joinedUnsubTopics = new HashSet<>();
		notifyFlows = new ConcurrentHashMap<>();

		localCtl = new Controller(localAddr + ":8181");
		groupRoutes = Collections.synchronizedSet(new HashSet<Route>());
	}
}
