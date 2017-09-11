package org.outsrcd.ggService;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

public class Launcher {

	public static void main(String... args) throws Exception {		
		
		MultipartUploadBean uploadBean = new MultipartUploadBean();
	    SimpleRegistry reg = new SimpleRegistry();
	    reg.put("uploadBean", uploadBean);
		
	    PropertiesComponent props = new PropertiesComponent();
		props.setLocation("classpath:config.properties");
		
	    CamelContext context = new DefaultCamelContext(reg);
		context.addComponent("properties", props);
	    context.addRoutes(new UploadRoute());
	    context.start();
	}
}
