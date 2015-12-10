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

import java.lang.management.ManagementFactory;

import io.netty.handler.logging.LogLevel;

/**
 * <p>Title: Constants</p>
 * <p>Description: Configuration constants and defaults</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.Constants</code></p>
 */

public abstract class Constants {
	
	/** The number of cores available to this JVM */
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	
	/** The conf property name for the main netty listening port */
	public static final String CONF_NETTY_PORT = "netty.port";
	/** The default main netty listening port */
	public static final int DEFAULT_NETTY_PORT = 4242;

	/** The conf property name for the main netty bind interface */
	public static final String CONF_NETTY_IFACE = "netty.iface";
	/** The default main netty bind interface */
	public static final String DEFAULT_NETTY_IFACE = "0.0.0.0";

	/** The conf property name for the JMXMP listening port */
	public static final String CONF_JMXMP_PORT = "jmxmp.port";
	/** The default JMXMP listening port */
	public static final int DEFAULT_JMXMP_PORT = 4245;

	/** The conf property name for the JMXMP bind interface */
	public static final String CONF_JMXMP_IFACE = "jmxmp.iface";
	/** The default JMXMP bind interface */
	public static final String DEFAULT_JMXMP_IFACE = "0.0.0.0";
	
	/** The conf property name for the Metrics MBeanServer JMXMP listening port */
	public static final String CONF_METRICS_JMXMP_PORT = "jmxmp.port";
	/** The default Metrics MBeanServer JMXMP listening port */
	public static final int DEFAULT_METRICS_JMXMP_PORT = 4243;

	/** The conf property name for the Metrics MBeanServer JMXMP bind interface */
	public static final String CONF_METRICS_JMXMP_IFACE = "jmxmp.iface";
	/** The default Metrics MBeanServer JMXMP bind interface */
	public static final String DEFAULT_METRICS_JMXMP_IFACE = "0.0.0.0";
	
	/** The conf property name for the mbean server name that metrics are published to */
	public static final String CONF_METRICS_MSERVER = "metrics.domain";
	/** The default mbean server name that metrics are published to */
	public static final String DEFAULT_METRICS_MSERVER = "DefaultDomain";

	/** The conf property name for the "host/app" first object name model of published metric mbeans fflag */
	public static final String CONF_METRICS_HOSTAPP_MODE = "metrics.hostapp";
	/** The default mbean server name that metrics are published to */
	public static final boolean DEFAULT_METRICS_HOSTAPP_MODE = false;
	
	
	/** The conf property name for the netty boss pool thread count */
	public static final String CONF_NETTY_BOSS_THREADS = "netty.poolsize.boss";
	/** The default netty boss pool thread count */
	public static final int DEFAULT_NETTY_BOSS_THREADS = 1;
	
	/** The conf property name for the netty worker pool thread count */
	public static final String CONF_NETTY_WORKER_THREADS = "netty.poolsize.worker";
	/** The default netty worker pool thread count */
	public static final int DEFAULT_NETTY_WORKER_THREADS = CORES * 2;
	
	/** The conf property name for the netty channel group pool thread count */
	public static final String CONF_NETTY_CGROUP_THREADS = "netty.poolsize.group";
	/** The default netty channel group pool thread count */
	public static final int DEFAULT_NETTY_CGROUP_THREADS = CORES;
	
	/** The conf property name for the netty event executor thread count */
	public static final String CONF_NETTY_EVENT_THREADS = "netty.poolsize.group";
	/** The default netty event executor thread count */
	public static final int DEFAULT_NETTY_EVENT_THREADS = CORES;
	
	
	/** The conf property name for the netty server channel logging level */
	public static final String CONF_NETTY_SERVER_LOGLEVEL = "netty.loglevel.server";
	/** The default netty server channel logging level */
	public static final String DEFAULT_NETTY_SERVER_LOGLEVEL = LogLevel.INFO.name();
	
	/** The conf property name for the netty child channel logging level */
	public static final String CONF_NETTY_CHILD_LOGLEVEL = "netty.loglevel.child";
	/** The default netty child channel logging level */
	public static final String DEFAULT_NETTY_CHILD_LOGLEVEL = LogLevel.INFO.name();
	
}
