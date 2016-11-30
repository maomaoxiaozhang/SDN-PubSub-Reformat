package edu.bupt.wangfu.info.device;

import java.io.*;

public class Flow implements Serializable {
	private static final long serialVersionUID = 1L;

	public int flow_id;
	public int table_id;
	public int priority;

	public String topic;
	public String swtId;
	public String in;
	public String out;
	public String nw_src; // ipv4 源ip
	public String nw_dst; // ipv4 目的ip
	public String ipv6_dst; // ipv6 目的地址

	public String toStringOutput() {
		if (in != null) { // generateFlow
			return String.format("table=%d priority=%s in_port=%s ipv6_dst=%s action=output:%s", table_id, priority, in, ipv6_dst, out);
		}if (ipv6_dst != null) { // generateNoInPortFlow
			return String.format("table=%d priority=%s ipv6_dst=%s action=output:%s", table_id, priority, ipv6_dst, out);
		}if (nw_src != null) { // generateRestFlow
			return String.format("table=%d priority=%s nw_src=%s action=output:%s", table_id, priority, nw_src, out);
		}if (nw_dst != null) {
			return String.format("table=%d priority=%s nw_dst=%s action=output:%s", table_id, priority, nw_dst, out);
		}
		return null;
	}

	public String toStringEnQueue() {
		return String.format("table=%d priority=%s in_port=%s ipv6_dst=%s action=enqueue:%s", table_id, priority, in, ipv6_dst, out);
	}

	public String toStringDelete() {
		return String.format("table=%d in_port=%s ipv6_dst=%s action=output:%s",table_id, in, ipv6_dst,out);
	}
}
