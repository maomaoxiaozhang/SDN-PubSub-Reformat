package edu.bupt.wangfu.module.subpub.ws;

import edu.bupt.wangfu.module.base.SysInfo;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

@WebService
/**
 * @ Created by HanB on 2016/11/29.
 */
public class WsnSPRegister extends SysInfo implements Runnable {

	public static void main(String[] args) {
		Endpoint.publish("http://10.108.164.152:30000/WsnRegisterService", new WsnSPRegister());
	}

	// 发布的服务方法处理发布者订阅者的注册，并返回处理结果
	public String wsnServerMethod(String msg) {
		WsnProcessImpl wsnProcessImpl = new WsnProcessImpl();
		return wsnProcessImpl.wsnProcess(msg);
	}

	@Override
	@WebMethod(exclude = true)
	public void run() {
		System.out.println("WebService启动成功，地址为：http://" + localAddr + ":" + tPort + "/WsnRegisterService");
		Endpoint.publish("http://" + localAddr + ":" + tPort + "/WsnRegisterService", new WsnSPRegister());
	}
}
