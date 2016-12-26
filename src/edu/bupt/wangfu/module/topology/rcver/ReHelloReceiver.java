package edu.bupt.wangfu.module.topology.rcver;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.device.GroupLink;
import edu.bupt.wangfu.info.msg.Hello;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.topology.GroupUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.Map;

import static edu.bupt.wangfu.module.base.WsnMgr.cloneGrpMap;

/**
 * Created by lenovo on 2016-6-23.
 */
public class ReHelloReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public ReHelloReceiver() {
		System.out.println("ReHello监听线程启动");
		handler = new MultiHandler(sysPort, "re_hello", "sys");
	}

	@Override
	public void run() {
		while (true) {
			Object msg = handler.v6Receive();
			Hello re_hello = (Hello) msg;
			if (!re_hello.endGroup.equals(localGroupName))
				onReHello(re_hello);
		}
	}

	private void onReHello(Hello re_hello) {
		GroupLink gl = new GroupLink();
		gl.srcGroupName = re_hello.startGroup;//自己集群的名字，因为这是对面收到hello消息后的回复
		gl.dstGroupName = re_hello.endGroup;
		gl.srcBorderSwtId = re_hello.startBorderSwtId;
		gl.srcOutPort = re_hello.startOutPort;
		gl.dstBorderSwtId = re_hello.endBorderSwtId;
		gl.dstOutPort = re_hello.endOutPort;
		nbrGrpLinks.put(gl.dstGroupName, gl);
		System.out.println("从" + gl.dstGroupName + "集群收到ReHello消息，我方边界交换机为" + gl.srcBorderSwtId + "，对外端口为" + gl.srcOutPort);

		//同步LSDB，其他集群的连接情况；把对面已知的每个group的信息都替换为最新版本的
		Map<String, Group> newAllGroup = re_hello.allGroups;
		for (String grpName : newAllGroup.keySet()) {
			if ((allGroups.get(grpName) == null //这个集群的信息对面有，而我没有
					&& System.currentTimeMillis() - allGroups.get(grpName).updateTime < nbrGrpExpiration)//同时这条集群信息尚未过期
					|| allGroups.get(grpName).id < newAllGroup.get(grpName).id)//或者这个集群的信息我和对面都有，但对面的比较新
				allGroups.put(grpName, newAllGroup.get(grpName));
		}
		//再更新自己这个集群和新邻居的距离信息
		Group g = allGroups.get(localGroupName);
		g.id += 1;
		g.updateTime = System.currentTimeMillis();
		g.dist2NbrGrps.put(re_hello.endGroup, 1);//TODO 初始化邻居集群间距离为1
		//全网广播自己的集群信息
		GroupUtil.spreadLocalGrp();

		re_hello.allGroups.clear();
		re_hello.allGroups = cloneGrpMap(allGroups);//之前发来的allGroups是对面集群的，现在给它回复过去，让它存我们这边的
		handler = new MultiHandler(sysPort, "hello", "sys");
		handler.v6Send(re_hello);//因为现在还在HeartMgr.HelloTask()长度为helloPeriod的sleep()中，因此直接发送就可以
		System.out.println("向集群" + re_hello.endGroup + "回复FinalHello消息");
	}
}