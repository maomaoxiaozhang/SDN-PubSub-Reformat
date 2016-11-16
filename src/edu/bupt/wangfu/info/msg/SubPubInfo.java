package edu.bupt.wangfu.info.msg;

import edu.bupt.wangfu.mgr.subpub.Action;

import java.io.Serializable;

/**
 * Created by lenovo on 2016-10-26.
 */
public class SubPubInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	public Action action;
	public String group;
	public String topic;
	public String hostMac;
	public String hostIP;
	public String port;
	public String swtId;
}
