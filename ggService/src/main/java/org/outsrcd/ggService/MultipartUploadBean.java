package org.outsrcd.ggService;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class MultipartUploadBean {

	Properties _config;

	public MultipartUploadBean() throws Exception {

		_config = new Properties();
		_config.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
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

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
				if (httpResponse == null || httpResponse.getStatusLine() == null) {
					throw new IOException("Unable to make HTTP request");
				}
				int status = httpResponse.getStatusLine().getStatusCode();
				if (status > 399) {
					throw new IOException("Upload request failed with status: " + String.valueOf(status));
				}
			}
		}
	}
}
