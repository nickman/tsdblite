/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2015, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.tsdblite.metric;

import java.util.Date;

import javax.management.ObjectName;

import com.heliosapm.tsdblite.metric.MetricCache.MetricMBean;

/**
 * <p>Title: AppMetricMXBean</p>
 * <p>Description: JMX MXBean interface for {@link AppMetric} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.AppMetricMXBean</code></p>
 */

public interface AppMetricMXBean {
	
	/** The JMX notification type for a new submission */
	public static final String NOTIF_NEW_SUB = "metric.submission";
	
	/**
	 * Returns the last value submitted or null if no submissions have occurred
	 * @return the last value submitted
	 */
	public double getLastValue();

	/**
	 * Returns the timestamp of the last submission as a long UTC 
	 * @return the timestamp of the last submission
	 */
	public long getLastSubmission();

	/**
	 * Returns the timestamp of the last submission as a java Date 
	 * @return the timestamp of the last submission or null if one has not occurred
	 */
	public Date getLastSubmissionDate();

	/**
	 * Returns the metric
	 * @return the metric
	 */
	public MetricMBean getMetric();


	/**
	 * Returns the JMX ObjectName for this metric 
	 * @return the objectName
	 */
	public ObjectName getObjectName();
	
	/**
	 * Returns the number of submission subscribers
	 * @return the number of submission subscribers
	 */
	public int getSubscriberCount();
	
	/**
	 * Returns the count of submission notifications sent
	 * @return the count of submission notifications sent
	 */
	public long getNotificationsSent();
	

}
