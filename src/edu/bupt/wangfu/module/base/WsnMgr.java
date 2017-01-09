package edu.bupt.wangfu.module.base;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.module.route.RouteSyncMsgReceiver;
import edu.bupt.wangfu.module.subpub.SubPubMgr;
import edu.bupt.wangfu.module.topology.HeartMgr;
import edu.bupt.wangfu.module.topology.rcver.LSAReceiver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lenovo on 2016-6-22.
 */
public class WsnMgr extends SysInfo {
	private static WsnMgr wsnMgr;
	private HeartMgr heartMgr;//集群内检测模块
	private SubPubMgr subPubMgr;//订阅与发布管模块

	private WsnMgr() {
		while (true) {
			if (switchMap.size() == 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				break;
			}
		}
		System.out.println("WsnMgr启动，本地地址" + localAddr + "，集群控制器" + groupCtl.url);
		if (localCtl.url.equals(groupCtl.url)) {
			heartMgr = new HeartMgr();
		}
		subPubMgr = new SubPubMgr();
		new Thread(new LSAReceiver()).start();
		new Thread(new RouteSyncMsgReceiver()).start();
	}

	public static WsnMgr getInstance() {
		if (wsnMgr == null) {
			wsnMgr = new WsnMgr();
		}
		return wsnMgr;
	}

	public static Map<String, Group> cloneGrpMap(Map<String, Group> allGrps) {
		Map<String, Group> res = new ConcurrentHashMap<>();
		for (String key : allGrps.keySet()) {
			res.put(key, allGrps.get(key));
		}
		return res;
	}

	public static Map<String, Set<String>> cloneSetMap(Map<String, Set<String>> map) {
		Map<String, Set<String>> res = new ConcurrentHashMap<>();
		for (String key : map.keySet()) {
			Set<String> set = new HashSet<>();
			for (String inSet : map.get(key)) {
				set.add(inSet);
			}
			res.put(key, set);
		}
		return res;
	}

	public static List<String> cloneStrList(List<String> route) {
		List<String> res = new ArrayList<>();
		for (int i = 0; i < route.size(); i++) {
			res.add(i, route.get(i));
		}
		return res;
	}

	public static Map<String, Integer> cloneIntMap(Map<String, Integer> dist2NbrGrps) {
		Map<String, Integer> res = new ConcurrentHashMap<>();
		for (String key : dist2NbrGrps.keySet()) {
			res.put(key, dist2NbrGrps.get(key));
		}
		return res;
	}
}
