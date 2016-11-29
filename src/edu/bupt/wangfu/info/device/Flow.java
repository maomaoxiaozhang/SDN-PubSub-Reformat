package edu.bupt.wangfu.info.device;

import java.io.Serializable;

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
	public String dl_dst; // ipv6 目的地址

	public String toStringOutput() {
		return String.format("table=%d priority=%s in_port=%s dl_dst=%s action=output:%s", table_id, priority, in, dl_dst, out);
	}

	public String toStringEnQueue() {
		return String.format("table=%d priority=%s in_port=%s dl_dst=%s action=enqueue:%s", table_id, priority, in, dl_dst, out);
	}

	public String toStringDelete() {
		return String.format("table=%d in_port=%s dl_dst=%s action=output:%s", table_id, in, dl_dst, out);
	}
}
