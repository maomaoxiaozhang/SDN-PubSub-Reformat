package edu.bupt.wangfu.opendaylight;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RestProcess {
	public static String REST_URL = "http://192.168.1.30:8080";
	private static String API_KEY = "your api key";
	private static String SECRET_KEY = "your secret key";
	private static int index = 2;
	private static CloseableHttpClient client = HttpClients.createDefault();


	public static String doClientDelete(String url) {
		try {
			HttpClient httpclient = new HttpClient();
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
			httpclient.getState().setCredentials(AuthScope.ANY, creds);
			DeleteMethod deleteMethod = new DeleteMethod(url);
			deleteMethod.setDoAuthentication(true);

			int status = httpclient.executeMethod(deleteMethod);
			System.out.println("the code is " + status);
			String body = deleteMethod.getResponseBodyAsString();
			deleteMethod.releaseConnection();
			return body;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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

	public static String doClientPut(String url, String content) {//nll 用户名密码认证方式
		try {
			HttpClient c = new HttpClient();
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
			c.getState().setCredentials(AuthScope.ANY, creds);
			PutMethod putMethod = new PutMethod(url);
			putMethod.setDoAuthentication(true);

			putMethod.addRequestHeader("Content-Type", "application/xml");
			putMethod.addRequestHeader("Accept", "application/xml");

			putMethod.setRequestBody(content);

			int status = c.executeMethod(putMethod);
			System.out.println("the code is " + status);
			String body = putMethod.getResponseBodyAsString();
			putMethod.releaseConnection();
			return body;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> doClientPost(String url, JSONObject jo) {
		List<String> result = new ArrayList<>();

		HttpPost method = new HttpPost(url);
		StringEntity entity = new StringEntity(jo.toString(), "utf-8");
		method.setHeader("Content-Type", "application/xml; charset=UTF-8");
		method.setEntity(entity);

		try {
			CloseableHttpResponse statusCode = client.execute(method);
			result.add(statusCode.toString());
			BufferedReader reader = new BufferedReader(new InputStreamReader(statusCode.getEntity().getContent(), "utf-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				result.add(line);
			}
		} catch (UnsupportedOperationException | IOException e) {

			e.printStackTrace();
		}

		return result;
	}
}