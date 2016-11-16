package edu.bupt.wangfu.info.device;

import java.io.Serializable;

/**
 * Created by root on 15-10-5.
 */
public class Controller implements Serializable {
	private static final long serialVersionUID = 1L;

	public String url;

	public Controller(String controllerAddr) {
		this.url = controllerAddr;
		if (!controllerAddr.startsWith("http://"))
			this.url = "http://" + controllerAddr;
	}
}
