package edu.bupt.wangfu.info.device;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Created by root on 15-7-14.
 */
public class Switch extends DevInfo {
	public Set<String> portSet;//经过initGroup()，剩下的端口就是outPorts；在普通节点里，portSet存的是所有激活的端口
	public String id;
	public double load;
	public Map<Integer, List<Queue>> queues;//一个端口有多个队列

	public Switch(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Switch{ " +
				"outPort（以及Local端口）有：" + portSet +
				"，id：'" + id + '\'' +
				"，load：" + load +
				"，邻居个数：" + neighbors.size() +
				" }";
	}
}