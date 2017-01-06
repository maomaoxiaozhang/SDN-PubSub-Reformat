package edu.bupt.wangfu.opendaylight;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import edu.bupt.wangfu.info.device.Controller;
import edu.bupt.wangfu.module.base.SysInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * @ Created by HanB on 2016/11/29.
 */
public class OvsProcess extends SysInfo {
	public static void addFlow(Controller ctrl, String tpid, String body) {
		String brName = RestProcess.getBrNameByTpid(ctrl, tpid);
		String cmd = "ovs-ofctl add-flow " + brName + " " + body;

		remoteExcuteCommand(cmd);

		/*try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	public static void deleteFlows(Controller ctrl, String tpid, String body) {
		String brName = RestProcess.getBrNameByTpid(ctrl, tpid);
		String cmd = "ovs-ofctl del-flows " + brName + " " + body;

		remoteExcuteCommand(cmd);

		/*try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	public static String dumpFlows(Controller ctrl, String tpid, String body) {
		String brName = RestProcess.getBrNameByTpid(ctrl, tpid);
		String cmd = "ovs-ofctl dump-flows " + brName + " " + body;

		return remoteExcuteCommand(cmd);

		/*BufferedReader br = null;
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
		return sb.toString();*/
	}

	//测试
	public static String remoteExcuteCommand(String cmd) {
		StringBuilder sb = new StringBuilder();
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession("root", "192.168.100.3", 22);
			session.setPassword("123456");
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			ChannelExec channel = (ChannelExec) session.openChannel("exec");

			BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));

			channel.setCommand(cmd);
			channel.connect();

			String msg;
			while ((msg = in.readLine()) != null) {
				sb.append(msg).append("\n");
			}

			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}
