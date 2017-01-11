package edu.bupt.wangfu.module.subpub.ws;

import edu.bupt.wangfu.info.msg.NotifyObj;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.subpub.SubPubMgr;
import edu.bupt.wangfu.opendaylight.MultiHandler;

/**
 * @ Created by HanB on 2016/11/29.
 */
public class WsnProcessImpl extends SysInfo {
	public String wsnProcess(String msg) {
		String[] msgSplit = msg.split("#");
		if (msgSplit.length == 3) {
			String action = msgSplit[0];
			String topic = msgSplit[1];
			String content = msgSplit[2];

			if (action.equals("SUB"))
				System.out.println("收到新订阅，主题为" + "，接受消息的服务地址为：" + content);
			else
				System.out.println("收到新消息，动作为" + action + "，主题为" + "，内容为" + content);

			switch (action) {
				case "SUB":
					return topic + "#" + (SubPubMgr.localSubscribe(topic.toLowerCase(), false, content) ? "success" : "fail");
				case "PUB":
					return topic + "#" + (SubPubMgr.localPublish(topic.toLowerCase()) ? "success" : "fail");
				case "UNSUB":
					return topic + "#" + (SubPubMgr.localUnsubscribe(topic.toLowerCase()) ? "success" : "fail");
				case "UNPUB":
					return topic + "#" + (SubPubMgr.localUnpublish(topic.toLowerCase()) ? "success" : "fail");
				case "NOTIFY":
					//TODO 想加切包、安全加密？都在这里！
					NotifyObj no = new NotifyObj(topic.toLowerCase(), content);
					MultiHandler handler = new MultiHandler(notifyPort, topic.toLowerCase(), "notify");
					handler.v6Send(no);
					return topic + "#" + "success";
				default:
					return topic + "#" + "fail";
			}
		} else
			return msg + " fail";
	}
}
