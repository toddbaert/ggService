package org.outsrcd.ggService;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

public class Launcher {

	public static void main(String... args) throws Exception {

		// if no arg for replay directory to scan,
		if (args == null || args.length < 3) {
			System.out.println("args must include: replay directory, user name, and passowrd");
			System.exit(1);
		} else {
			System.setProperty("replayDir", args[0]);
			System.setProperty("user", args[1]);
			System.setProperty("password", args[2]);
		}
		
		SimpleRegistry reg = new SimpleRegistry();
		
		// add the MultipartUploadBean to registry, so we can access it in routes
		MultipartUploadBean multipartUploadBean = new MultipartUploadBean();
		reg.put("multipartUploadBean", multipartUploadBean);

		// get properties file so we can add it to the context
		PropertiesComponent props = new PropertiesComponent();
		props.setLocation("classpath:config.properties");

		// start the context, passing in the replay directory to scan
		CamelContext context = new DefaultCamelContext(reg);
		context.addComponent("properties", props);
		context.addRoutes(new UploadRoute());
		context.start();

		// wait
		Thread.currentThread().join();
	}
}
