package edu.bupt.wangfu.test;

import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.MulticastSocket;

public class Receive_str1 {

	public static void main(String[] args) {
		new Thread(new Topic1Receiver()).start();
		new Thread(new Topic2Receiver()).start();
		new Thread(new Topic3Receiver()).start();
		new Thread(new Topic4Receiver()).start();
		new Thread(new Topic5Receiver()).start();
	}

	private static class Topic1Receiver implements Runnable {

		@Override
		public void run() {
			int n = 0;
			try {
				Inet6Address inetAddress = (Inet6Address) Inet6Address.getByName("FF0E:0080:0401:0000:0000:0000:0000:0000");
				byte[] data = new byte[100];
				MulticastSocket multicastSocket = new MulticastSocket(8000);
				multicastSocket.joinGroup(inetAddress);
				multicastSocket.setReceiveBufferSize(100 * 1024 * 1024);

				System.out.println("msg1 start listening");
				while (true) {
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
					multicastSocket.receive(datagramPacket);
					n ++;
					if (n % 200 == 0)
						System.out.println("收到第" + n + "条msg1消息: " + new String(data).trim() + "--" + System.currentTimeMillis());
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}
	private static class Topic2Receiver implements Runnable {

		@Override
		public void run() {
			int n = 0;
			try {
				Inet6Address inetAddress = (Inet6Address) Inet6Address.getByName("FF0E:0080:0402:0000:0000:0000:0000:0000");
				byte[] data = new byte[100];
				MulticastSocket multicastSocket = new MulticastSocket(8001);
				multicastSocket.joinGroup(inetAddress);
				multicastSocket.setReceiveBufferSize(100 * 1024 * 1024);

				System.out.println("msg2 start listening");
				while (true) {
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
					multicastSocket.receive(datagramPacket);
					n ++;
					if (n % 200 == 0)
						System.out.println("收到第" + n + "条msg2消息: " + new String(data).trim() + "--" + System.currentTimeMillis());
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}
	private static class Topic3Receiver implements Runnable {

		@Override
		public void run() {
			int n = 0;
			try {
				Inet6Address inetAddress = (Inet6Address) Inet6Address.getByName("FF0E:0080:0403:0000:0000:0000:0000:0000");
				byte[] data = new byte[100];
				MulticastSocket multicastSocket = new MulticastSocket(8002);
				multicastSocket.joinGroup(inetAddress);
				multicastSocket.setReceiveBufferSize(100 * 1024 * 1024);

				System.out.println("msg3 start listening");
				while (true) {
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
					multicastSocket.receive(datagramPacket);
					n ++;
					if (n % 200 == 0)
						System.out.println("收到第" + n + "条msg3消息: " + new String(data).trim() + "--" + System.currentTimeMillis());
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}
	private static class Topic4Receiver implements Runnable {

		@Override
		public void run() {
			int n = 0;
			try {
				Inet6Address inetAddress = (Inet6Address) Inet6Address.getByName("FF0E:0080:0401:0040:0000:0000:0000:0000");
				byte[] data = new byte[100];
				MulticastSocket multicastSocket = new MulticastSocket(8003);
				multicastSocket.joinGroup(inetAddress);
				multicastSocket.setReceiveBufferSize(100 * 1024 * 1024);

				System.out.println("msg4 start listening");
				while (true) {
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
					multicastSocket.receive(datagramPacket);
					n ++;
					if (n % 200 == 0)
						System.out.println("收到第" + n + "条msg4消息: " + new String(data).trim() + "--" + System.currentTimeMillis());
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}
	private static class Topic5Receiver implements Runnable {

		@Override
		public void run() {
			int n = 0;
			try {
				Inet6Address inetAddress = (Inet6Address) Inet6Address.getByName("FF0E:0080:0401:0080:0000:0000:0000:0000");
				byte[] data = new byte[100];
				MulticastSocket multicastSocket = new MulticastSocket(8004);
				multicastSocket.joinGroup(inetAddress);
				multicastSocket.setReceiveBufferSize(100 * 1024 * 1024);

				System.out.println("msg5 start listening");
				while (true) {
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
					multicastSocket.receive(datagramPacket);
					n ++;
					if (n % 200 == 0)
						System.out.println("收到第" + n + "条msg5消息: " + new String(data).trim() + "--" + System.currentTimeMillis());
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}

}
