package edu.bupt.wangfu.mgr.topology;

import edu.bupt.wangfu.info.device.Flow;
import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.device.Switch;
import edu.bupt.wangfu.info.msg.Hello;
import edu.bupt.wangfu.mgr.base.SysInfo;
import edu.bupt.wangfu.mgr.route.RouteUtil;
import edu.bupt.wangfu.mgr.topology.rcver.HelloReceiver;
import edu.bupt.wangfu.mgr.topology.rcver.ReHelloReceiver;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lenovo on 2016-6-22.
 */
//只有localCtl == groupCtl时，才启动这个
public class HeartMgr extends SysInfo {
	private static Timer helloTimer = new Timer();

	public HeartMgr() {
		addSelf2Allgrps();
		downRcvHelloRehelloFlow();

		new Thread(new HelloReceiver()).start();
		new Thread(new ReHelloReceiver()).start();

		Properties props = new Properties();
		String propertiesPath = "./resources/DtConfig.properties";
		try {
			props.load(new FileInputStream(propertiesPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		reHelloPeriod = Long.parseLong(props.getProperty("reHelloPeriod"));//判断失效阀值
		helloPeriod = Long.parseLong(props.getProperty("helloPeriod"));//发送周期
		helloTaskPeriod = Long.parseLong(props.getProperty("helloTaskPeriod"));//hello任务的执行周期
		nbrGrpExpiration = Long.parseLong(props.getProperty("nbrGrpExpiration"));//邻居集群丢失时间的判断阈值

		System.out.println("starting hello task");
		helloTimer.schedule(new HelloTask(), 0, helloTaskPeriod);
	}

	private void addSelf2Allgrps() {
		Group g = new Group(localGroupName);
		g.updateTime = System.currentTimeMillis();
		g.subMap = groupSubMap;
		g.pubMap = groupPubMap;
		allGroups.put(localGroupName, g);
	}

	private void downRcvHelloRehelloFlow() {
		for (Switch swt : outSwitches.values()) {
			for (String out : swt.portSet) {
				if (!out.equals("LOCAL")) {
					//这条路径保证outPort进来hello消息可以传到groupCtl
					List<String> inHello = RouteUtil.calRoute(swt.id, localSwtId);
					//这里流表的out设置为portWsn2Swt，是因为只有在groupCtl == localCtl时才调用这个函数
					RouteUtil.downInGrpRtFlows(inHello, out, portWsn2Swt, "hello", "sys", groupCtl);
				}
			}
		}
		System.out.println("down heart flows complete");
	}

	//依次向每个outPort发送Hello信息
	private class HelloTask extends TimerTask {
		@Override
		public void run() {
			for (Switch swt : outSwitches.values()) {
				for (String out : swt.portSet) {
					if (!out.equals("LOCAL")) {
						Group localGrp = allGroups.get(localGroupName);
						localGrp.updateTime = System.currentTimeMillis();
						allGroups.put(localGroupName, localGrp);

						List<String> outHello = RouteUtil.calRoute(localSwtId, swt.id);
						List<String> inRehello = RouteUtil.calRoute(swt.id, localSwtId);
						List<Flow> ctl2out = RouteUtil.downInGrpRtFlows(outHello, portWsn2Swt, out, "hello", "sys", groupCtl);
						List<Flow> out2ctl = RouteUtil.downInGrpRtFlows(inRehello, out, portWsn2Swt, "re_hello", "sys", groupCtl);

						sendHello(out, swt.id);
						System.out.println("sending hello to switch " + swt.id + " through port " + out);
						//发送后阻塞线程，这期间：对面收到hello，回复re_hello，最后再发送一条最终版的hello
						//这之后（无论之前是否回复），都继续发下一条
						try {
							Thread.sleep(helloPeriod);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						//删除这次握手的流表，准备下次的
						RouteUtil.delRouteFlows(ctl2out);
						RouteUtil.delRouteFlows(out2ctl);
						System.out.println("delete route to switch " + swt.id + " through port " + out);
					}
				}
			}
			//定时检测邻居集群的代表是否还在线
			for (Group g : allGroups.values()) {
				if (System.currentTimeMillis() - g.updateTime > nbrGrpExpiration) {
					allGroups.remove(g.groupName);
					nbrGrpLinks.remove(g.groupName);
					System.out.println("remove group " + g.groupName + " from local allGroups");
				}
			}
		}

		private void sendHello(String out, String swtId) {
			Hello hello = new Hello();
			MultiHandler handler = new MultiHandler(uPort, "hello", "sys");

			hello.startGroup = localGroupName;
			hello.startOutPort = out;
			hello.startBorderSwtId = swtId;
			hello.reHelloPeriod = reHelloPeriod;
			hello.allGroups = allGroups;

			handler.v6Send(hello);
		}
	}

}
