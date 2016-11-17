package edu.bupt.wangfu.info.device;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by root on 15-10-5.
 */
public class Host extends DevInfo {
	public String ip;
	public String swtId;
	public String port;//swt上和这个host连接的端口的端口号

	public Host() {
	}

	public Host(String ip) {
		this.ip = ip;
	}

	public static void main(String[] args) {
		Host x = new Host("10.108.166.15");
		System.out.println(x.getMac());
	}

	@Override
	public String getMac() {
		//TODO 可能会因为操作系统的不同，而改变这个函数，需要测试
		if (this.mac == null) {
			InetAddress ia;
			byte[] mac = null;
			try {
				ia = InetAddress.getByName(this.ip);
				mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
			} catch (UnknownHostException | SocketException e) {
				e.printStackTrace();
			}

			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < mac.length; i++) {
				if (i != 0) {
					sb.append(":");
				}
				String s = Integer.toHexString(mac[i] & 0xFF);
				sb.append(s.length() == 1 ? 0 + s : s);
			}

			this.mac = sb.toString().toLowerCase();
		}
		return this.mac;
	}
}
