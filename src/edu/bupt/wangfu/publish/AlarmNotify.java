package edu.bupt.wangfu.publish;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by HanB on 2017/1/10.
 */
public class AlarmNotify {
	private static int count = 500;

	public static void main(String[] args) {
		URL wsdlUrl = null;
		try {
			wsdlUrl = new URL("http://" + args[0] + ":" + args[1] + "/WsnRegisterService?wsdl");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		Service s = Service.create(wsdlUrl, new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterService"));
		WsnSPRegister hs = s.getPort(new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterPort"), WsnSPRegister.class);

		String topic = "all:a"; // 发布的主题
		String content = "I'm a bupter. -- (" + System.currentTimeMillis() + ")"; // 发布的内容

		// 发布注册成功，开始发布消息
		hs.wsnServerMethod("PUB#" + topic + "#" + content);
		/*if ( hs.wsnServerMethod("PUB#" + topic + "#" + content).contains("success")) {*/
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		count = Integer.parseInt(args[2]);
		// 发布消息数
		for (int i = 1; i <= count; i++) {
			hs.wsnServerMethod("NOTIFY#" + topic + "#" + content);
			if (i % 100 == 0)
				System.out.println("PUB " + topic + " -- " + i);
		}
		/*} else {
	        System.out.println("PUB " + topic + " failed!");
        }*/
	}
}
