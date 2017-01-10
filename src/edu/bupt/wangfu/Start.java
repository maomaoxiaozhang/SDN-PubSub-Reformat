package edu.bupt.wangfu;

import edu.bupt.wangfu.module.base.Config;
import edu.bupt.wangfu.module.base.WsnMgr;
import edu.bupt.wangfu.module.topology.GroupUtil;

/**
 *  Created by LCW on 2016-6-19.
 */
public class Start {
	public static void main(String[] args) {
		Config.configure();//这里进行配置，配置文件的内容写到SysInfo里
		GroupUtil.initGroup();//初始化本地关于集群的hostMap，switchMap还有outPorts

		new Thread(new mgrInstance()).start();
	}

	private static class mgrInstance implements Runnable {
		public void run() {
			WsnMgr.getInstance();
		}
	}
}
