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

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.cliffc.high_scale_lib.NonBlockingHashSet;

import com.heliosapm.tsdblite.jmx.Util;
import com.heliosapm.tsdblite.metric.AppMetric.SubNotif;
import com.heliosapm.utils.collections.FluentMap;
import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: MetricSubscription</p>
 * <p>Description: Represents a subscription to submission events for one metric (ObjectName) pattern</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.MetricSubscription</code></p>
 */

public class MetricSubscription implements NotificationListener {
	/** The pattern of the metric subscribed to */
	protected final ObjectName pattern;
	/** A set of the subscribed channels */
	protected final NonBlockingHashMap<String, Channel> subscribedChannels;
	/** A set of the known actual ObjectNames */
	protected final NonBlockingHashSet<ObjectName> objectNames;
	/** A ref to the Metric MBeanServer */
	protected final MBeanServer metricServer;
	
	/** The long hash code of the pattern */
	protected final long key;
	
	/** A MetricSubscription place holder */
	protected static final MetricSubscription PLACEHOLDER = new MetricSubscription();
	
	/** A map of all MetricSubscriptions keyed by the long hash code of the ObjectName pattern */
	protected static final NonBlockingHashMapLong<MetricSubscription> subscriptions = new NonBlockingHashMapLong<MetricSubscription>(128, false);
	
	/** Callback invoked when a new matching metric is registered */
	protected final NotificationListener onNew = new NotificationListener(){
		@Override
		public void handleNotification(final Notification notification, final Object handback) {
			final ObjectName on = ((MBeanServerNotification)notification).getMBeanName();
			if(objectNames.add(on)) {
				JMXHelper.addNotificationListener(metricServer, on, this, null, null);
				final SubNotif initial = MetricCache.getInstance().getSubNotif(on);
				if(initial!=null) {
					final Map<String, Object> data = FluentMap.newMap(String.class, Object.class).fput("rtype", "new-initial-data").fput("data", initial);
					for(Channel channel: subscribedChannels.values()) {
						channel.writeAndFlush(data);
					}
				}								
			}
		}
	};
	
	/** Callback invoked when a subscribed matching metric is unregistered */
	protected final NotificationListener onUnReg = new NotificationListener(){
		@Override
		public void handleNotification(final Notification notification, final Object handback) {			
			objectNames.remove(((MBeanServerNotification)notification).getMBeanName());
		}
	};
	
	
	/**
	 * Retrieves the MetricSubscription for the passed pattern
	 * @param pattern The pattern to subscribe to
	 * @return the MetricSubscription for the passed pattern
	 */
	public static MetricSubscription get(final ObjectName pattern) {
		if(pattern==null) throw new IllegalArgumentException("The passed pattern was null");
		final long key = Util.hashCode(pattern);
		MetricSubscription sub = subscriptions.putIfAbsent(key, PLACEHOLDER);
		if(sub==null || sub==PLACEHOLDER) {
			sub = new MetricSubscription(pattern, key);
			subscriptions.replace(key, sub);
		}
		return sub;
	}
	
	private MetricSubscription(final ObjectName pattern, final long key) {		
		this.pattern = pattern;
		subscribedChannels = new NonBlockingHashMap<String, Channel>();
		this.key = key;
		objectNames = new NonBlockingHashSet<ObjectName>();
		metricServer = MetricCache.getInstance().getMetricServer();
		JMXHelper.addMBeanRegistrationListener(metricServer, this.pattern, onNew, 0);
		JMXHelper.addMBeanUnregistrationListener(metricServer, this.pattern, onUnReg, 0);
		for(ObjectName on: metricServer.queryNames(pattern, null)) {
			if(objectNames.add(on)) {
				JMXHelper.addNotificationListener(metricServer, on, this, null, null);				
			}
		}
	}
	
	private MetricSubscription() {
		pattern = null;
		subscribedChannels = null;
		key = 0;
		objectNames = null;
		metricServer = null;
	}
	
	/**
	 * Adds a channel to this subscription
	 * @param channel the channel to add
	 */
	public void subscribe(final Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null");
		if(subscribedChannels.putIfAbsent(channel.id().asLongText(), channel)==null) {
			final List<SubNotif> initial = new ArrayList<SubNotif>(objectNames.size());
			for(ObjectName on: objectNames) {
				SubNotif sn = MetricCache.getInstance().getSubNotif(on);
				if(sn==null) continue;
				initial.add(sn);
			}
			channel.writeAndFlush(FluentMap.newMap(String.class, Object.class).fput("rtype", "initial-data").fput("data", initial));
		}
	}
	
	/**
	 * Removes a channel from this subscription
	 * @param channel the channel to remove
	 */
	public void remove(final Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null");
		subscribedChannels.remove(channel.id().asLongText());
		if(subscribedChannels.isEmpty()) {
			subscriptions.remove(key);
			try { metricServer.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, onNew); } catch (Exception x) {/* No Op */}
			try { metricServer.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, onUnReg); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (key ^ (key >>> 32));
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
		if (!(obj instanceof MetricSubscription))
			return false;
		MetricSubscription other = (MetricSubscription) obj;
		if (key != other.key)
			return false;
		return true;
	}

	
	
}
