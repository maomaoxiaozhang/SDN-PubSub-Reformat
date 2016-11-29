package edu.bupt.wangfu.opendaylight;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.ArrayList;
import java.util.List;

public class RestProcess {

	public static List<String> doClientPost(String url, String body) {
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


}