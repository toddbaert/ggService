package org.outsrcd.ggService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.apache.camel.Exchange;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

public class HttpPostBean {

	Properties _config;

	public HttpPostBean() throws Exception {

		_config = new Properties();
		_config.load(getClass().getClassLoader().getResourceAsStream("config.properties"));

	}

	public void login(Exchange exchange) throws Exception {

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("user[email]", System.getProperty("user")));
		formparams.add(new BasicNameValuePair("user[password]", System.getProperty("password")));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);

		HttpPost httpPost = new HttpPost("http://" + _config.getProperty("config.login.url"));
		httpPost.setEntity(entity);

		Function<List<? extends Header>, Header[]> setHeaderFunction = (headers) -> {
			List<Header> result = new ArrayList<Header>();

			headers.forEach((h) -> {
				result.add(new BasicHeader("cookie", h.getValue()));
			});

			return result.toArray(new Header[0]);
		};

		sendPost(exchange, httpPost, setHeaderFunction);
	}

	public void upload(Exchange exchange) throws Exception {

		Map<String, Object> formData = exchange.getIn().getHeaders();

		File replayFile = exchange.getIn().getBody(File.class);
		HttpEntity entity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)

				// add the text params retreived from the signutre request
				.addTextBody("key", (String) formData.get("key"))
				.addTextBody("AWSAccessKeyId", _config.getProperty("config.s3.accesskeyid"))
				.addTextBody("acl", (String) _config.getProperty("config.s3.acl"))
				.addTextBody("policy", (String) formData.get("policy"))
				.addTextBody("signature", (String) formData.get("signature"))
				.addTextBody("success_action_status", "201")

				// add the file to be uploaded
				.addBinaryBody("file", replayFile).build();

		HttpPost httpPost = new HttpPost("http://" + _config.getProperty("config.s3.host"));
		httpPost.setEntity(entity);
		httpPost.setHeaders((Header[]) exchange.getIn().getHeader("cookie"));
		sendPost(exchange, httpPost, null);
	}

	public void linkUpload(Exchange exchange) throws Exception {

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("file_name", exchange.getIn().getHeader("CamelFileName", String.class)));
		formparams.add(new BasicNameValuePair("s3_key", exchange.getIn().getHeader("key", String.class)));
		formparams.add(new BasicNameValuePair("channel", _config.getProperty("config.replay.channel")));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);

		HttpPost httpPost = new HttpPost("http://" + _config.getProperty("config.drop.url"));
		httpPost.setHeaders((Header[]) exchange.getIn().getHeader("cookie"));
		httpPost.setEntity(entity);
		sendPost(exchange, httpPost, null);
	}

	private void sendPost(Exchange exchange, HttpPost httpPost,
			Function<List<? extends Header>, Header[]> setHeaderFunction) throws Exception {

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
				if (httpResponse == null || httpResponse.getStatusLine() == null) {
					throw new IOException("Unable to make HTTP request");
				}
				int status = httpResponse.getStatusLine().getStatusCode();
				if (status > 399) {
					throw new IOException("Upload request failed with status: " + String.valueOf(status));
				}

				if (setHeaderFunction != null) {
					Header[] setCookieHeaders = httpResponse.getHeaders("Set-Cookie");
					Header[] cookieHeaders = setHeaderFunction.apply(Arrays.asList(setCookieHeaders));
					exchange.getIn().setHeader("cookie", cookieHeaders);
				}
			}
		}
	}
}
