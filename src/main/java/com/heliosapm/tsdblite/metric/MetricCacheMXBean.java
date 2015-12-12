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
package com.heliosapm.tsdblite.metric;

import javax.management.ObjectName;

import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: MetricCacheMXBean</p>
 * <p>Description: JMX MXBean for the {@link MetricCache} instance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.MetricCacheMXBean</code></p>
 */

public interface MetricCacheMXBean {

	/** The JMX ObjectName for the {@link MetricCache}  */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("com.heliosapm.tsdblite:service=MetricCache");
	
	/**
	 * Returns the number of metrics in the metric cache
	 * @return the metric cache size
	 */
	public long getMetricCacheSize();

	/**
	 * Returns the default domain of the metrics MBeanServer
	 * @return the metric MBeanServer default domain
	 */
	public String getMetricMBeanServer();
	
	/**
	 * Returns the number of registered MBeans in the metrics MBeanServer.
	 * This will be more than the number of metrics.
	 * @return the number of registered MBeans in the metrics MBeanServer.
	 */
	public int getMetricMBeanServerMBeanCount();
	

	/**
	 * Returns the cummulative number of bad metric submissions
	 * @return the cummulative number of bad metric submissions
	 */
	public long getBadMetrics();

	/**
	 * Returns the cummulative number of expired metrics
	 * @return the cummulative number of expired metrics
	 */
	public long getExpiredMetrics();

	/**
	 * Returns the elapsed time of the last expiry dispatch in ms.
	 * @return the elapsed time of the last expiry dispatch in ms.
	 */
	public long getLastExpiryDispatchTime();

	/**
	 * Returns the elapsed time of the last expiry completion in ms.
	 * @return the elapsed time of the last expiry completion in ms.
	 */
	public long getLastExpiryTime();

	/**
	 * Returns the expiry period in ms.
	 * @return the expiry period in ms.
	 */
	public long getExpiryPeriod();

	/**
	 * Returns the metric expiry in ms.
	 * @return the metric expiry in ms.
	 */
	public long getExpiry();
	
}
