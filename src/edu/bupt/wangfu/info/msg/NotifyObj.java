package edu.bupt.wangfu.info.msg;

import java.io.Serializable;

/**
 * Created by lenovo on 2016-10-26.
 */
public class NotifyObj implements Serializable {
	private static final long serialVersionUID = 1L;

	public String topic;
	public String content;

	public NotifyObj() {
	}

	public NotifyObj(String topic, String content) {
		this.topic = topic;
		this.content = content;
	}
}
