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
package com.heliosapm.tsdblite.sub;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.heliosapm.utils.jmx.notifcations.NotificationListenerFilter;

/**
 * <p>Title: SubscriptionEvent</p>
 * <p>Description: Defines the subjects that a remote client can subscribe to</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.sub.SubscriptionEvent</code></p>
 */

public enum SubscriptionEvent {
	/** Metric name notifications emitted for all new and expired metrics matching a specified pattern */
	METRICS,
	/** Metric name notifications emitted for all new metrics matching a specified pattern */
	NEWMETRICS,
	/** Metric name notifications emitted for all expired metrics matching a specified pattern */
	EXPIREDMETRICS,
	/** Data notifications emitted for all current metrics matching a specified pattern */
	DATA4METRICS;
	
	
	
	public abstract static class NewMetricListenerFactory implements NotificationListenerFactory {
		public abstract void handleSubNotification(final Subscription sub, final ObjectName on, final Notification n);
		public NotificationListener createListener(final Subscription sub) {
			if(sub==null) throw new IllegalArgumentException("The passed Subscription was null");
			return new NotificationListener() {
				@Override
				public void handleNotification(final Notification n, final Object handback) {
					if(n==null) return;
					final Object source = n.getSource();
					if(source instanceof ObjectName) {
						handleSubNotification(sub, (ObjectName)source, n);
					}					
				}
			};
		}
	}
	
	public abstract static class NewMetricListenerFilterFactory implements NotificationListenerFactory {
		public abstract void handleSubNotification(final Subscription sub, final ObjectName on, final Notification n);
		public boolean handleFilter(final ObjectName on) {
			return false;
		}
		public NotificationListener createListener(final Subscription sub) {
			if(sub==null) throw new IllegalArgumentException("The passed Subscription was null");
			final NotificationListenerFilter nlf = new NotificationListenerFilter() {
				@Override
				public void handleNotification(final Notification n, final Object handback) {
					if(n==null) return;
					final Object source = n.getSource();
					if(source instanceof ObjectName) {
						handleSubNotification(sub, (ObjectName)source, n);
					}
				}
				@Override
				public boolean isNotificationEnabled(final Notification n) {
					if(n==null) return false;
					final Object source = n.getSource();
					if(source instanceof ObjectName) {
						return handleFilter((ObjectName)n.getSource());
					}
					return false;
				}
			};
			return nlf;
		}
	}
	
	
}
