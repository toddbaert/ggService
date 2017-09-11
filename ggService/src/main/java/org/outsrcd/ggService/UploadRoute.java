package org.outsrcd.ggService;

import java.io.InputStream;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UploadRoute extends RouteBuilder {
	
	String replayDir;
	
	public UploadRoute(String replayDir) {
		this.replayDir = replayDir;
	}

	class SignatureRequestAggregationStrategy implements AggregationStrategy {
		
		@SuppressWarnings("unchecked")
		@Override
		public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

			// get the results from the s3 upload signature request
			HashMap<String, Object> responseMap = null;
			try (InputStream inputStream = (InputStream) newExchange.getIn().getBody()) {
				responseMap = new ObjectMapper().readValue(inputStream, HashMap.class);
			} catch (Exception e) {
				log.error("Error parsing signature response", e);
			}
			
			// add data from signature request (newExchange) into file data (oldExchange)
			responseMap.forEach((key, value) -> {
				oldExchange.getIn().setHeader(key, value);
			});
			
			return oldExchange;
		}
	}

	@Override
	public void configure() throws Exception {

		// scan replay directory every 60 seconds for new replay, only looking for files without '_processed' marker
		from("file:" + replayDir + 
				"/?recursive=true&delay=60000&filterFile=${file:onlyname} not contains '_processed'")

			// throttle to 1 upload per second max as per ggTracker maintainer request
			.throttle(1)
			.log(">>> found ${file:onlyname}")
			
			// make request to sign the upload for s3 using enrich EIP
			.setHeader(Exchange.HTTP_QUERY, simple("doc[title]=${file:onlyname}"))
			.setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET))
			.enrich("http4://{{config.signature.host}}", new SignatureRequestAggregationStrategy())
			
			// perform multipart upload to s3
			.doTry()
				.to("bean:uploadBean?method=send")
			.endDoTry()			
			.doCatch(Exception.class)
				.log(">>> message: ${exception.message}")
			.end()
			
			// rename file to prevent duplicate uploads
			.to("file:?fileName=${file:parent}/${file:name.noext}_processed.SC2Replay");
	}
}
