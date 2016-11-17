package edu.bupt.wangfu.mgr.base;

import edu.bupt.wangfu.info.device.Controller;
import edu.bupt.wangfu.info.device.Host;
import edu.bupt.wangfu.info.msg.Route;
import edu.bupt.wangfu.opendaylight.RestProcess;
import edu.bupt.wangfu.opendaylight.WsnUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lenovo on 2016-6-22.
 */
public class Config extends SysInfo {
	public void configure() {
		setParams();

		Host node = new Host(localAddr);
		localMac = node.getMac();
		localSwtId = getLinkedSwtId(localMac);

		//初始化topic和对应的编码
		WsnUtil.initSysTopicMap();
		WsnUtil.initNotifyTopicMap();
	}

	private String getLinkedSwtId(String hostMac) {
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
	}

	private void setParams() {
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
		localAddr = props.getProperty("localAddress");
		tPort = Integer.valueOf(props.getProperty("tPort"));
		uPort = Integer.valueOf(props.getProperty("uPort"));

		refreshPeriod = Long.parseLong(props.getProperty("refreshPeriod"));
		checkSplitPeriod = Long.parseLong(props.getProperty("checkSplitPeriod"));
		splitThreshold = Integer.parseInt(props.getProperty("splitThreshold"));

		groupEdges = new HashSet<>();
		outSwitches = new ConcurrentHashMap<>();
		hostMap = new ConcurrentHashMap<>();
		switchMap = new ConcurrentHashMap<>();
		nbrGrpLinks = new ConcurrentHashMap<>();
		allGroups = new ConcurrentHashMap<>();

		sysTopicAddrMap = new ConcurrentHashMap<>();
		notifyTopicAddrMap = new ConcurrentHashMap<>();

		localSubTopic = new HashSet<>();
		localPubTopic = new HashSet<>();
		groupSubMap = new ConcurrentHashMap<>();
		outerSubMap = new ConcurrentHashMap<>();
		groupPubMap = new ConcurrentHashMap<>();
		outerPubMap = new ConcurrentHashMap<>();
		joinedSubTopics = new HashSet<>();
		joinedUnsubTopics = new HashSet<>();
		notifyFlows = new HashMap<>();

		localCtl = new Controller(localAddr);
		groupRoutes = Collections.synchronizedSet(new HashSet<Route>());
	}
}
