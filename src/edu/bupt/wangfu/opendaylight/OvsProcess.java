package edu.bupt.wangfu.opendaylight;

import edu.bupt.wangfu.info.device.Controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @ Created by HanB on 2016/11/29.
 */
public class OvsProcess {
	public static void addFlow(Controller ctrl, String tpid, String body) {
		String brName = RestProcess.getBrNameByTpid(ctrl, tpid);
		String cmd = "ovs-ofctl add-flow " + brName + " \"" + body + "\"";
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void deleteFlows(Controller ctrl, String tpid, String body) {
		String brName = RestProcess.getBrNameByTpid(ctrl, tpid);
		String cmd = "ovs-ofctl del-flows " + brName + " \"" + body + "\"";
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String dumpFlows(Controller ctrl, String tpid, String body) {
		String brName = RestProcess.getBrNameByTpid(ctrl, tpid);
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
}
