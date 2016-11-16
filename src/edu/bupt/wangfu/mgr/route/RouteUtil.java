package edu.bupt.wangfu.mgr.route;

import edu.bupt.wangfu.info.device.*;
import edu.bupt.wangfu.info.msg.Route;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.route.graph.Dijkstra;
import edu.bupt.wangfu.mgr.route.graph.Edge;
import edu.bupt.wangfu.mgr.subpub.Action;
import edu.bupt.wangfu.opendaylight.FlowUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;
import edu.bupt.wangfu.opendaylight.RestProcess;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by LCW on 2016-7-16.
 */
public class RouteUtil extends SysInfo {
	public static void main(String[] args) {

			String url = "http://10.108.165.188:8181/restconf/operational/network-topology:network-topology/";

			//测试用
		HashMap<String, Host> hostMap = new HashMap<>();
		HashMap<String, Switch> switchMap = new HashMap<>();
		HashSet<Edge> groupEdges = new HashSet<>();
		HashMap<String, Switch> outSwitches = new HashMap<>();
			//结束
			hostMap.clear();
			switchMap.clear();
			groupEdges.clear();
			outSwitches.clear();

			String body = RestProcess.doClientGet(url);
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



		List<String> res = test("138650386896193", "143995463036234",switchMap);
		System.out.println(123);
	}

	public static List<String> test(String startSwtId, String endSwtId,Map<String,Switch> switchMap) {
		return Dijkstra.dijkstra(startSwtId, endSwtId, switchMap);
	}

	public static List<String> calRoute(String startSwtId, String endSwtId) {
		for (Route r : groupRoutes) {
			if (r.startSwtId.equals(startSwtId) && r.endSwtId.equals(endSwtId)) {
				return r.route;
			}
		}
		//TODO 冠群
		List<String> route = Dijkstra.dijkstra(startSwtId, endSwtId, switchMap);

		Route r = new Route();
		r.group = localGroupName;
		r.startSwtId = startSwtId;
		r.endSwtId = endSwtId;
		r.route = route;

		groupRoutes.add(r);
		spreadRoute(r);//在集群内广播这条路径，后面再需要就不用重复计算了
		return route;
	}

	public static void newSuber(String newSuberGrp, String suberSwtId, String out, String topic) {
		if (newSuberGrp.equals(localGroupName)) {
			//订阅者直接相连的swt，主要是为了预防订阅swt是一个边界swt
			Flow inFlow = FlowUtil.getInstance().generateFlow(suberSwtId, out, topic, "notify", 1, 10);
			FlowUtil.downFlow(groupCtl, inFlow, "update");
			//群内非outSwt，匹配上topic，都flood；outSwt则是匹配上topic之后，向除outPort外的port转发
			downGrpFlows(topic);
		}
		//outSwt，计算并下发属于自己集群的这段流表
		calGraph(newSuberGrp, topic, Action.SUB);
	}


	public static void newPuber(String newPuberGrp, String puberSwtId, String in, String topic) {
		if (newPuberGrp.equals(localGroupName)) {
			Flow floodFlow = FlowUtil.getInstance().generateFlow(puberSwtId, in, "flood-in-grp", topic, "notify", 1, 10);
			FlowUtil.downFlow(groupCtl, floodFlow, "update");
			downGrpFlows(topic);
		}
		calGraph(newPuberGrp, topic, Action.PUB);
	}

	private static void downGrpFlows(String topic) {
		for (String swtId : switchMap.keySet()) {
			if (!outSwitches.containsKey(swtId)) {
				Flow floodFlow = FlowUtil.getInstance().generateFlow(swtId, "flood", topic, "notify", 1, 10);
				FlowUtil.downFlow(groupCtl, floodFlow, "update");
			} else {
				Flow floodInFlow = FlowUtil.getInstance().generateFlow(swtId, "flood-in-grp", topic, "notify", 1, 10);
				FlowUtil.downFlow(groupCtl, floodInFlow, "update");
			}
		}
	}

	//计算集群间的路由
	private static void calGraph(String grpName, String topic, Action action) {
		if (action.equals(Action.SUB)) {
			Set<Group> puberGrps = new HashSet<>();
			//找到所有发送这个主题以及它孩子主题的集群
			for (Group grp : allGroups.values()) {
				for (String grpPub : grp.pubMap.keySet()) {
					if (grpPub.contains(topic)) {
						puberGrps.add(grp);
						break;
					}
				}
			}
			for (Group pg : puberGrps) {
				List<String> route = calGraphRoute(pg.groupName, grpName);
				downBridgeFlow(route, topic);
			}
		} else if (action.equals(Action.PUB)) {
			Set<Group> suberGrps = new HashSet<>();
			//找到所有订阅这个主题以及它父亲主题的集群
			for (Group grp : allGroups.values()) {
				for (String grpSub : grp.subMap.keySet()) {
					if (topic.contains(grpSub)) {
						suberGrps.add(grp);
						break;
					}
				}
			}
			for (Group sg : suberGrps) {
				List<String> route = calGraphRoute(grpName, sg.groupName);
				downBridgeFlow(route, topic);
			}
		}
	}

	public static void reCalGraph(String topic) {
		Set<Group> suberGrps = new HashSet<>();
		for (Group grp : allGroups.values()) {
			for (String grpSub : grp.subMap.keySet()) {
				if (topic.contains(grpSub)) {
					suberGrps.add(grp);
					break;
				}
			}
		}

		Set<Group> puberGrps = new HashSet<>();
		for (Group grp : allGroups.values()) {
			for (String grpPub : grp.pubMap.keySet()) {
				if (grpPub.contains(topic)) {
					puberGrps.add(grp);
					break;
				}
			}
		}
		for (Group pg : puberGrps) {
			for (Group sg : suberGrps) {
				List<String> route = calGraphRoute(pg.groupName, sg.groupName);
				downBridgeFlow(route, topic);
			}
		}
	}

	private static void downBridgeFlow(List<String> route, String topic) {
		for (int i = 0; i < route.size(); i++) {
			//找到我们这个集群在整个通路中的位置
			if (route.get(i).equals(localGroupName)) {
				if (i == 0 && route.size() > 1) {
					GroupLink groupLink = null;
					for (GroupLink ngl : nbrGrpLinks.values()) {
						if (ngl.dstGroupName.equals(route.get(i + 1))) {
							groupLink = ngl;
							break;
						}
					}
					Flow outFlow = FlowUtil.getInstance().generateFlow(groupLink.srcBorderSwtId, groupLink.srcOutPort, topic, "notify", 1, 10);
					FlowUtil.downFlow(groupCtl, outFlow, "update");
				} else if (i >= 1 && i <= route.size() - 2) {//当前集群在这条路径的中间
					String swt2PreGrp = nbrGrpLinks.get(route.get(i - 1)).srcBorderSwtId;
					String port2PreGrp = nbrGrpLinks.get(route.get(i - 1)).srcOutPort;

					String swt2NextGrp = nbrGrpLinks.get(route.get(i + 1)).srcBorderSwtId;
					String port2NextGrp = nbrGrpLinks.get(route.get(i + 1)).srcOutPort;

					List<String> r = RouteUtil.calRoute(swt2PreGrp, swt2NextGrp);
					RouteUtil.downRouteFlows(r, port2PreGrp, port2NextGrp, topic, "notify", groupCtl);
				} else if (route.size() > 1 && i == route.size() - 1) {//当前集群是路径中的最后一个
					String swt2PreGrp = nbrGrpLinks.get(route.get(i - 1)).srcBorderSwtId;
					String port2PreGrp = nbrGrpLinks.get(route.get(i - 1)).srcOutPort;

					Flow inFloodFlow = FlowUtil.getInstance().generateFlow(swt2PreGrp, port2PreGrp, "flood", topic, "notify", 1, 10);
					FlowUtil.downFlow(groupCtl, inFloodFlow, "update");
				}
			}
		}
	}

	//计算集群间的最短路径
	private static List<String> calGraphRoute(String startGrpName, String endGrpName) {
		//TODO 也是冠群，输入的是集群的名字，素材是allGroups
		return new ArrayList<>();
	}


	private static void spreadRoute(Route r) {
		MultiHandler handler = new MultiHandler(uPort, "route", "sys");
		handler.v6Send(r);
	}

	public static List<Flow> downRouteFlows(List<String> route, String in, String out, String topic, String topicType, Controller ctl) {
		List<Flow> routeFlows = new ArrayList<>();
		for (int i = 0; i < route.size(); i++) {
			Switch pre;
			Switch cur;
			Switch next;

			String inPort = (i == 0 ? in : null);
			String outPort = (i == route.size() - 1 ? out : null);

			for (Edge e : groupEdges) {
				if (i != 0) {
					pre = switchMap.get(route.get(i - 1));
					cur = switchMap.get(route.get(i));
					if (e.getStart().equals(pre.id) && e.getFinish().equals(cur.id))
						inPort = e.finishPort;
				}
				if (i != route.size() - 1) {
					cur = switchMap.get(route.get(i));
					next = switchMap.get(route.get(i + 1));
					if (e.getStart().equals(cur.id) && e.getFinish().equals(next.id))
						outPort = e.startPort;
				}
			}
			Flow flow = FlowUtil.getInstance().generateFlow(route.get(i), inPort, outPort, topic, topicType, 1, 10);
			routeFlows.add(flow);
			FlowUtil.downFlow(ctl, flow, "update");
		}
		return routeFlows;
	}

	public static void delRouteFlows(List<Flow> routeFlows) {
		for (Flow flow : routeFlows) {
			FlowUtil.deleteFlow(groupCtl, flow);
		}
	}

	//下发同步流表，使wsn计算出来的新route可以全网同步
	public static void downSyncGroupRouteFlow() {
		Flow floodOutFlow = FlowUtil.getInstance().generateFlow(localSwtId, portWsn2Swt, "flood", "route", "sys", 1, 10);
		FlowUtil.downFlow(groupCtl, floodOutFlow, "add");

		for (Switch swt : switchMap.values()) {
			for (String p : swt.neighbors.keySet()) {
				Flow floodInFlow = FlowUtil.getInstance().generateFlow(localSwtId, p, "flood", "route", "sys", 1, 10);
				FlowUtil.downFlow(groupCtl, floodInFlow, "add");
			}
		}
	}
}
