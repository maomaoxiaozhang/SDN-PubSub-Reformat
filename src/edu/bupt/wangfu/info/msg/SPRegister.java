package edu.bupt.wangfu.info.msg;

import edu.bupt.wangfu.module.subpub.Action;

import java.io.Serializable;

public class SPRegister implements Serializable {
	private static final long serialVersionUID = 1L;

	public Action type;
	public String topic;
	public boolean success;
}
