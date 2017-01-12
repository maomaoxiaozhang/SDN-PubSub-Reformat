package edu.bupt.wangfu.opendaylight;

import edu.bupt.wangfu.info.device.Controller;
import edu.bupt.wangfu.info.device.Flow;
import edu.bupt.wangfu.info.device.Switch;
import edu.bupt.wangfu.module.base.SysInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @ Created by lenovo on 2016-5-18.
 */
public class FlowUtil extends SysInfo {
	private static FlowUtil ins;
	private int flowcount;

	private FlowUtil() {
		this.flowcount = 0;
	}

	public static synchronized FlowUtil getInstance() {
		if (ins == null)
			ins = new FlowUtil();
		return ins;
	}

	public static void deleteFlow(Controller controller, Flow flow) {
		//RestProcess.doClientDelete(controller, flow.swtId, flow.toStringOutput());
		OvsProcess.deleteFlows(controller, flow.swtId, flow.toStringDelete());
	}

	public static void downFlow(Controller controller, Flow flow, String action) {
		if (flow == null) {
			return;
		}
		//这里还要考虑下发到具体哪个流表里，看要执行的动作是 更新流表项 还是 添加新流表项
		// action == "add" "update"
		//RestProcess.doClientPost(controller, flow.swtId, flow.toStringOutput());
		if (action.equals("update")) { // 如果是更新的流表，先查看已下发的出端口，然后将新的端口添加进去
			String dumpResult = OvsProcess.dumpFlows(controller, flow.swtId, flow.toStringDelete());
			if (dumpResult.split("\n").length < 2) {
				OvsProcess.addFlow(controller, flow.swtId, flow.toStringOutput());
			} else {
				String outPort = "";
				for (int i = 1; i < dumpResult.split("\n").length; i++) {
					String singleFlow = dumpResult.split("\n")[i];
					singleFlow = singleFlow.substring(singleFlow.indexOf("actions="));
					singleFlow = singleFlow.substring(singleFlow.indexOf("=") + 1);
					ArrayList<String> list = new ArrayList<>();
					for (int j = 0; j < singleFlow.split(",").length; j++) {
						if (singleFlow.split(",")[j].equals("LOCAL") && !list.contains("LOCAL"))
							list.add("LOCAL");
						if (singleFlow.split(",")[j].contains(":")) {
							String str = singleFlow.split(",")[j].split(":")[1];
							if (!list.contains(str))
								list.add(str);
						}
					}
					if (!list.contains(flow.out))
						list.add(flow.out);
					for (String s : list)
						outPort += ("," + s);
				}
				outPort = outPort.substring(1);
				flow.out = outPort;
				OvsProcess.addFlow(controller, flow.swtId, flow.toStringOutput());
			}
//			System.out.println("update flow \"" + flow.toStringOutput() + "\" complete");
		} else if (action.equals("add")) {//把旧流表覆盖掉
			OvsProcess.addFlow(controller, flow.swtId, flow.toStringOutput());
//			System.out.println("add flow \"" + flow.toStringOutput() + "\" complete");
		}
	}

	//这里使用单例模式是为了方便计数flowcount，每条流表的编号必须不一样
	public Flow generateFlow(String swtId, String in, String out, String topic, String topicType, String t_id, String pri) {
		System.out.println("生成简单流表中，参数为：swtId=" + swtId + "；in=" + in + "；out=" + out + "；topic=" + topic);
		Set<Flow> topicFlowSet;
		//将route中的每一段flow都添加到set中，保证后面不用重复下发，控制flowcount
		if (notifyFlows.get(topic) != null) {
			topicFlowSet = notifyFlows.get(topic);
			for (Flow flow : topicFlowSet) {
				if (flow.swtId.equals(swtId)
						&& (flow.in == null || flow.in.equals(in))
						&& flow.out.equals(out)
						&& flow.topic.equals(topic)) {
					return flow;
				}
			}
		} else {
			topicFlowSet = new HashSet<>();
		}
		//之前没生成过这条流表，需要重新生成
		// 非outport flood流表
		if (out.equals("flood-in-grp")) {
			if (switchMap.get(swtId).neighbors.keySet().isEmpty())
				return null;
			else {
				out = "";
				for (String s : switchMap.get(swtId).neighbors.keySet())
					out += ("," + s);
				out = out.substring(1);
			}
		}

		String v6Addr = null;
		if (topicType.equals("sys")) {
			v6Addr = sysTopicAddrMap.get(topic);
		} else if (topicType.equals("notify")) {
			v6Addr = notifyTopicAddrMap.get(topic);
		}

		flowcount++;

		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.in = in;
		flow.out = out;
		flow.topic = topic;
		flow.table_id = t_id;
		flow.flow_id = flowcount;
		flow.priority = pri;
		flow.ipv6_dst = v6Addr + "/" + "128";
		//生成后，将其添加到notifyFlows里，以备后面调用查看
		topicFlowSet.add(flow);
		notifyFlows.put(topic, topicFlowSet);

		return flow;
	}

	public Flow generateAllOutFlow(String swtId, String in, String topic, String topicType, String t_id, String pri) {
		System.out.println("生成AllOut流表中，参数为：swtId=" + swtId + "；in=" + in + "；out=全部outPort；topic=" + topic);
		Set<Flow> topicFlowSet;
		//将route中的每一段flow都添加到set中，保证后面不用重复下发，控制flowcount
		if (notifyFlows.get(topic) != null) {
			topicFlowSet = notifyFlows.get(topic);
			for (Flow flow : topicFlowSet) {
				if (flow.swtId.equals(swtId)
						&& (flow.in == null || flow.in.equals(in))
						&& flow.topic.equals(topic)) {
					return flow;
				}
			}
		} else {
			topicFlowSet = new HashSet<>();
		}
		String out = "";
		if (outSwitches.get(swtId) != null) {
			Switch sw = outSwitches.get(swtId);
			for (String port : sw.portSet)
				out += "," + port;
		}
		out = out.substring(1);

		String v6Addr = null;
		if (topicType.equals("sys")) {
			v6Addr = sysTopicAddrMap.get(topic);
		} else if (topicType.equals("notify")) {
			v6Addr = notifyTopicAddrMap.get(topic);
		}

		flowcount++;

		Flow flow = new Flow();
		flow.in = in;
		flow.swtId = swtId;
		flow.out = out;
		flow.table_id = t_id;
		flow.priority = pri;
		flow.ipv6_dst = v6Addr + "/128";
		flow.topic = topic;
		flow.flow_id = flowcount;
		topicFlowSet.add(flow);
		notifyFlows.put(topic, topicFlowSet);
		return flow;
	}

	public Flow generateNoInPortFlow(String swtId, String out, String topic, String topicType, String t_id, String pri) {
		//Sys默认50，NoInPort默认20，InPort默认10，越高越早匹配
		// out有一种是flood-in-grp，就是选择这个swt中所有非outPort作为out
		System.out.println("生成NoInPort流表中，参数为：swtId=" + swtId + "；没有in端口；out=" + out + "；topic=" + topic);
		String v6Addr = null;
		if (topicType.equals("sys")) {
			v6Addr = sysTopicAddrMap.get(topic);
		} else if (topicType.equals("notify")) {
			v6Addr = notifyTopicAddrMap.get(topic);
		}

		if (out.equals("flood-in-grp")) {
			if (switchMap.get(swtId).neighbors.keySet().isEmpty())
				return null;
			else {
				out = "";
				for (String s : switchMap.get(swtId).neighbors.keySet())
					out += ("," + s);
				out = out.substring(1);
			}
		}

		flowcount++;

		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.out = out;
		flow.topic = topic;
		flow.table_id = t_id;
		flow.flow_id = flowcount;
		flow.priority = pri;
		flow.ipv6_dst = v6Addr + "/" + "128";
		return flow;
	}

	//生成向groupCtl发送REST请求的专用流表
	public Flow generateRestFlow(String swtId, String out, String t_id, String pri, String v4Addr) {
		System.out.println("生成Rest流表中，参数为：swtId=" + swtId + "；IPv4=" + v4Addr + "；out=" + out);
		flowcount++;

		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.out = out;
		flow.table_id = t_id;
		flow.flow_id = flowcount;
		flow.priority = pri;
		if (v4Addr.startsWith("src")) {
			flow.nw_src = v4Addr.split(":")[1];
		}
		if (v4Addr.startsWith("dst")) {
			flow.nw_dst = v4Addr.split(":")[1];
		}
		return flow;
	}

	public Flow generateLeadTabFlow(String swtId, String in, String t_id, String pri, String gotoTable) {
		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.table_id = t_id;
		flow.in = in;
		flow.priority = pri;
		flow.out = gotoTable;
		return flow;
	}
}
