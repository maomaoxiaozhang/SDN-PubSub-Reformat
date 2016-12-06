package edu.bupt.wangfu.opendaylight;

import edu.bupt.wangfu.info.device.Controller;
import edu.bupt.wangfu.mgr.base.SysInfo;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RestProcess extends SysInfo {

	public static List<String> doClientPost(String url, String body) {
		System.out.println("sending POST to " + url);
		if (!url.startsWith("http://")) url = "http://" + url;
		String new_url = url + "/restconf/operations/sal-flow:add-flow";
		int status = 0;
		try {
			HttpClient httpclient = new HttpClient();
			UsernamePasswordCredentials cred = new UsernamePasswordCredentials("admin", "admin");
			httpclient.getState().setCredentials(AuthScope.ANY, cred);

			PostMethod postMethod = new PostMethod(new_url);
			postMethod.setRequestHeader("Content-Type", "application/xml");
			postMethod.setRequestBody(body);

			postMethod.setDoAuthentication(true);

			status = httpclient.executeMethod(postMethod);
			System.out.println("Post Status:" + status);

		} catch (Exception e) {
			e.printStackTrace();
		}

		List<String> result = new ArrayList<>();
		result.add(Integer.toString(status));
		return result;
	}

	/**
	 * new modify by HanB
	 */
	public static List<String> doClientDelete(String url, String body) {
		System.out.println("sending DELETE to " + url);
		if (!url.startsWith("http://")) url = "http://" + url;
		String new_url = url + "/restconf/operations/sal-flow:remove-flow";
		List<String> result = new ArrayList<>();
		int status = 0;
		try {
			HttpClient httpclient = new HttpClient();
			UsernamePasswordCredentials cred = new UsernamePasswordCredentials("admin", "admin");
			httpclient.getState().setCredentials(AuthScope.ANY, cred);

			PostMethod postMethod = new PostMethod(new_url);
			postMethod.setRequestHeader("Content-Type", "application/xml");
			postMethod.setRequestBody(body);

			postMethod.setDoAuthentication(true);

			status = httpclient.executeMethod(postMethod);
			System.out.println("Delete Status:" + status);

		} catch (Exception e) {
			e.printStackTrace();
		}
		result.add(Integer.toString(status));
		return result;
	}

	public static List<String> doClientUpdate(String url, String body) {
		System.out.println("sending UPDATE to " + url);
		if (!url.startsWith("http://")) url = "http://" + url;
		url += "/restconf/operations/sal-flow:update-flow";
		int status = 0;
		try {
			HttpClient httpclient = new HttpClient();
			UsernamePasswordCredentials cred = new UsernamePasswordCredentials("admin", "admin");
			httpclient.getState().setCredentials(AuthScope.ANY, cred);

			PostMethod postMethod = new PostMethod(url);
			postMethod.setRequestHeader("Content-Type", "application/xml");
			postMethod.setRequestBody(body);

			postMethod.setDoAuthentication(true);

			status = httpclient.executeMethod(postMethod);
			System.out.println("Update Status:" + status);

		} catch (Exception e) {
			e.printStackTrace();
		}
		ArrayList<String> result = new ArrayList<>();
		result.add(Integer.toString(status));
		return result;
	}

	public static String doClientGet(String url) {//nll 用户名密码认证方式
		System.out.println("sending GET to " + url);
		try {
			HttpClient httpclient = new HttpClient();
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
			httpclient.getState().setCredentials(AuthScope.ANY, creds);
			GetMethod getMethod = new GetMethod(url);
			getMethod.setDoAuthentication(true);

			int status = httpclient.executeMethod(getMethod);
			System.out.println("the code is " + status);
			String body = getMethod.getResponseBodyAsString();
			getMethod.releaseConnection();
			return body;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getBrNameByTpid(Controller ctrl, String tpid) {
		if (id2NameMap.containsKey(tpid))
			return id2NameMap.get(tpid);
		String brName = "";
		String url = ctrl.url + "/restconf/operational/opendaylight-inventory:nodes/node/openflow:" + tpid;
		String body = RestProcess.doClientGet(url);
		assert body != null;
		try {
			JSONObject json = new JSONObject(body);
			JSONArray node = json.getJSONArray("node");
			JSONArray node_connector = node.getJSONObject(0).getJSONArray("node-connector");
			for (int i = 0; i < node_connector.length(); i++) {
				JSONObject term = node_connector.getJSONObject(i);
				String portName = term.getString("flow-node-inventory:name");
				if (portName.startsWith("br")) {
					brName = portName;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("got br name with tpid: " + tpid + ", and the name is " + brName);
		id2NameMap.put(tpid, brName);
		return brName;
	}

}