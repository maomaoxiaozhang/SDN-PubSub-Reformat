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
	private static int flowcount;

	private FlowUtil() {
		this.flowcount = 0;
	}

	public static synchronized FlowUtil getInstance() {
		if (ins == null)
			ins = new FlowUtil();
		return ins;
	}

	/*public static String topicName2multiV6Addr(String topicName, List<List<String>> topicList, int queueNo) {
		String tc = topicName2topicCode(topicName, topicList);
		return topicCode2multiV6Addr(tc, queueNo);
	}

	public static String topicCode2multiV6Addr(String topicCode, int queueNo) {
		//topicCode length, from 10 to 2, 7 bits
		String len = Integer.toBinaryString(topicCode.length());
		int l = len.length();
		for (int k = 0; k < 7 - l; k++) {
			len = "0" + len;
		}

		//queue number, from 10 to 2, 3 bits
		String qn = Integer.toBinaryString(queueNo);
		l = qn.length();
		for (int k = 0; k < 3 - l; k++) {
			qn = "0" + qn;
		}

		//ldap code, complete to 100 bits
		l = topicCode.length();
		for (int k = 0; k < 100 - l; k++) {
			topicCode = topicCode + "0";
		}

		return "11111111" + "0000" + "1110" + "10" + len + qn + topicCode;
	}

	public static String topicName2topicCode(String topicName, List<List<String>> topicList) {
		//topicList中每一个list代表（像是all:a:d这样）一截主题8bit，100bit能容纳12层
		//e.g. topicList == [[all],[a,b,c],[d,e,f]]
		//所以all:a:d == 0:0:0

		String[] names = topicName.split(":");
		StringBuilder binIndex = new StringBuilder();

		for (int i = 0; i < names.length; i++) {
			List<String> levelList = topicList.get(i);
			String cur = names[i];
			for (int j = 0; j < levelList.size(); j++) {
				if (cur.equals(levelList.get(j))) {
					String t = Integer.toBinaryString(j);
					int l = t.length();
					for (int k = 0; k < 8 - l; k++) {
						t = "0" + t;
					}
					binIndex.append(t);
				}
			}
		}

		return binIndex.toString();
	}
*/
	public static boolean downFlows(Controller controller, List<Flow> flows, List<String> actions) {
		boolean success = false;
		for (Flow flow : flows) {
			if (downFlow(controller, flow, actions.get(flows.indexOf(flow))))
				success = true;
		}
		return success;
	}

	public static boolean deleteFlow(Controller controller, String table_id, String flow_id) {
		String url = controller.url + "/restconf/config/opendaylight-inventory:nodes/node/openflow:" + localSwtId
				+ "/table/" + table_id + "/flow/" + flow_id;
		return RestProcess.doClientDelete(url).equals("200");
	}

	public static boolean deleteFlow(Controller controller, Flow flow) {
		String url = controller.url + "/restconf/config/opendaylight-inventory:nodes/node/openflow:" + localSwtId
				+ "/table/" + flow.table_id + "/flow/" + flow.flow_id;
		return RestProcess.doClientDelete(url).equals("200");
	}

	public static boolean downFlow(Controller controller, Flow flow, String action) {
		//TODO 这里还要考虑下发到具体哪个流表里，看要执行的动作是 更新流表项 还是 添加新流表项
		// action == "Add" "update"
		return RestProcess.doClientPost(controller.url, flow.jsonContent).get(0).equals("200");
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
		String v6Addr;
		if (topicType.equals("sys")) {
			v6Addr = sysTopicAddrMap.get(topic);
		} else if (topicType.equals("notify")) {
			v6Addr = notifyTopicAddrMap.get(topic);
		}

		flowcount++;
		String table_id = String.valueOf(t_id);
		String priority = String.valueOf(pri);//TODO 优先级是数字越大越靠前吗？

		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.in = in;
		flow.out = out;
		flow.topic = topic;
		flow.table_id = t_id;
		flow.flow_id = flowcount;
		flow.priority = pri;
		//生成后，将其添加到notifyFlows里，以备后面调用查看
		topicFlowSet.add(flow);
		notifyFlows.put(topic, topicFlowSet);

		return flow;
	}

	public Flow generateFlow(String swtId, String out, String topic, String topicType, int t_id, int pri) {
		//TODO out有一种是flood-in-grp，就是选择这个swt中所有非outPort作为out
		String v6Addr;
		if (topicType.equals("sys")) {
			v6Addr = sysTopicAddrMap.get(topic);
		} else if (topicType.equals("notify")) {
			v6Addr = notifyTopicAddrMap.get(topic);
		}

		flowcount++;
		String table_id = String.valueOf(t_id);
		String priority = String.valueOf(pri);//TODO 优先级是数字越大越靠前吗？

		Flow flow = new Flow();
		flow.swtId = swtId;
		flow.out = out;
		flow.topic = topic;
		flow.table_id = t_id;
		flow.flow_id = flowcount;
		flow.priority = pri;

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

		return flow;
	}

}
