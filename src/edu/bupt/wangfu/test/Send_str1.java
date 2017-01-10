package edu.bupt.wangfu.test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.MulticastSocket;

public class Send_str1 {
	public static void main(String[] args) {
		String[] topicV6Addrs = new String[5];
		topicV6Addrs[0] = "FF0E:0080:0401:0000:0000:0000:0000:0000";
		topicV6Addrs[1] = "FF0E:0080:0402:0000:0000:0000:0000:0000";
		topicV6Addrs[2] = "FF0E:0080:0403:0000:0000:0000:0000:0000";
		topicV6Addrs[3] = "FF0E:0080:0401:0040:0000:0000:0000:0000";
		topicV6Addrs[4] = "FF0E:0080:0401:0080:0000:0000:0000:0000";
		try {
			for (int i = 0; i < 5; i++) {
				MulticastSocket multicastSocket = new MulticastSocket();
				Inet6Address inetAddress = (Inet6Address) Inet6Address.getByName(topicV6Addrs[i]);
				multicastSocket.joinGroup(inetAddress);//do send also need to join group
				multicastSocket.setSendBufferSize(100 * 1024 * 1024);
				for (int j = 0; j < Integer.parseInt(args[0]); j++) {

					byte[] msg = ("msg" + (i + 1) + "  " + System.currentTimeMillis()).getBytes();
					DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, inetAddress, Integer.parseInt("800" + i));
					multicastSocket.send(datagramPacket);

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(Integer.parseInt(args[0]) + "条消息已发送完毕！");
	}
}