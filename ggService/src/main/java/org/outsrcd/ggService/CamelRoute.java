package org.outsrcd.ggService;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CamelRoute extends RouteBuilder {

	class SignatureUrlAggregationStrategy implements AggregationStrategy {
		@Override
		public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

			HashMap<String, Object> result = null;
			try (InputStream inputStream = (InputStream) newExchange.getIn().getBody()) {
				result = new ObjectMapper().readValue(inputStream, HashMap.class);
			} catch (Exception e) {
				log.error("Error parsing signature response");
			}

			GenericFile<File> file = (GenericFile<File>) oldExchange.getIn().getBody();

			return null;
		}
	}

	@Override
	public void configure() throws Exception {

		PropertiesComponent props = new PropertiesComponent();
		props.setLocation("classpath:config.properties");
		this.getContext().addComponent("properties", props);

		/*
		 * from("timer:testTimer?fixedRate=true&period=1000") .log("testing...");
		 */

		from("file:/home/todd/temp/").log("{{config.s3.key}}").log("got ${file:onlyname}")
				.setHeader(Exchange.HTTP_QUERY, simple("doc[title]=${file:onlyname}"))
				.setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET)).log(">>> ${header.CamelHttpQuery}")
				.enrich("http4://{{config.signature.host}}", new SignatureUrlAggregationStrategy())
				.to("http4://gg2-replays-prod.s3.amazonaws.com/");

	}
}
