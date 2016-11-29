package edu.bupt.wangfu.mgr.wsn;

import edu.bupt.wangfu.mgr.subpub.SubPubMgr;

/**
 * @ Created by HanB on 2016/11/29.
 */
public class WsnRegisterImplement {
    public String processPubSubRegister(String msg) {
        String[] msgSplit = msg.split("#");
        if (msgSplit.length == 2) {
            String cation = msgSplit[0];
            String topic = msgSplit[1];
            switch (cation) {
                case "SUB":
                    return topic + "#" + (SubPubMgr.localSubscribe(topic) ? "success" : "fail");
                case "PUB":
                    return topic + "#" + (SubPubMgr.localSubscribe(topic) ? "success" : "fail");
                case "UNSUB":
                    return topic + "#" + (SubPubMgr.localSubscribe(topic) ? "success" : "fail");
                case "UNPUB":
                    return topic + "#" + (SubPubMgr.localSubscribe(topic) ? "success" : "fail");
                default:
                    return topic + "#" + "fail";
            }
        }else
            return msg + " fail";
    }
}
