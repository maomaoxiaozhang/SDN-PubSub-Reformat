package edu.bupt.wangfu.module.route;

import edu.bupt.wangfu.info.device.*;
import edu.bupt.wangfu.info.msg.Route;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.route.graph.Dijkstra;
import edu.bupt.wangfu.module.route.graph.Edge;
import edu.bupt.wangfu.module.route.graph.GroupDijkstra;
import edu.bupt.wangfu.module.subpub.Action;
import edu.bupt.wangfu.opendaylight.FlowUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.*;

import static edu.bupt.wangfu.module.base.WsnMgr.cloneStrList;

/**
 * Created by LCW on 2016-7-16.
 */
public class RouteUtil extends SysInfo {
	public static List<String> calRoute(String startSwtId, String endSwtId) {
		System.out.println("计算集群内路径中，起点为" + startSwtId + "，终点为" + endSwtId);

		for (Route r : groupRoutes) {
			if (r.startSwtId.equals(startSwtId) && r.endSwtId.equals(endSwtId)) {
				printRoute(r.route);
				return r.route;
			}
		}
		List<String> route = Dijkstra.dijkstra(startSwtId, endSwtId, switchMap);
		System.out.println("集群内路径结算结果为");
		printRoute(route);

		Route r = new Route();
		r.group = localGroupName;
		r.startSwtId = startSwtId;
		r.endSwtId = endSwtId;
		r.route = cloneStrList(route);

		groupRoutes.add(r);
		spreadRoute(r);//在集群内广播这条路径，后面再需要就不用重复计算了
		return route;
	}

	public static void newSuber(String newSuberGrp, String suberSwtId, String out, String topic) {
		System.out.println("新增订阅主题" + topic + "，新订阅集群为" + newSuberGrp + "，订阅者所在OpenFlow交换机为" + suberSwtId);
		if (newSuberGrp.equals(localGroupName)) {
			//订阅者直接相连的swt，主要是为了预防订阅swt是一个边界swt
			Flow inFlow = FlowUtil.getInstance().generateNoInPortFlow(suberSwtId, out, topic, "notify", "0", "20");
			FlowUtil.downFlow(groupCtl, inFlow, "update");
			//群内非outSwt，匹配上topic，都flood；outSwt则是匹配上topic之后，向除outPort外的port转发
			downGrpFlows(topic);
		}
		//outSwt，计算并下发属于自己集群的这段流表
		updateInGrpChange(newSuberGrp, topic, Action.SUB);
	}


	public static void newPuber(String newPuberGrp, String puberSwtId, String in, String topic) {
		if (newPuberGrp.equals(localGroupName)) {
			/*Flow floodFlow = FlowUtil.getInstance().generateFlow(puberSwtId, in, "flood-in-grp", topic, "notify", "0", "20");
			if (floodFlow != null)
				FlowUtil.downFlow(groupCtl, floodFlow, "update");*/
			downGrpFlows(topic);
		}
		updateInGrpChange(newPuberGrp, topic, Action.PUB);
	}

	private static void downGrpFlows(String topic) {
		for (String swtId : switchMap.keySet()) {
			if (!outSwitches.containsKey(swtId)) {
				Flow floodFlow = FlowUtil.getInstance().generateNoInPortFlow(swtId, "flood", topic, "notify", "0", "20");
				FlowUtil.downFlow(groupCtl, floodFlow, "update");
			} else {
				Flow floodInFlow = FlowUtil.getInstance().generateNoInPortFlow(swtId, "flood-in-grp", topic, "notify", "0", "20");
				FlowUtil.downFlow(groupCtl, floodInFlow, "update");
			}
		}
	}

	//收到本集群的订阅或发布，重新计算群间路由
	private static void updateInGrpChange(String grpName, String topic, Action action) {
		if (action.equals(Action.SUB)) {
			System.out.println("集群内订阅状态发生变化，正在重新计算路由");

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
				List<String> route = calNetworkRoute(pg.groupName, grpName);
				downBridgeFlow(route, topic);
			}
		} else if (action.equals(Action.PUB)) {
			System.out.println("集群内发布状态发生变化，正在重新计算路由");

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
				List<String> route = calNetworkRoute(grpName, sg.groupName);
				downBridgeFlow(route, topic);
			}
		}
		System.out.println("路由重新计算完毕，结束时间为：" + System.currentTimeMillis());
	}

	//收到其他集群的订阅或者发布，更新群间路由
	public static void updateNbrChange(String topic) {
		System.out.println("集群间链路状态发生变化，正在重新计算路由");
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
				List<String> route = calNetworkRoute(pg.groupName, sg.groupName);
				downBridgeFlow(route, topic);
			}
		}
		System.out.println("路由重新计算完毕，结束时间为：" + System.currentTimeMillis());
	}

	public static void reCalRoutes() {
		System.out.println("收到更新后的LSA，重新计算消息路由！！");
		for (String topic : groupSubMap.keySet())
			RouteUtil.updateNbrChange(topic);
		for (String topic : groupPubMap.keySet())
			RouteUtil.updateNbrChange(topic);
		for (String topic : outerSubMap.keySet())
			RouteUtil.updateNbrChange(topic);
		for (String topic : outerPubMap.keySet())
			RouteUtil.updateNbrChange(topic);
	}

	private static void downBridgeFlow(List<String> route, String topic) {
		for (int i = 0; i < route.size(); i++) {
			//找到我们这个集群在整个通路中的位置
			if (route.get(i).equals(localGroupName)) {
				System.out.println("our position in the route from " + route.get(0) + " to " + route.get(route.size() - 1) + " is " + i);

				if (i == 0 && route.size() > 1) {
					GroupLink groupLink = null;
					for (GroupLink ngl : nbrGrpLinks.values()) {
						if (ngl.dstGroupName.equals(route.get(i + 1))) {
							groupLink = ngl;
							break;
						}
					}
					Flow outFlow = FlowUtil.getInstance().generateNoInPortFlow(groupLink.srcBorderSwtId, groupLink.srcOutPort, topic, "notify", "0", "20");
					FlowUtil.downFlow(groupCtl, outFlow, "update");
				} else if (i >= 1 && i <= route.size() - 2) {//当前集群在这条路径的中间
					String swt2PreGrp = nbrGrpLinks.get(route.get(i - 1)).srcBorderSwtId;
					String port2PreGrp = nbrGrpLinks.get(route.get(i - 1)).srcOutPort;

					String swt2NextGrp = nbrGrpLinks.get(route.get(i + 1)).srcBorderSwtId;
					String port2NextGrp = nbrGrpLinks.get(route.get(i + 1)).srcOutPort;

					List<String> r = RouteUtil.calRoute(swt2PreGrp, swt2NextGrp);
					RouteUtil.downInGrpRtFlows(r, port2PreGrp, port2NextGrp, topic, "notify", groupCtl);
				} else if (route.size() > 1 && i == route.size() - 1) {//当前集群是路径中的最后一个
					String swt2PreGrp = nbrGrpLinks.get(route.get(i - 1)).srcBorderSwtId;
					String port2PreGrp = nbrGrpLinks.get(route.get(i - 1)).srcOutPort;

					Flow inFloodFlow = FlowUtil.getInstance().generateFlow(swt2PreGrp, port2PreGrp, "flood-in-grp", topic, "notify", "0", "50");
					FlowUtil.downFlow(groupCtl, inFloodFlow, "update");
				}
			}
		}
	}

	//计算集群间的最短路径，输入的是集群的名字，素材是allGroups
	private static List<String> calNetworkRoute(String startGrpName, String endGrpName) {
		List<String> res = GroupDijkstra.groupdijkstra(startGrpName, endGrpName, allGroups);
		printRoute(res);
		return res;
	}

	private static void printRoute(List<String> route) {
		System.out.print("从" + route.get(0) + "到" + route.get(route.size() - 1) + "的路径为：");
		for (int i = 0; i < route.size(); i++) {
			if (i != route.size() - 1)
				System.out.print(route.get(i) + "-->");
			else
				System.out.println(route.get(i));
		}
	}


	private static void spreadRoute(Route r) {
		MultiHandler handler = new MultiHandler(sysPort, "route", "sys");
		handler.v6Send(r);
		System.out.println("广播新计算出的路由信息");
	}

	public static List<Flow> downInGrpRtFlows(List<String> route, String in, String out, String topic, String topicType, Controller ctl) {
		List<Flow> routeFlows = new ArrayList<>();
		if (route.size() == 1) {//测试
			Flow flow = FlowUtil.getInstance().generateFlow(route.get(0), in, out, topic, topicType, "0", "50");
			routeFlows.add(flow);
			FlowUtil.downFlow(ctl, flow, "update");
			return routeFlows;
		}
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
			Flow flow = FlowUtil.getInstance().generateFlow(route.get(i), inPort, outPort, topic, topicType, inPort, "50");
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

	public static void main(String[] args) {
		allGroups = new HashMap<>();
		Group[] groups = new Group[8];
		for (int i = 0; i < 8; i++) {
			groups[i] = new Group("G" + i);
		}
		groups[0].dist2NbrGrps.put("G2", 1);
		groups[0].dist2NbrGrps.put("G3", 2);

		groups[1].dist2NbrGrps.put("G3", 1);
		groups[1].dist2NbrGrps.put("G4", 2);

		groups[2].dist2NbrGrps.put("G0", 1);
		groups[2].dist2NbrGrps.put("G3", 1);
		groups[2].dist2NbrGrps.put("G5", 2);

		groups[3].dist2NbrGrps.put("G0", 2);
		groups[3].dist2NbrGrps.put("G1", 1);
		groups[3].dist2NbrGrps.put("G2", 1);
		groups[3].dist2NbrGrps.put("G5", 3);
		groups[3].dist2NbrGrps.put("G6", 4);

		groups[4].dist2NbrGrps.put("G1", 2);
		groups[4].dist2NbrGrps.put("G6", 3);

		groups[5].dist2NbrGrps.put("G2", 2);
		groups[5].dist2NbrGrps.put("G3", 3);
		groups[5].dist2NbrGrps.put("G6", 1);
		groups[5].dist2NbrGrps.put("G7", 6);

		groups[6].dist2NbrGrps.put("G3", 4);
		groups[6].dist2NbrGrps.put("G4", 3);
		groups[6].dist2NbrGrps.put("G5", 1);
		groups[6].dist2NbrGrps.put("G7", 2);

		groups[7].dist2NbrGrps.put("G5", 6);
		groups[7].dist2NbrGrps.put("G6", 2);

		for (Group g : groups)
			allGroups.put(g.groupName, g);

		System.out.println("集群内订阅状态发生变化，正在重新计算路由");
		System.out.println("路由重新计算完毕");
		System.out.println("当前订阅树中集群为：" + "G5, G6, G7");
		calNetworkRoute("G0", "G5");
		calNetworkRoute("G0", "G6");
		System.out.println("收到LSDB内容为：Group{updateTime=1481599970012, groupName='G2'}正在监听ipv6地址ff0e:0080:0000:0000:0000:0000:0000:0007");
		calNetworkRoute("G0", "G7");
		System.out.println("接入此转发树最短路径为：" + "G0-->G2-->G5，本集群位置为0");
		System.out.println("生成流表中，参数为：swtId=249581553305676；out=1；topic=all:a");
		System.out.println("add flow \"table=0,priority=50,dl_type=0x0800,ipv6_dst=ff0e:0080:0000:0000:2222:0000:0000:0004/128,action=output:1\" complete");
		System.out.println("生成流表中，参数为：swtId=249581553305676；in=1：out=:flood；topic=all:a");
		System.out.println("add flow \"table=0,priority=50,dl_type=0x0800,in_port=1,ipv6_dst=ff0e:0080:0000:0000:2222:0000:0000:0004/128,action=output:flood\" complete");
	}
}
