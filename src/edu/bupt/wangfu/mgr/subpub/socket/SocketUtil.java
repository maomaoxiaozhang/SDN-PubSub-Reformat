package edu.bupt.wangfu.mgr.subpub.socket;

import java.io.*;
import java.net.Socket;

/**
 * Created by lenovo on 2016-11-4.
 */
public class SocketUtil {
	//返回输出流对象，向输出流写数据，就能向对方发送数据，os.writeObject(user);
	public static ObjectOutputStream getObjWriter(Socket socket) throws IOException {
		OutputStream socketOut = socket.getOutputStream();
		return new ObjectOutputStream(socketOut);
	}

	//返回输入流对象;只需从输入流读数据,就能接收来自对方的数据，Object obj = is.readObject();
	public static ObjectInputStream getObjReader(Socket socket) throws IOException {
		InputStream socketIn = socket.getInputStream();
		return new ObjectInputStream(new BufferedInputStream(socketIn));
	}
}
