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
	//第一个参数是节点接收SUB信息的WebService地址，第二个参数是WebService的端口，第三个参数是订阅的主题
	//192.168.100.31:30000，192.168.100.31:29999
	public static void main(String[] args) {
		if (args[0] == null) {
			System.out.println("第一个参数是节点接收SUB信息的WebService地址，第二个参数是WebService的端口，第三个参数是订阅的主题");
		}
		URL wsdlUrl = null;
		try {
			wsdlUrl = new URL("http://" + args[0] + ":" + args[1] + "/WsnRegisterService?wsdl");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		// 调用节点webservice
		Service s = Service.create(wsdlUrl, new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterService"));
		WsnSPRegister hs = s.getPort(new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterPort"), WsnSPRegister.class);

		// 启动订阅接受服务
		String subscribeProcessAddr = "http://" + args[0] + ":" + (Integer.parseInt(args[1]) - 1) + "/SubscribeProcess";
		Endpoint.publish(subscribeProcessAddr, new SubscribeProcess());

//		String topic = "all:a"; // 订阅主题
		String topic = args[2]; // 订阅主题
		String ret = hs.wsnServerMethod("SUB#" + topic + "#" + subscribeProcessAddr);
		if (ret.contains("success"))
			System.out.println("SUB " + topic + " success!");
		else
			System.out.println("SUB " + topic + " failed!");
	}
}
