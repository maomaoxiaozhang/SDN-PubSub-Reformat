package edu.bupt.wangfu.info.msg;

import edu.bupt.wangfu.info.device.Group;

import java.io.Serializable;
import java.util.Map;

import static edu.bupt.wangfu.module.base.WsnMgr.cloneGrpMap;

/**
 * Created by lenovo on 2016-11-13.
 */
public class AllGrps implements Serializable {
	private static final long serialVersionUID = 1L;

	public Map<String, Group> allGrps;

	public AllGrps(Map<String, Group> allGrps) {
		this.allGrps = cloneGrpMap(allGrps);
	}
}
