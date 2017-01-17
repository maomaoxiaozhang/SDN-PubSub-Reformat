package edu.bupt.wangfu.subscribe;

import javax.jws.WebService;

/**
 * Created by HanB on 2017/1/10.
 */
@WebService
public class SubscribeProcess {
	private static int count = 0;
	private static long first = 0;
	public void subscribeProcess(String content) {
		count++;

		if (count == 1 || count % 50 == 0) {
			if (count == 1)
				first = System.currentTimeMillis();
			System.out.println("收到订阅消息:" + content + " -- " + count + "条," + (System.currentTimeMillis()-first));
		}
	}
}
