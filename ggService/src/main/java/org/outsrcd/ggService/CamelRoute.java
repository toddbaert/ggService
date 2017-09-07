package org.outsrcd.ggService;

import org.apache.camel.builder.RouteBuilder;

public class CamelRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		from("timer:testTimer?fixedRate=true&period=1000")
			.log("testing...");
		
	}
}
