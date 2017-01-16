package edu.bupt.wangfu.subscribe;

import javax.jws.WebService;

/**
 * Created by HanB on 2017/1/10.
 */
@WebService
public class SubscribeProcess {
	private static int count = 0;

	public void subscribeProcess(String content) {
		count++;

		if (count == 1 || count % 100 == 0) {
			System.out.println("收到订阅消息:" + content + " -- " + count + "条！");
		}
	}
}
