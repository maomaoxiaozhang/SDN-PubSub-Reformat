package edu.bupt.wangfu.test;

import edu.bupt.wangfu.subscribe.SubscribeProcess;

import javax.xml.ws.Endpoint;

/**
 * Created by HanB on 2017/1/10.
 */
public class SubTest {
	public static void main(String[] args) {
		Endpoint.publish("http://10.108.164.152:29999/SubscribeProcess", new SubscribeProcess());
	}
}
