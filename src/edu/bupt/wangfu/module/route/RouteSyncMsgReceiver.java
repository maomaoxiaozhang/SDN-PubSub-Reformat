package edu.bupt.wangfu.module.route;

import edu.bupt.wangfu.info.msg.Route;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.opendaylight.MultiHandler;

/**
 * Created by lenovo on 2016-10-27.
 */
public class RouteSyncMsgReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public RouteSyncMsgReceiver() {
		System.out.println("集群内路径同步（RouteSync）监听线程启动");
		handler = new MultiHandler(sysPort, "route", "sys");
	}

	@Override
	public void run() {
		while (true) {
			Object msg = handler.v6Receive();
			Route r = (Route) msg;
			new Thread(new SyncMsgHandler(r)).start();
		}
	}

	private class SyncMsgHandler implements Runnable {
		private Route route;

		SyncMsgHandler(Route r) {
			this.route = r;
		}

		@Override
		public void run() {
			groupRoutes.add(route);//在初始化groupRoutes时，已经确保过线程安全了
		}
	}
}
