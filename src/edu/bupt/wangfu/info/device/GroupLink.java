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

	@Override
	public String toString() {
		return "GroupLink{" +
				"本地集群名='" + srcGroupName + '\'' +
				", 对面集群名='" + dstGroupName + '\'' +
				", 本集群边界交换机ID='" + srcBorderSwtId + '\'' +
				", 本集群边界交换机端口='" + srcOutPort + '\'' +
				", 对面集群边界交换机ID='" + dstBorderSwtId + '\'' +
				", 对面集群边界交换机端口='" + dstOutPort + '\'' +
				'}';
	}
}
