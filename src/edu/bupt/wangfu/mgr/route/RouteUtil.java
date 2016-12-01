package edu.bupt.wangfu.mgr.route;

import edu.bupt.wangfu.info.device.*;
import edu.bupt.wangfu.info.msg.Route;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.route.graph.Dijkstra;
import edu.bupt.wangfu.mgr.route.graph.Edge;
import edu.bupt.wangfu.mgr.route.graph.GroupDijkstra;
import edu.bupt.wangfu.mgr.subpub.Action;
import edu.bupt.wangfu.opendaylight.FlowUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.*;

/**
 * Created by LCW on 2016-7-16.
 */
public class RouteUtil extends SysInfo {
	public static List<String> test(String startSwtId, String endSwtId, Map<String, Switch> switchMap) {
		return Dijkstra.dijkstra(startSwtId, endSwtId, switchMap);
	}

	public static List<String> calRoute(String startSwtId, String endSwtId) {
		for (Route r : groupRoutes) {
			if (r.startSwtId.equals(startSwtId) && r.endSwtId.equals(endSwtId)) {
				return r.route;
			}
		}
		//冠群
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
			Flow inFlow = FlowUtil.getInstance().generateNoInPortFlow(suberSwtId, out, topic, "notify", 1, 10);
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
				Flow floodFlow = FlowUtil.getInstance().generateNoInPortFlow(swtId, "flood", topic, "notify", 1, 10);
				FlowUtil.downFlow(groupCtl, floodFlow, "update");
			} else {
				Flow floodInFlow = FlowUtil.getInstance().generateNoInPortFlow(swtId, "flood-in-grp", topic, "notify", 1, 10);
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
					Flow outFlow = FlowUtil.getInstance().generateNoInPortFlow(groupLink.srcBorderSwtId, groupLink.srcOutPort, topic, "notify", 1, 10);
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
		//也是冠群，输入的是集群的名字，素材是allGroups
		return GroupDijkstra.groupdijkstra(startGrpName, endGrpName, allGroups);
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
			Flow flow = FlowUtil.getInstance().generateFlow(route.get(i), inPort, outPort, topic, topicType, 0, 20);
			routeFlows.add(flow);
			FlowUtil.downFlow(ctl, flow, "add");
		}
		return routeFlows;
	}

	public static void delRouteFlows(List<Flow> routeFlows) {
		for (Flow flow : routeFlows) {
			FlowUtil.deleteFlow(groupCtl, flow);
		}
	}
}
