package edu.bupt.wangfu.module.topology.rcver;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.opendaylight.MultiHandler;

/**
 *  Created by lenovo on 2016-6-23.
 */
public class HeartBeatReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public HeartBeatReceiver() {
		System.out.println("HeartBeat监听线程启动");
		handler = new MultiHandler(sysPort, "heart", "sys");
	}

	@Override
	public void run() {
		while (true) {
			Object msg = handler.v6Receive();
			Group heart = (Group) msg;
			onHeartBeat(heart);
		}
	}

	private void onHeartBeat(Group heart) {

	}
}