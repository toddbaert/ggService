package org.outsrcd.ggService;

import org.apache.camel.main.Main;

public class Launcher {

	public static void main(String... args) throws Exception {
		Main main = new Main();
		main.addRouteBuilder(new CamelRoute());
		main.run(args);

	}
}
