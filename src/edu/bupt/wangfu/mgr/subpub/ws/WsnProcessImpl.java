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
					return topic + "#" + (SubPubMgr.localSubscribe(topic) ? "success" : "fail");
				case "PUB":
					return topic + "#" + (SubPubMgr.localSubscribe(topic) ? "success" : "fail");
				case "UNSUB":
					return topic + "#" + (SubPubMgr.localSubscribe(topic) ? "success" : "fail");
				case "UNPUB":
					return topic + "#" + (SubPubMgr.localSubscribe(topic) ? "success" : "fail");
				case "NOTIFY":
					//TODO 想加切包、安全加密？都在这里！
					NotifyObj no = new NotifyObj(topic, content);
					MultiHandler handler = new MultiHandler(notifyPort, topic, "notify");
					handler.v6Send(no);
					return "success";
				default:
					return topic + "#" + "fail";
			}
		} else
			return msg + " fail";
	}
}
