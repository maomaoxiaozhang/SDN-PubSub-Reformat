package edu.bupt.wangfu.opendaylight.rcver;

import edu.bupt.wangfu.info.msg.NotifyTopics;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.Map;

import static edu.bupt.wangfu.opendaylight.WsnUtil.binStr2TopicAddr;

/**
 * Created by lenovo on 2016-6-23.
 */
public class AdminReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public AdminReceiver() {
		handler = new MultiHandler(sysPort, "admin", "sys");
	}

	@Override
	public void run() {
		NotifyTopics nt = new NotifyTopics();
		handler.v6Send(nt);
		while (true) {
			Object msg = handler.v6Receive();
			if (msg instanceof NotifyTopics) {
				nt = (NotifyTopics) msg;
				if (nt.NotifyTopicCodeMap != null) {//保证其他节点收到这条空的请求信息时不会做出任何反应
					Map<String, String> ntcMap = nt.NotifyTopicCodeMap;
					for (String key : ntcMap.keySet()) {
						String binStr = "11111111"//prefix ff 8bit
								+ "0000"//flag 0 4bit
								+ "1110"//global_scope e 4bit
								+ "01"//event_type notify 01 2bit
								+ "1111111"//topic_length 7 7bit
								+ "001"//queue_NO 1 3bit
								+ ntcMap.get(key);//topic_code 100bit

						notifyTopicAddrMap.put(key, binStr2TopicAddr(binStr));
					}
				}
			}
		}
	}
}