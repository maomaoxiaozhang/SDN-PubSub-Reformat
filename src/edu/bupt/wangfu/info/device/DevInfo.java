package edu.bupt.wangfu.info.device;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DevInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	public String mac;
	public Map<String, DevInfo> neighbors;//key是端口号，value是设备，不包括跨集群的邻居
	public Map<String, Set<String>> subMap;//本集群的订阅信息，key是topic，value是swtId:port的集合
	public Map<String, Set<String>> pubMap;//本集群的发布信息，key是topic，value是swtId:port的集合

	public DevInfo() {
		this.neighbors = new ConcurrentHashMap<>();
		this.subMap = new ConcurrentHashMap<>();
		this.pubMap = new ConcurrentHashMap<>();
	}

	public String getMac() {
		return mac;
	}

	public void addNeighbor(String port, DevInfo dev) {
		this.neighbors.put(port, dev);
	}
}
