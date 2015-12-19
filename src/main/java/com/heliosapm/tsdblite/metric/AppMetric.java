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

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.ObjectName;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heliosapm.tsdblite.json.JSON;
import com.heliosapm.utils.jmx.ExposedSubscribersNotificationBroadcaster;
import com.heliosapm.utils.jmx.SharedNotificationExecutor;

/**
 * <p>Title: AppMetric</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.AppMetric</code></p>
 */

public class AppMetric extends ExposedSubscribersNotificationBroadcaster implements AppMetricMXBean {
	/** The metric */
	protected final Metric metric;
	/** The JMX ObjectName the metric is registered under */
	protected final ObjectName objectName;
	/** The last value submitted */
	protected double lastValue = Double.NaN;
	/** The timestamp of the last value submitted */
	protected long lastSubmission = -1L;
	/** The timestamp of the last activity */
	protected long lastActivity = -1L;
	
	
	/** Notification serial number generator */
	protected final AtomicLong notifSerial = new AtomicLong(-1L);
	
	/** Place holder app metric */
	public static final AppMetric PLACEHOLDER = new AppMetric();
	
	private static final MBeanNotificationInfo[] NOTIFS = new MBeanNotificationInfo[] {
		new MBeanNotificationInfo(new String[]{NOTIF_NEW_SUB}, Notification.class.getName(), "Emitted when a new submission is received")
	};
	
	
	/**
	 * Creates a new AppMetric
	 * @param metric The metric
	 */
	public AppMetric(final Metric metric) {
		super(SharedNotificationExecutor.getInstance(), NOTIFS);
		if(metric==null) throw new IllegalArgumentException("The passed metric was null");
		this.metric = metric;
		objectName = this.metric.toObjectName();
		lastActivity = System.currentTimeMillis();
	}
	
	/**
	 * Creates the AppMetric placeholder
	 */
	public AppMetric() {
		this.metric = Metric.PLACEHOLDER;
		objectName = null;
	}
	

	/**
	 * Submits a new trace for this metric
	 * @param trace The trace to apply
	 */
	public void submit(final Trace trace) {		
		lastValue = trace.isDoubleType() ? trace.getDoubleValue() : trace.getLongValue();
		lastSubmission = trace.getTimestampMs();
		lastActivity = System.currentTimeMillis();
		if(hasSubscribers()) {
			final long serial = notifSerial.incrementAndGet();
			final Notification notif = new Notification(NOTIF_NEW_SUB, objectName, notifSerial.incrementAndGet(), lastSubmission, JSON.serializeToString(new SubNotif(objectName.toString(), trace.isDoubleType() ? trace.getDoubleValue() : trace.getLongValue(), lastSubmission, serial)));
			notif.setUserData(trace);
			sendNotification(notif);
		}
	}
	
	/**
	 * Returns the most recent SubNotif for this metric
	 * @return a SubNotif
	 */
	public SubNotif getSubNotif() {
		return new SubNotif(objectName.toString(), lastValue, lastSubmission, -1L);
	}
	
	public class SubNotif {
		@JsonProperty("m")
		final String metric;
		@JsonProperty("v")
		final Number value;
		@JsonProperty("t")
		final long ts;
		@JsonProperty("s")
		final long serial;
		
		/**
		 * Creates a new SubNotif
		 * @param metric The metric ObjectName as a string
		 * @param value The value
		 * @param ts The timestamp
		 * @param serial The notification serial number
		 */
		public SubNotif(final String metric, final Number value, final long ts, final long serial) {
			this.metric = metric;
			this.value = value;
			this.ts = ts;
			this.serial = serial;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.AppMetricMXBean#getNotificationsSent()
	 */
	@Override
	public long getNotificationsSent() {		
		return getDispatchCount();
	}
	

	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.AppMetricMXBean#getSubscriberCount()
	 */
	@Override
	public int getSubscriberCount() {
		return size();
	}
	
	

	/**
	 * Returns the last value submitted or null if no submissions have occurred
	 * @return the last value submitted
	 */
	@Override
	public double getLastValue() {
		return lastValue;
	}

	/**
	 * Returns the timestamp of the last submission as a long UTC 
	 * @return the timestamp of the last submission
	 */
	@Override
	public long getLastSubmission() {
		return lastSubmission;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.AppMetricMXBean#getLastActivity()
	 */
	@Override
	public long getLastActivity() {
		return lastActivity;
	}

	/**
	 * Returns the timestamp of the last submission as a java Date 
	 * @return the timestamp of the last submission or null if one has not occurred
	 */
	@Override
	public Date getLastSubmissionDate() {
		return lastSubmission==-1L ? null : new Date(lastSubmission);
	}

	/**
	 * Returns the metric interface
	 * @return the metric interface
	 */
	@Override
	public MetricMBean getMetric() {
		return metric;
	}
	
	/**
	 * Returns the metric instance
	 * @return the metric instance
	 */
	public Metric getMetricInstance() {
		return metric;
	}
	


	/**
	 * Returns the JMX ObjectName for this metric 
	 * @return the objectName
	 */
	@Override
	public ObjectName getObjectName() {
		return objectName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.AppMetricMXBean#getMetricHashCode()
	 */
	@Override
	public long getMetricHashCode() {
		return metric.getHashCode();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((metric == null) ? 0 : metric.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AppMetric))
			return false;
		AppMetric other = (AppMetric) obj;
		if (metric == null) {
			if (other.metric != null)
				return false;
		} else if (!metric.equals(other.metric))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AppMetric [objectName=");
		builder.append(objectName);
		builder.append(", lastValue=");
		builder.append(lastValue);
		builder.append(", lastSubmission=");
		builder.append(lastSubmission);
		builder.append("]");
		return builder.toString();
	}
	
	

}
