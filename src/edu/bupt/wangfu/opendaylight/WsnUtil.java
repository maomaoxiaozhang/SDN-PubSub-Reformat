package edu.bupt.wangfu.opendaylight;


import edu.bupt.wangfu.info.device.Flow;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.opendaylight.rcver.AdminReceiver;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * Created by root on 15-10-6.
 */
public class WsnUtil extends SysInfo {
	public static void initNotifyTopicMap() {
		Flow fromAdmin = FlowUtil.getInstance().generateFlow(localSwtId, portWsn2Swt, "admin", "sys", 1, 10);
		FlowUtil.downFlow(groupCtl, fromAdmin, "add");
		Flow toAdmin = FlowUtil.getInstance().generateFlow(localSwtId, portWsn2Swt, "flood", "admin", "sys", 1, 10);
		FlowUtil.downFlow(groupCtl, toAdmin, "add");

		new Thread(new AdminReceiver()).start();
	}

	public static void initSysTopicMap() {
		Properties props = new Properties();
		String propertiesPath = "resources/SysTopic.properties";
		try {
			props.load(new FileInputStream(propertiesPath));
		} catch (FileNotFoundException e) {
			System.out.println("找不到系统主题配置文件");
		} catch (IOException e) {
			System.out.println("读取系统主题配置文件时发生IOException");
		}

		Enumeration<?> e = props.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			int value = Integer.valueOf(props.getProperty(key));

			String topicAddr = "11111111"//prefix ff 8bit
					+ "0000"//flag 0 4bit
					+ "1110"//global_scope e 4bit
					+ "00"//event_type sys 00 2bit
					+ "0000111"//topic_length 7 7bit
					+ "001"//queue_NO 1 3bit
					+ getFullLengthTopicCode(value);//topic_code 100bit
			sysTopicAddrMap.put(key, topicAddr);
		}
	}

	private static String getFullLengthTopicCode(int newTopicCode) {
		char[] tmp = new char[100];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = '0';
		}
		String s = Integer.toBinaryString(newTopicCode);
		for (int i = tmp.length - 1, j = s.length() - 1; j >= 0; i--, j--) {
			tmp[i] = s.charAt(j);
		}
		String res = String.valueOf(tmp);
		return res;
	}

	public static Map<String, String> getSysTopicMap() {
		return sysTopicAddrMap;
	}

	public static Map<String, String> getNotifyTopicList() {
		return notifyTopicAddrMap;
	}

	public static String getLinkedSwtId(String hostMac) {
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
}
