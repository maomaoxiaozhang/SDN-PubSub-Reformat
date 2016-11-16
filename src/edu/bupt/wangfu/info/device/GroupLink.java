package edu.bupt.wangfu.info.device;

/**
 * Created by lenovo on 2016-10-31.
 */
public class GroupLink {
	public String srcGroupName;//发信人自己这边的groupName
	public String dstGroupName;//发信人对面的集群名称

	public String srcBorderSwtId;//自己这边，边界swtId
	public String srcOutPort;

	public String dstBorderSwtId;//outerGroup这边，边界swtId
	public String dstOutPort;
}
