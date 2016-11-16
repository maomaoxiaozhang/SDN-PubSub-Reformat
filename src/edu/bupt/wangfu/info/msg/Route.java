package edu.bupt.wangfu.info.msg;

import java.io.Serializable;
import java.util.List;

public class Route implements Serializable {
	private static final long serialVersionUID = 1L;

	public String group;
	public String startSwtId;
	public String endSwtId;
	public List<String> route;//途经的swt的ID
}