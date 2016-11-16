package edu.bupt.wangfu.mgr.base;

import edu.bupt.wangfu.mgr.route.RouteSyncMsgReceiver;
import edu.bupt.wangfu.mgr.subpub.SubPubMgr;
import edu.bupt.wangfu.mgr.topology.HeartMgr;
import edu.bupt.wangfu.mgr.topology.rcver.LSAReceiver;

/**
 * Created by lenovo on 2016-6-22.
 */
public class WsnMgr extends SysInfo {
	private static WsnMgr wsnMgr = new WsnMgr();
	private HeartMgr dt;//集群内检测模块
	private SubPubMgr spMgr;//订阅与发布管模块

	private WsnMgr() {
		if (groupCtl.equals(localCtl)) {
			dt = new HeartMgr();
		}
		new Thread(new LSAReceiver()).start();
		new Thread(new RouteSyncMsgReceiver()).start();
		spMgr = new SubPubMgr();
	}

	public static WsnMgr getInstance() {
		return wsnMgr;
	}
}
