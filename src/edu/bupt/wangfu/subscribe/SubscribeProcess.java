package edu.bupt.wangfu.subscribe;

import javax.jws.WebService;

/**
 *  Created by HanB on 2017/1/10.
 */
@WebService
public class SubscribeProcess {
    private static int count;
    public void subscribeProcess (String content) {
        count ++;
        System.out.println("收到订阅消息:" + content + " -- " + count + "条！");
    }
}
