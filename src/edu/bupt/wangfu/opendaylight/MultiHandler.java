package edu.bupt.wangfu.opendaylight;

import edu.bupt.wangfu.module.base.SysInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.MulticastSocket;

/**
 * Created by lenovo on 2016-6-12.
 */
public class MultiHandler extends SysInfo {
	private int port;
	private String v6addr;//形如FF01:0000:0000:0000:0001:2345:6789:abcd，128bit

	public MultiHandler(int port, String topic, String topicType) {
		this.port = port;
		if (topicType.equals("sys")) {
			v6addr = sysTopicAddrMap.get(topic);

			//String[] tmp = this.v6addr.split("");

			this.port += v6addr.charAt(v6addr.length() - 1) - '0';
//			System.out.println("！！！主题为"+v6addr+"端口为"+this.port);
		} else if (topicType.equals("notify")) {
			v6addr = notifyTopicAddrMap.get(topic);
		}
	}

	public Object v6Receive() {
		try {
			MulticastSocket multicastSocket = new MulticastSocket(port);
			Inet6Address inetAddress = (Inet6Address) Inet6Address.getByName(v6addr);
			multicastSocket.joinGroup(inetAddress);//多播套接字加入多播组
			ByteArrayInputStream bais;
			ObjectInputStream ois;

			byte[] data = new byte[409600];
			bais = new ByteArrayInputStream(data);
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length);//创建一个用于接收数据的数据包
			multicastSocket.receive(datagramPacket);//接收数据包
			ois = new ObjectInputStream(bais);
			multicastSocket.close();
			return ois.readObject();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return null;
	}

	public void v6Send(Object obj) {
		try {
			ByteArrayOutputStream baos;
			ObjectOutputStream oos;
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);

			oos.writeObject(obj);
			byte[] msg = baos.toByteArray();
			Inet6Address inetAddress = (Inet6Address) Inet6Address.getByName(v6addr);//根据主题名返回主题的IP地址
			DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, inetAddress, port);//这里的端口没有用，最终转发还是看流表
			MulticastSocket multicastSocket = new MulticastSocket();

//			multicastSocket.setLoopbackMode(true);
//			multicastSocket.setTimeToLive(10);

			multicastSocket.send(datagramPacket);//发送数据包

			multicastSocket.close();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
}