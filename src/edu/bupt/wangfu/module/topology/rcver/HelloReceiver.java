package edu.bupt.wangfu.module.topology.rcver;

import edu.bupt.wangfu.info.device.Flow;
import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.device.GroupLink;
import edu.bupt.wangfu.info.device.Switch;
import edu.bupt.wangfu.info.msg.Hello;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.route.RouteUtil;
import edu.bupt.wangfu.module.topology.GroupUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.List;
import java.util.Map;

import static edu.bupt.wangfu.module.base.WsnMgr.cloneGrpMap;

/**
 * Created by lenovo on 2016-6-23.
 */
public class HelloReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public HelloReceiver() {
		System.out.println("Hello和FinalHello监听线程启动");
		handler = new MultiHandler(sysPort, "hello", "sys");
	}

	@Override
	public void run() {
		while (true) {
			Object msg = handler.v6Receive();
			Hello mh = (Hello) msg;

			try {
				onHello(mh);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void onHello(Hello mh) throws InterruptedException {
		if (!mh.startGroup.equals(localGroupName)) {
			if (mh.endGroup.equals(localGroupName)) {
				//第三次握手，携带这个跨集群连接的全部信息
				new Thread(new OnFinalHello(mh)).start();
			} else {
				//第一次握手，只携带发起方的信息，需要补完接收方的信息，也就是当前节点
				new Thread(new OnHello(mh)).start();
				System.out.println("收到Hello消息");
			}
		}
	}

	private class OnHello implements Runnable {
		Hello re_hello;

		OnHello(Hello mh) {
			mh.endGroup = localGroupName;
			this.re_hello = mh;
		}

		@Override
		public void run() {
			for (Switch swt : outSwitches.values()) {
				for (String out : swt.portSet) {
					if (!out.equals("LOCAL")) {
						re_hello.endBorderSwtId = swt.id;
						re_hello.endOutPort = out;
						re_hello.allGroups = cloneGrpMap(allGroups);

						System.out.println("收到来自" + re_hello.startGroup + "的Hello消息");

						//这条路径保证从groupCtl发出来的re_hello能到达borderSwt
						List<String> outRehello = RouteUtil.calRoute(localSwtId, swt.id);
						List<Flow> rs = RouteUtil.downInGrpRtFlows(outRehello, portWsn2Swt, out, "re_hello", "sys", groupCtl);
						System.out.println("下发从本地交换机到" + swt.id + "交换机的" + out + "端口的ReHello消息流表");

						//把re_hello发送到每一个outPort，中间的时延保证对面有足够的时间反应第一条收到的信息
						MultiHandler handler = new MultiHandler(sysPort, "re_hello", "sys");
						handler.v6Send(re_hello);
						System.out.println("通过" + swt.id + "交换机的" + out + "端口发送ReHello消息");

						try {
							Thread.sleep(re_hello.reHelloPeriod);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						//删除这条回复流表，准备下次发送
						RouteUtil.delRouteFlows(rs);
					}
				}
			}
		}
	}

	private class OnFinalHello implements Runnable {
		Hello finalHello;

		OnFinalHello(Hello mh) {
			this.finalHello = mh;
		}

		@Override
		public void run() {
			//这里存的和最早发出hello信息的那边，顺序正好相反
			GroupLink gl = new GroupLink();
			gl.srcGroupName = finalHello.endGroup;
			gl.dstGroupName = finalHello.startGroup;
			gl.srcBorderSwtId = finalHello.endBorderSwtId;
			gl.srcOutPort = finalHello.endOutPort;
			gl.dstBorderSwtId = finalHello.startBorderSwtId;
			gl.dstOutPort = finalHello.startOutPort;
			nbrGrpLinks.put(gl.dstGroupName, gl);
			System.out.println("从" + gl.srcGroupName + "集群获得了FinallHello消息，此连接中我方边界交换机为" + gl.dstBorderSwtId + "，对外端口为" + gl.dstOutPort);

			//同步LSDB，其他集群的连接情况；把对面已知的每个group的信息都替换为最新版本的
			Map<String, Group> newAllGroup = finalHello.allGroups;
			for (String grpName : newAllGroup.keySet()) {
				if ((allGroups.get(grpName) == null
						&& System.currentTimeMillis() - allGroups.get(grpName).updateTime < nbrGrpExpiration)
						|| allGroups.get(grpName).updateTime < newAllGroup.get(grpName).updateTime)
					allGroups.put(grpName, newAllGroup.get(grpName));
			}

			System.out.println("邻居建立完成,邻居情况如下:");
			for (String groupName : allGroups.keySet())
				System.out.println(groupName + allGroups.get(groupName).getMac());

			//全网广播自己的集群信息
			Group g = allGroups.get(localGroupName);
			g.updateTime = System.currentTimeMillis();
			g.dist2NbrGrps.put(finalHello.startGroup, 1);
			//全网广播自己的集群信息
			GroupUtil.spreadLocalGrp();
		}
	}
}