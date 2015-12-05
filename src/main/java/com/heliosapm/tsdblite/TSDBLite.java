/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.heliosapm.tsdblite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.utils.concurrency.ExtendedThreadManager;
import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.io.StdInCommandHandler;
import com.heliosapm.utils.jmx.JMXHelper;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

/**
 * <p>Title: TSDBLite</p>
 * <p>Description: The boot class for TSDBLite</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.TSDBLite</code></p>
 */

public class TSDBLite {
	/** Static class logger */
	private static final Logger log = LoggerFactory.getLogger(TSDBLite.class);

	/** The server instance */
	private static Server server = null;
	
	/**
	 * Main entry point
	 * @param args None for now
	 */
	public static void main(String[] args) {
		log.info("TSDBLite booting....");	
		ExtendedThreadManager.install();
		InternalLoggerFactory .setDefaultFactory(new Slf4JLoggerFactory());		
		final String jmxmpIface = ConfigurationHelper.getSystemThenEnvProperty(Constants.CONF_JMXMP_IFACE, Constants.DEFAULT_JMXMP_IFACE);
		final int jmxmpPort = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.CONF_JMXMP_PORT, Constants.DEFAULT_JMXMP_PORT);
		JMXHelper.fireUpJMXMPServer(jmxmpIface, jmxmpPort, JMXHelper.getHeliosMBeanServer());
		server = Server.getInstance();
		final Thread mainThread = Thread.currentThread();
		StdInCommandHandler.getInstance().registerCommand("stop", new Runnable(){
			@Override
			public void run() {
				if(server!=null) {
					log.info("Stopping TSDBLite Server.....");
					server.stop();
					log.info("TSDBLite Server Stopped. Bye.");
					mainThread.interrupt();
				}
			}
		});
		try { Thread.currentThread().join(); } catch (Exception x) {/* No Op */}
	}

}
