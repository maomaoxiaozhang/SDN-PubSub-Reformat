package edu.bupt.wangfu.opendaylight;

import edu.bupt.wangfu.info.device.Controller;
import edu.bupt.wangfu.info.device.Flow;
import edu.bupt.wangfu.mgr.base.SysInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by lenovo on 2016-5-18.
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

	public static boolean downFlows(Controller controller, List<Flow> flows, List<String> actions) {
		boolean success = false;
		for (Flow flow : flows) {
			if (downFlow(controller, flow, actions.get(flows.indexOf(flow))))
				success = true;
		}
		return success;
	}

	public static boolean deleteFlow(Controller controller, Flow flow) {
		//RestProcess.doClientDelete(controller, flow.swtId, flow.toStringOutput());
		OvsProcess.deleteFlows(controller,flow.swtId,flow.toStringDelete());
		// TODO 查看流表下发成功与否
		return true;
	}

	public static boolean downFlow(Controller controller, Flow flow, String action) {
		//TODO 这里还要考虑下发到具体哪个流表里，看要执行的动作是 更新流表项 还是 添加新流表项
		// action == "Add" "update"
		//RestProcess.doClientPost(controller, flow.swtId, flow.toStringOutput());
		String dumpResult = OvsProcess.dumpFlows(controller,flow.swtId,flow.toStringDelete());
		if (dumpResult.split("\n").length < 2) {
			OvsProcess.addFlow(controller, flow.swtId, flow.toStringOutput());
		}else {
			String outPort = ",";
			for (int i = 0; i < dumpResult.split("\n").length; i ++) {
				String singleflow = dumpResult.split("\n")[i];
				singleflow = singleflow.substring(singleflow.indexOf("actions="));
				singleflow = singleflow.substring(singleflow.indexOf("=")+1);
				for (int j = 0; j < singleflow.split(",").length; j ++)
					outPort += singleflow.split(",")[j].charAt(singleflow.split(",")[j].length()-1);
			}
			outPort = outPort.substring(1);
			flow.out = outPort;
			OvsProcess.addFlow(controller, flow.swtId, flow.toStringOutput());
		}
		// TODO 查看流表下发成功与否
		return true;
	}

	//TODO 生成函数找韩波
	//这里使用单例模式是为了方便计数flowcount，每条流表的编号必须不一样
	public Flow generateFlow(String swtId, String in, String out, String topic, String topicType, int t_id, int pri) {
		//将route中的每一段flow都添加到set中，保证后面不用重复下发，控制flowcount
		Set<Flow> topicFlowSet = notifyFlows.get(topic) == null ? new HashSet<Flow>() : notifyFlows.get(topic);
		for (Flow flow : topicFlowSet) {
			if (flow.swtId.equals(swtId)
					&& flow.in.equals(in)
					&& flow.out.equals(out)
					&& flow.topic.equals(topic)) {
				return flow;
			}
		}
		//之前没生成过这条流表，需要重新生成
		String v6Addr = null;
		if (topicType.equals("sys")) {
			v6Addr = sysTopicAddrMap.get(topic);
		} else if (topicType.equals("notify")) {
			v6Addr = notifyTopicAddrMap.get(topic);
		}

		flowcount ++;

		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.in = in;
		flow.out = out;
		flow.topic = topic;
		flow.table_id = t_id;
		flow.flow_id = flowcount;
		flow.priority = pri;
		flow.dl_dst = v6Addr;
		//生成后，将其添加到notifyFlows里，以备后面调用查看
		topicFlowSet.add(flow);
		notifyFlows.put(topic, topicFlowSet);

		return flow;
	}

	public Flow generateFlow(String swtId, String out, String topic, String topicType, int t_id, int pri) {
		//TODO out有一种是flood-in-grp，就是选择这个swt中所有非outPort作为out
		String v6Addr = null;
		if (topicType.equals("sys")) {
			v6Addr = sysTopicAddrMap.get(topic);
		} else if (topicType.equals("notify")) {
			v6Addr = notifyTopicAddrMap.get(topic);
		}

		flowcount++;

		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.out = out;
		flow.topic = topic;
		flow.table_id = t_id;
		flow.flow_id = flowcount;
		flow.priority = pri;
		flow.dl_dst = v6Addr;
		return flow;
	}

	//生成向groupCtl发送REST请求的专用流表
	public Flow generateRestFlow(String swtId, String out, int t_id, int pri) {
		flowcount++;
		String table_id = String.valueOf(t_id);
		String priority = String.valueOf(pri);//TODO 优先级是数字越大越靠前吗？

		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.out = out;
		flow.table_id = t_id;
		flow.flow_id = flowcount;
		flow.priority = pri;
		//flow.dl_dst = v6Addr;
		return flow;
	}

}
