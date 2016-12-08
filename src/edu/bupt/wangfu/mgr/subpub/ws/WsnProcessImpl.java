package edu.bupt.wangfu.mgr.subpub.ws;

import edu.bupt.wangfu.info.msg.NotifyObj;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.subpub.SubPubMgr;
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
			switch (action) {
				case "SUB":
					return topic + "#" + (SubPubMgr.localSubscribe(topic.toLowerCase(), false) ? "success" : "fail");
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
