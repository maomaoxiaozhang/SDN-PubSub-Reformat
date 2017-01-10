package edu.bupt.wangfu.module.topology;

import edu.bupt.wangfu.info.device.Flow;
import edu.bupt.wangfu.info.device.Group;
import edu.bupt.wangfu.info.device.Switch;
import edu.bupt.wangfu.info.msg.Hello;
import edu.bupt.wangfu.module.base.SysInfo;
import edu.bupt.wangfu.module.route.RouteUtil;
import edu.bupt.wangfu.module.topology.rcver.HelloReceiver;
import edu.bupt.wangfu.module.topology.rcver.ReHelloReceiver;
import edu.bupt.wangfu.opendaylight.MultiHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import static edu.bupt.wangfu.module.base.WsnMgr.cloneGrpMap;
import static edu.bupt.wangfu.module.base.WsnMgr.cloneIntMap;


/**
 * Created by lenovo on 2016-6-22.
 */
//只有localCtl == groupCtl时，才启动这个
public class HeartMgr extends SysInfo {
	private static Timer helloTimer = new Timer();

	public HeartMgr() {
		System.out.println("HeartMgr启动");

		new Thread(new HelloReceiver()).start();
		new Thread(new ReHelloReceiver()).start();

		Properties props = new Properties();
		String propertiesPath = "./resources/DtConfig.properties";
		try {
			props.load(new FileInputStream(propertiesPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Long firstHelloDelay = Long.parseLong(props.getProperty("firstHelloDelay"));
		reHelloPeriod = Long.parseLong(props.getProperty("reHelloPeriod"));//判断失效阀值
		helloPeriod = Long.parseLong(props.getProperty("helloPeriod"));//发送周期
		helloTaskPeriod = Long.parseLong(props.getProperty("helloTaskPeriod"));//hello任务的执行周期
		nbrGrpExpiration = Long.parseLong(props.getProperty("nbrGrpExpiration"));//邻居集群丢失时间的判断阈值

		helloTimer.schedule(new HelloTask(), firstHelloDelay, helloTaskPeriod);
	}

	//依次向每个outPort发送Hello信息
	private class HelloTask extends TimerTask {
		@Override
		public void run() {
			System.out.println("开始心跳任务");
			for (Switch swt : outSwitches.values()) {
				for (String out : swt.portSet) {
					if (!out.equals("LOCAL")) {

						/*//判断发送的是heart维护还是hello探索
						boolean isKnownGrpLink = false;
						for (GroupLink gl : nbrGrpLinks.values()) {
							if (gl.srcBorderSwtId.equals(swt.id) && gl.srcOutPort.equals(out)) {
								isKnownGrpLink = true;
								break;
							}
						}

						if (!isKnownGrpLink) {*/
						Group localGrp = allGroups.get(localGroupName);
						localGrp.id += 1;
						localGrp.updateTime = System.currentTimeMillis();
						allGroups.put(localGroupName, localGrp);

						List<String> outHello = RouteUtil.calRoute(localSwtId, swt.id);
						List<String> inRehello = RouteUtil.calRoute(swt.id, localSwtId);
						List<Flow> ctl2out = RouteUtil.downInGrpRtFlows(outHello, portWsn2Swt, out, "hello", "sys", groupCtl);
						List<Flow> out2ctl = RouteUtil.downInGrpRtFlows(inRehello, out, portWsn2Swt, "re_hello", "sys", groupCtl);

						sendHello(out, swt.id);
						System.out.println("向交换机" + swt.id + "通过" + out + "端口发送Hello消息");
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
						System.out.println("删除从" + swt.id + "交换机的" + out + "端口发出Hello消息的流表");
						/*} else {
							sendHeartBeat();
						}*/
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
			MultiHandler handler = new MultiHandler(sysPort, "hello", "sys");

			hello.startGroup = localGroupName;
			hello.startOutPort = out;
			hello.startBorderSwtId = swtId;
			hello.reHelloPeriod = reHelloPeriod;
			hello.allGroups = cloneGrpMap(allGroups);

			handler.v6Send(hello);
		}

		private void sendHeartBeat() {
			//可能会多次发送给相同的集群
			Group heart = new Group(localGroupName);
			heart.id += allGroups.get(localGroupName).id;
			heart.updateTime = System.currentTimeMillis();
			heart.dist2NbrGrps = cloneIntMap(allGroups.get(localGroupName).dist2NbrGrps);

			MultiHandler handler = new MultiHandler(sysPort, "heart", "sys");
			handler.v6Send(heart);
		}
	}

}
