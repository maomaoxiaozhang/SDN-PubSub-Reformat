package edu.bupt.wangfu.info.device;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lenovo on 2016-10-31.
 */
public class Group extends DevInfo {
	/**
	 * Link State Advertisement，全称链路状态广播
	 * 包含这个集群的全部信息，直接存起来就可以，不是增量的
	 */
	public long updateTime;
	public String groupName;
	public Map<String, Integer> dist2NbrGrps;//实现了neighbor的功能，不需要用DevInfo里面的neighbors了。key是邻居groupName，value是二者相连的链路的距离
	//还有subMap和pubMap也要用

	public Group(String groupName) {
		super();
		this.dist2NbrGrps = new ConcurrentHashMap<>();
		this.groupName = groupName;
	}

	@Override
	public String toString() {
		return "Group{" +
				"updateTime=" + updateTime +
				", groupName='" + groupName + '\'' +
				'}';
	}
}