package edu.bupt.wangfu.subscribe;


import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @ Created by HanB on 2016/12/7.
 */
public class SingleSubscribe {

	public static void main(String[] args) {
		URL wsdlUrl = null;
		try {
			wsdlUrl = new URL("http://" + args[0] +  "/WsnRegisterService?wsdl");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		// 调用节点webservice
		Service s = Service.create(wsdlUrl, new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterService"));
		WsnSPRegister hs = s.getPort(new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterPort"), WsnSPRegister.class);

		// 启动订阅接受服务
		String subscribeProcessAddr = "http://" + args[1] + "/SubscribeProcess";
		Endpoint.publish(subscribeProcessAddr,new SubscribeProcess());

		String topic = "all:a"; // 订阅主题
		String ret = hs.wsnServerMethod("SUB#" + topic + "#" + subscribeProcessAddr);
		if (ret.contains("success"))
			System.out.println("SUB " + topic + "success!");
		else
			System.out.println("SUB " + topic + "failed!");
	}
}
