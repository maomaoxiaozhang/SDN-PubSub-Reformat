package edu.bupt.wangfu.mgr.topology.rcver;

import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.device.GroupLink;
import edu.bupt.wangfu.info.device.Switch;
import edu.bupt.wangfu.info.msg.Hello;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.topology.GroupUtil;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.util.Map;

/**
 * Created by lenovo on 2016-6-23.
 */
public class HelloReceiver extends SysInfo implements Runnable {
	private MultiHandler handler;

	public HelloReceiver() {
		handler = new MultiHandler(uPort, "hello", "sys");
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
		if (mh.endGroup.equals(localGroupName)) {
			//第三次握手，携带这个跨集群连接的全部信息
			new Thread(new onFinalHello(mh)).start();
		} else {
			//第一次握手，只携带发起方的信息，需要补完接收方的信息，也就是当前节点
			new Thread(new ReHello(mh)).start();
		}
	}

	private class ReHello implements Runnable {
		Hello re_hello;

		ReHello(Hello mh) {
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
						re_hello.allGroups = allGroups;

						//把re_hello发送到每一个outPort，中间的时延保证对面有足够的时间反应第一条收到的信息
						MultiHandler handler = new MultiHandler(uPort, "re_hello", "sys");
						handler.v6Send(re_hello);

						try {
							Thread.sleep(re_hello.reHelloPeriod);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private class onFinalHello implements Runnable {
		Hello finalHello;

		onFinalHello(Hello mh) {
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

			//同步LSDB，其他集群的连接情况；把对面已知的每个group的信息都替换为最新版本的
			Map<String, Group> newAllGroup = finalHello.allGroups;
			for (String grpName : newAllGroup.keySet()) {
				if ((allGroups.get(grpName) == null
						&& System.currentTimeMillis() - allGroups.get(grpName).updateTime < nbrGrpExpiration)
						|| allGroups.get(grpName).updateTime < newAllGroup.get(grpName).updateTime)
					allGroups.put(grpName, newAllGroup.get(grpName));
			}
			//全网广播自己的集群信息
			Group g = allGroups.get(localGroupName);
			g.updateTime = System.currentTimeMillis();
			g.dist2NbrGrps.put(finalHello.startGroup, 1);
			allGroups.put(localGroupName, g);
			//全网广播自己的集群信息
			GroupUtil.spreadLocalGrp(g);
		}
	}
}