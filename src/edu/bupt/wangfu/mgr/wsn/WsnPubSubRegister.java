package edu.bupt.wangfu.mgr.wsn;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

@WebService
/**
 * @ Created by HanB on 2016/11/29.
 */
public class WsnPubSubRegister implements Runnable {

	public static void main(String[] args) {
		Endpoint.publish("http://localhost:1111/WsnRegisterService", new WsnPubSubRegister());
	}

	// 发布的服务方法处理发布者订阅者的注册，并返回处理结果
	public String wsnServerMethod(String msg) {
		WsnRegisterImplement wsnRegisterImplement = new WsnRegisterImplement();
		return wsnRegisterImplement.processPubSubRegister(msg);
	}

	@Override
	@WebMethod(exclude = true)
	public void run() {
		Endpoint.publish("http://localhost:1111/WsnRegisterService", new WsnPubSubRegister());
	}
}
