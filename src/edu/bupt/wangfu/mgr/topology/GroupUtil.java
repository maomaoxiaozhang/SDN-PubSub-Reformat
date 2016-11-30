package edu.bupt.wangfu.mgr.topology;

import edu.bupt.wangfu.info.device.*;
import edu.bupt.wangfu.info.msg.AllGrps;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.route.graph.Edge;
import edu.bupt.wangfu.opendaylight.FlowUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;
import edu.bupt.wangfu.opendaylight.RestProcess;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @ Created by lenovo on 2016-10-16.
 */
public class GroupUtil extends SysInfo {
	private static RefreshGroup refreshTask = new RefreshGroup();
	private static Timer refreshTimer = new Timer();

	public static void main(String[] args) {
		getGrpTopo(new Controller("10.108.165.188:8181"));
	}

	public static void initGroup() {
		refreshTimer.schedule(refreshTask, 0, refreshPeriod);
	}

	//初始化hostMap，switchMap，outPorts
	private static void getGrpTopo(Controller controller) {
		String url = controller.url + "/restconf/operational/network-topology:network-topology/";

		hostMap.clear();
		switchMap.clear();
		groupEdges.clear();
		outSwitches.clear();

		String body = RestProcess.doClientGet(url);
		assert body != null;
		JSONObject json = new JSONObject(body);
		JSONObject net_topology = json.getJSONObject("network-topology");
		JSONArray topology = net_topology.getJSONArray("topology");
		JSONArray nodes = topology.getJSONObject(0).getJSONArray("node");
		for (int j = 0; j < nodes.length(); j++) {
			String node_id = nodes.getJSONObject(j).getString("node-id");
			if (node_id.contains("host")) {
				String swt = nodes.getJSONObject(j).getJSONArray("host-tracker-service:attachment-points").getJSONObject(0).getString("tp-id");
				String port = swt.split(":")[2];
				String swtId = swt.split(":")[1];
				String ip = nodes.getJSONObject(j).getJSONArray("host-tracker-service:addresses").getJSONObject(0).getString("ip");
				String mac = node_id.substring(5, node_id.length());

				Host host = new Host(ip);
				host.mac = mac;
				host.swtId = swtId;
				host.port = port;
				hostMap.put(mac, host);
			} else if (node_id.contains("openflow")) {
				String swtId = node_id.split(":")[1];
				Switch swt = new Switch(swtId);
				swt.portSet = new HashSet<>();
				JSONArray nbs = nodes.getJSONObject(j).getJSONArray("termination-point");
				for (int i = 0; i < nbs.length(); i++) {
					String port = nbs.getJSONObject(i).getString("tp-id").split(":")[2];
					swt.portSet.add(port);
				}
				switchMap.put(swtId, swt);
			}
		}

		//清理本集群内失效的swt
		for (Set<String> subSwts : groupSubMap.values()) {
			for (String swt_Port : subSwts) {
				if (!switchMap.keySet().contains(swt_Port.split(":")[0])) {//说明原本有订阅的这个swt丢失了，那么就要在subMap里面把它清除
					subSwts.remove(swt_Port);
				}
			}
		}
		for (Set<String> pubSwts : groupPubMap.values()) {
			for (String swt_Port : pubSwts) {
				if (!switchMap.keySet().contains(swt_Port.split(":")[0])) {
					pubSwts.remove(swt_Port);
				}
			}
		}
		Group localGrp = allGroups.get(localGroupName) == null ? new Group(localGroupName) : allGroups.get(localGroupName);
		localGrp.subMap = groupSubMap;
		localGrp.pubMap = groupPubMap;
		localGrp.updateTime = System.currentTimeMillis();
		spreadLocalGrp(localGrp);

		for (int i = 0; i < topology.length(); i++) {
			JSONArray links = topology.getJSONObject(i).getJSONArray("link");
			for (int j = 0; j < links.length(); j++) {
				String link_id = links.getJSONObject(j).getString("link-id");
				String dest = links.getJSONObject(j).getJSONObject("destination").getString("dest-tp");
				String src = links.getJSONObject(j).getJSONObject("source").getString("source-tp");
				String hostMac = null, swtId1, port1, swtId2 = null, port2 = null;
				//获取连接关系
				if (link_id.contains("host")) {
					hostMac = dest.contains("host") ? dest.substring(5, dest.length()) : src.substring(5, src.length());
					swtId1 = dest.contains("host") ? src.split(":")[1] : dest.split(":")[1];
					port1 = dest.contains("host") ? src.split(":")[2] : dest.split(":")[2];
				} else {
					swtId1 = src.split(":")[1];
					port1 = src.split(":")[2];
					swtId2 = dest.split(":")[1];
					port2 = dest.split(":")[2];
				}
				//修改连接关系
				if (hostMac != null) {
					Host h = hostMap.get(hostMac);
					switchMap.get(swtId1).addNeighbor(port1, h);
					switchMap.get(swtId1).portSet.remove(port1);
				} else {
					Switch s1 = switchMap.get(swtId1);
					Switch s2 = switchMap.get(swtId2);
					s1.addNeighbor(port1, s2);
					s1.portSet.remove(port1);
					s2.addNeighbor(port2, s1);
					s2.portSet.remove(port2);
				}
			}
		}

		for (int i = 0; i < topology.length(); i++) {
			JSONArray links = topology.getJSONObject(i).getJSONArray("link");
			for (int j = 0; j < links.length(); j++) {
				String link_id = links.getJSONObject(j).getString("link-id");
				if (!link_id.contains("host")) {//这说明这个连接是一个swt--swt的连接
					Edge e = new Edge();
					String[] s = links.getJSONObject(j).getJSONObject("destination").getString("dest-tp").split(":");
					e.setStart(s[1]);
					e.startPort = s[2];

					String[] f = links.getJSONObject(j).getJSONObject("source").getString("source-tp").split(":");
					e.setFinish(f[1]);
					e.finishPort = f[2];

					groupEdges.add(e);
				}
			}
		}

		for (Switch swt : switchMap.values()) {
			if (swt.portSet.size() > 1) {
				outSwitches.put(swt.id, swt);
			}
		}
		for (GroupLink gl : nbrGrpLinks.values()) {
			if (!isGrpLinked(gl)) {//两集群不相连，则删掉二者之间的邻居关系
				nbrGrpLinks.remove(gl.dstGroupName);

				Group g = allGroups.get(gl.srcGroupName);
				g.dist2NbrGrps.remove(gl.dstGroupName);
				g.updateTime = System.currentTimeMillis();

				Group g2 = allGroups.get(gl.dstGroupName);
				g2.dist2NbrGrps.remove(gl.srcGroupName);
				g2.updateTime = System.currentTimeMillis();
				//后面在定时任务里已经有spreadAllGrps()了
			}
		}

		for (String id : switchMap.keySet()) {
			Switch swt = switchMap.get(id);
			System.out.println("Switch " + id + " has " + (swt.portSet.size() - 1) + " port(s) connected to other " +
					"group(s), and " + swt.neighbors.keySet().size() + " port(s) connected to in group device(s)");
		}
	}

	private static boolean isGrpLinked(GroupLink gl) {
		//之前连接着对面group的swt还在，暴露的outPort还是一样，就认为对面连接的还是同一个集群
		return outSwitches.containsKey(gl.srcBorderSwtId)
				&& outSwitches.get(gl.srcBorderSwtId).portSet.contains(gl.srcOutPort);
	}

	public static void spreadLocalGrp(Group g) {
		MultiHandler handler = new MultiHandler(uPort, "lsa", "sys");
		handler.v6Send(g);
	}

	//更新group拓扑信息
	private static class RefreshGroup extends TimerTask {
		@Override
		public void run() {
			getGrpTopo(groupCtl);
			while (switchMap.size() == 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			//定时广播自己集群的信息，确保每个新增加的节点都有最新的全网集群信息
			spreadAllGrps();
			//下发注册流表，之后如果wsn要产生新订阅或新发布，就可以通过它扩散到全网
			downSubPubFlow();
			//下发同步流表，使wsn计算出来的新route可以在集群内同步
			downSynGrpRtFlow();
			//下发访问groupCtl的flood流表
			if (localCtl.equals(groupCtl))
				downRestFlow();

		}

		private void spreadAllGrps() {
			for (Switch swt : switchMap.values()) {
				Flow floodFlow = FlowUtil.getInstance().generateNoInPortFlow(swt.id, "flood", "lsa", "sys", 1, 10);//TODO 优先级是越大越靠后吗？
				FlowUtil.downFlow(groupCtl, floodFlow, "add");
			}

			MultiHandler handler = new MultiHandler(uPort, "lsa", "sys");
			AllGrps ags = new AllGrps(allGroups);
			handler.v6Send(ags);
		}

		private void downSubPubFlow() {
			for (Switch swt : switchMap.values()) {
				//这里也是不需要定义in_port，只需要出现这样的消息，就全网flood
				Flow floodFlow = FlowUtil.getInstance().generateNoInPortFlow(swt.id, "flood", "sub", "sys", 1, 10);//TODO 优先级是越大越靠后吗？
				FlowUtil.downFlow(groupCtl, floodFlow, "add");
				floodFlow = FlowUtil.getInstance().generateNoInPortFlow(swt.id, "flood", "pub", "sys", 1, 10);
				FlowUtil.downFlow(groupCtl, floodFlow, "add");
			}
		}

		private void downSynGrpRtFlow() {
			Flow floodOutFlow = FlowUtil.getInstance().generateFlow(localSwtId, portWsn2Swt, "flood", "route", "sys", 1, 10);
			FlowUtil.downFlow(groupCtl, floodOutFlow, "add");

			for (Switch swt : switchMap.values()) {
				for (String p : swt.neighbors.keySet()) {
					Flow floodInFlow = FlowUtil.getInstance().generateFlow(localSwtId, p, "flood", "route", "sys", 1, 10);
					FlowUtil.downFlow(groupCtl, floodInFlow, "add");
				}
			}
		}

		//groupCtl下发全集群各swt上flood流表
		private void downRestFlow() {
			for (Switch swt : switchMap.values()) {
				String id = swt.id;
				//可以不匹配端口，直接匹配v4_dst和v6_dst(topic)
				String v4Addr = localAddr;
				Flow fromGroupCtlFlow = FlowUtil.getInstance().generateRestFlow(id, "flood", 1, 10, "src:"+v4Addr);
				FlowUtil.downFlow(groupCtl, fromGroupCtlFlow, "add");

				Flow toGroupCtlFlow = FlowUtil.getInstance().generateRestFlow(id, "flood", 1, 10, "dst:"+v4Addr);
				FlowUtil.downFlow(groupCtl, toGroupCtlFlow, "add");
			}
		}
	}
}
