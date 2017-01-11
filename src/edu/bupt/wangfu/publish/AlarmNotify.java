package edu.bupt.wangfu.publish;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *  Created by HanB on 2017/1/10.
 */
public class AlarmNotify {
    private static int count = 1000;
    public static void main(String[] args) {
        URL wsdlUrl = null;
        try {
            wsdlUrl = new URL("http://" + args[0] + "/WsnRegisterService?wsdl");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Service s = Service.create(wsdlUrl, new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterService"));
        WsnSPRegister hs = s.getPort(new QName("http://ws.subpub.module.wangfu.bupt.edu/", "WsnSPRegisterPort"), WsnSPRegister.class);

        String topic = "all:a"; // 发布的主题
        String content = "I'm a bupter."; // 发布的内容

        // 发布注册成功，开始发布消息
        if (hs.wsnServerMethod("PUB#" + topic + content).contains("success")) {
            // 发布消息数
            for (int i = 0; i < count; i ++) {
                hs.wsnServerMethod("NOTIFY#" + topic + "#" + content);
                if (i % 100 == 0)
                    System.out.println("PUB " + topic + " -- " + i);
            }
        } else {
            System.out.println("PUB " + topic + "failed!");
        }
    }
}
