package edu.bupt.wangfu.subscribe;


import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @ Created by HanB on 2016/12/7.
 */
public class SingleSubscribe {
	//    public static String localAddr = "localhost";
	public static String localPort = "30000";

	public static void main(String[] args) {

		URL wsdlUrl = null;
		try {
			wsdlUrl = new URL("http://" + args[0] + ":" + localPort + "/WsnRegisterService?wsdl");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		Service s = Service.create(wsdlUrl, new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterService"));
		WsnSPRegister hs = s.getPort(new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterPort"), WsnSPRegister.class);
		String ret = hs.wsnServerMethod("SUB#all:a#123");
		System.out.println(ret);
	}
}
