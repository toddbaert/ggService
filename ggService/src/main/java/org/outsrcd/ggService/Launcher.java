package org.outsrcd.ggService;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

public class Launcher {

	public static void main(String... args) throws Exception {

		// if no arg for replay directory to scan,
		if (args == null || args.length < 1) {
			System.out.println("Must provide argument for directory to be scanned");
			System.exit(1);
		}

		// add the MultipartUploadBean to registry, so we can access it in routes
		MultipartUploadBean uploadBean = new MultipartUploadBean();
		SimpleRegistry reg = new SimpleRegistry();
		reg.put("uploadBean", uploadBean);

		// add properties file to classpath
		PropertiesComponent props = new PropertiesComponent();
		props.setLocation("classpath:config.properties");

		// start the context, passing in the replay directory to scan
		CamelContext context = new DefaultCamelContext(reg);
		context.addComponent("properties", props);
		context.addRoutes(new UploadRoute(args[0]));
		context.start();

		// wait
		Thread.currentThread().join();
	}
}
