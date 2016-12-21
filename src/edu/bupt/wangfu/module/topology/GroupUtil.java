package edu.bupt.wangfu.module.topology;

import edu.bupt.wangfu.info.device.*;
import edu.bupt.wangfu.info.msg.AllGrps;
import edu.bupt.wangfu.module.base.Config;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.route.RouteUtil;
import edu.bupt.wangfu.module.route.graph.Edge;
import edu.bupt.wangfu.opendaylight.FlowUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;
import edu.bupt.wangfu.opendaylight.RestProcess;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static edu.bupt.wangfu.module.base.WsnMgr.cloneSetMap;


/**
 * @ Created by lenovo on 2016-10-16.
 */
public class GroupUtil extends SysInfo {
	private static RefreshGroup refreshTask = new RefreshGroup();
	private static Timer refreshTimer = new Timer();

	public static void main(String[] args) {
		Config.configure();
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
		JSONObject topology = net_topology.getJSONArray("topology").getJSONObject(0);

		if (topology.has("node")) {
			JSONArray nodes = topology.getJSONArray("node");
			for (int i = 0; i < nodes.length(); i++) {
				String node_id = nodes.getJSONObject(i).getString("node-id");
				if (node_id.contains("host")) {
					String swt = nodes.getJSONObject(i).getJSONArray("host-tracker-service:attachment-points").getJSONObject(0).getString("tp-id");
					String port = swt.split(":")[2];
					String swtId = swt.split(":")[1];
					String ip = nodes.getJSONObject(i).getJSONArray("host-tracker-service:addresses").getJSONObject(0).getString("ip");
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
					JSONArray nbs = nodes.getJSONObject(i).getJSONArray("termination-point");
					for (int j = 0; j < nbs.length(); j++) {
						String port = nbs.getJSONObject(j).getString("tp-id").split(":")[2];
						swt.portSet.add(port);
					}
					switchMap.put(swtId, swt);
				}
			}
		}

		//从groupSub和groupPub中清理本集群内失效的swt
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

		Group localGrp = new Group(localGroupName);
		localGrp.subMap = cloneSetMap(groupSubMap);
		localGrp.pubMap = cloneSetMap(groupPubMap);
		localGrp.updateTime = System.currentTimeMillis();
		allGroups.put(localGroupName, localGrp);

		if (topology.has("link")) {
			JSONArray links = topology.getJSONArray("link");
			for (int i = 0; i < links.length(); i++) {
				String link_id = links.getJSONObject(i).getString("link-id");
				String dest = links.getJSONObject(i).getJSONObject("destination").getString("dest-tp");
				String src = links.getJSONObject(i).getJSONObject("source").getString("source-tp");
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

			for (int i = 0; i < links.length(); i++) {
				String link_id = links.getJSONObject(i).getString("link-id");
				if (!link_id.contains("host")) {//这说明这个连接是一个swt--swt的连接
					Edge e = new Edge();
					String[] s = links.getJSONObject(i).getJSONObject("destination").getString("dest-tp").split(":");
					e.setStart(s[1]);
					e.startPort = s[2];

					String[] f = links.getJSONObject(i).getJSONObject("source").getString("source-tp").split(":");
					e.setFinish(f[1]);
					e.finishPort = f[2];

					groupEdges.add(e);
				}
			}
		}

		//把switchMap中有outPort的swt放到outSwitches中
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


	}

	private static boolean isGrpLinked(GroupLink gl) {
		//之前连接着对面group的swt还在，暴露的outPort还是一样，就认为对面连接的还是同一个集群
		return outSwitches.containsKey(gl.srcBorderSwtId)
				&& outSwitches.get(gl.srcBorderSwtId).portSet.contains(gl.srcOutPort);
	}

	public static void spreadLocalGrp() {
		Group g = allGroups.get(localGroupName);
		MultiHandler handler = new MultiHandler(sysPort, "lsa", "sys");
		handler.v6Send(g);
		System.out.println("广播当前集群LSA" + g.toString());
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
			downRcvHelloFlow();//接收Hello消息的流表
			spreadLocalGrp();//全网定时广播本集群内容更新
			spreadAllGrps();//集群内定时广播自己拥有的allGroups，确保每个新增加的节点都有最新的全网集群信息
			downSubPubFlow();//下发注册流表，之后如果wsn要产生新订阅或新发布，就可以通过它扩散到全网
			downSynGrpRtFlow();//下发route同步流表，使wsn计算出来的新route可以在集群内同步
			downLeadTableFlow();//下发优先级为10的导向流表，将消息引导到第二级流表
			if (localCtl.url.equals(groupCtl.url))
				downRestFlow();//下发访问groupCtl的flood流表

		}

		private void downLeadTableFlow() {
			for (Switch swt : switchMap.values()) {
				for (int i = 0; i < 15; i++) {
					String inPort = String.valueOf(i);
					Flow leadTabFlow = FlowUtil.getInstance().generateLeadTabFlow(swt.id, inPort, "0", "10", inPort);
					FlowUtil.downFlow(groupCtl, leadTabFlow, "add");
				}
			}
		}

		private void downRcvHelloFlow() {
			for (Switch swt : outSwitches.values()) {
				for (String out : swt.portSet) {
					if (!out.equals("LOCAL")) {
						//这条路径保证outPort进来hello消息可以传到groupCtl
						List<String> inHello = RouteUtil.calRoute(swt.id, localSwtId);
						//这里流表的out设置为portWsn2Swt，是因为只有在groupCtl == localCtl时才调用这个函数
						RouteUtil.downInGrpRtFlows(inHello, out, portWsn2Swt, "hello", "sys", groupCtl);
					}
				}
			}
			System.out.println("down heart flows complete");
		}

		private void spreadAllGrps() {
			for (Switch swt : switchMap.values()) {
				Flow floodFlow = FlowUtil.getInstance().generateNoInPortFlow(swt.id, "flood", "lsa", "sys", "0", "50");
				FlowUtil.downFlow(groupCtl, floodFlow, "add");
			}

			MultiHandler handler = new MultiHandler(sysPort, "lsa", "sys");
			AllGrps ags = new AllGrps(allGroups);
			handler.v6Send(ags);
			System.out.println("spreading updated allGroups");
		}

		private void downSubPubFlow() {
			for (Switch swt : switchMap.values()) {
				Flow floodFlow = FlowUtil.getInstance().generateNoInPortFlow(swt.id, "flood", "sub", "sys", "0", "50");
				FlowUtil.downFlow(groupCtl, floodFlow, "add");
				floodFlow = FlowUtil.getInstance().generateNoInPortFlow(swt.id, "flood", "pub", "sys", "0", "50");
				FlowUtil.downFlow(groupCtl, floodFlow, "add");
			}
		}

		private void downSynGrpRtFlow() {
			Flow floodOutFlow = FlowUtil.getInstance().generateFlow(localSwtId, portWsn2Swt, "flood", "route", "sys", "0", "50");
			FlowUtil.downFlow(groupCtl, floodOutFlow, "add");

			for (Switch swt : switchMap.values()) {
				for (String p : swt.neighbors.keySet()) {
					Flow floodInFlow = FlowUtil.getInstance().generateFlow(swt.id, p, "flood", "route", "sys", "0", "50");
					FlowUtil.downFlow(groupCtl, floodInFlow, "add");
				}
			}
		}

		private void downRestFlow() {
			for (Switch swt : switchMap.values()) {
				Flow fromGrpCtlFlow = FlowUtil.getInstance().generateRestFlow(swt.id, "flood", "0", "50", "src:" + localAddr);
				FlowUtil.downFlow(groupCtl, fromGrpCtlFlow, "add");
				Flow toGrpCtlFlow = FlowUtil.getInstance().generateRestFlow(swt.id, "flood", "0", "50", "dst:" + localAddr);
				FlowUtil.downFlow(groupCtl, toGrpCtlFlow, "add");
			}
		}
	}
}
