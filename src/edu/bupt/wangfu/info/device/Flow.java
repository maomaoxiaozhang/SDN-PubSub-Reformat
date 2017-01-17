package edu.bupt.wangfu.info.device;

import java.io.Serializable;

public class Flow implements Serializable {
	private static final long serialVersionUID = 1L;

	public int flow_id;
	public String table_id;
	public String priority;

	public String topic;
	public String swtId;
	public String in;
	public String out;
	public String nw_src; // ipv4 源ip
	public String nw_dst; // ipv4 目的ip
	public String ipv6_dst; // ipv6 目的地址

	public String toStringOutput() {
		if (in != null) { // generateFlow
			if (ipv6_dst != null)
				return String.format("table=%s,priority=%s,dl_type=%s,in_port=%s,ipv6_dst=%s,action=output:%s", table_id, priority, "0x86DD", in, ipv6_dst, out);
			else
				return String.format("table=%s,priority=%s,dl_type=%s,in_port=%s,action=goto_table:%s", table_id, priority, "0x0800", in, out);
		}
		if (ipv6_dst != null) { // generateNoInPortFlow
			return String.format("table=%s,priority=%s,dl_type=%s,ipv6_dst=%s,action=output:%s", table_id, priority, "0x86DD", ipv6_dst, out);
		}
		if (nw_src != null) { // generateRestFlow
			return String.format("table=%s,priority=%s,dl_type=%s,nw_src=%s,action=output:%s", table_id, priority, "0x0800", nw_src, out);
		}
		if (nw_dst != null) {
			return String.format("table=%s,priority=%s,dl_type=%s,nw_dst=%s,action=output:%s", table_id, priority, "0x0800", nw_dst, out);
		}
		return null;
	}

	public String toString() {
		return "topic: " + topic + ", swtId: " + swtId + ", out port: " + out;
	}

	public String toStringEnQueue() {
		return String.format("table=%s,priority=%s,dl_type=%s,in_port=%s,ipv6_dst=%s,action=enqueue:%s", table_id, priority, "0x86DD", in, ipv6_dst, out);
	}

	public String toStringDelete() {
		if (in == null)
			return String.format("table=%s,dl_type=%s,ipv6_dst=%s", table_id, "0x86DD", ipv6_dst);
		return String.format("table=%s,dl_type=%s,in_port=%s,ipv6_dst=%s", table_id, "0x86DD", in, ipv6_dst);
	}
}
