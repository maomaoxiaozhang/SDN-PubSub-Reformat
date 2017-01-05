package edu.bupt.wangfu.test;

import edu.bupt.wangfu.info.msg.Hello;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.concurrent.ConcurrentHashMap;

import static edu.bupt.wangfu.opendaylight.WsnUtil.initSysTopicMap;

/**
 * @ Created by HanB on 2016/12/26.
 */
public class HelloPrint extends SysInfo {
	public static void main(String[] args) {
		sysTopicAddrMap = new ConcurrentHashMap<>();
		initSysTopicMap();

		MultiHandler multiHandler = new MultiHandler(30000, "re_hello", "sys");
		Object object = multiHandler.v6Receive();
		Hello hello = (Hello) object;
		System.out.println("StartGroup:" + hello.startGroup + ", startBorderSwtId:" + hello.startBorderSwtId);
		System.out.println("EndGroup:" + hello.endGroup + ", endBorderSwtId:" + hello.endBorderSwtId);
	}
}
