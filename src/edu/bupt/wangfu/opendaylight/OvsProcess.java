package edu.bupt.wangfu.opendaylight;

import edu.bupt.wangfu.info.device.Controller;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @ Created by HanB on 2016/11/29.
 */
public class OvsProcess {
	public static void addFlow(Controller ctrl, String tpid, String body) {
		String brName = getBrNameByTpid(ctrl, tpid);
		String cmd = "ovs-ofctl add-flow " + brName + " \"" + body + "\"";
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void deleteFlows(Controller ctrl, String tpid, String body) {
		String brName = getBrNameByTpid(ctrl, tpid);
		String cmd = "ovs-ofctl del-flows " + brName + " \"" + body + "\"";
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String dumpFlows(Controller ctrl, String tpid, String body) {
		String brName = getBrNameByTpid(ctrl, tpid);
		String cmd = "ovs-ofctl dump-flows " + brName + " \"" + body + "\"";
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}

	public static String getBrNameByTpid(Controller ctrl, String tpid) {
		String brName = "";
		String url = ctrl.url + "/restconf/operational/opendaylight-inventory:nodes/node/" + tpid;
		String body = RestProcess.doClientGet(url);
		assert body != null;
		try {
			JSONObject json = new JSONObject(body);
			JSONArray node = json.getJSONArray("node");
			JSONArray node_connector = node.getJSONObject(0).getJSONArray("node-connector");
			for (int i = 0; i < node_connector.length(); i++) {
				JSONObject term = node_connector.getJSONObject(i);
				String portName = term.getString("flow-node-inventory:name");
				if (portName.startsWith("br")) {
					brName = portName;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return brName;
	}
}
