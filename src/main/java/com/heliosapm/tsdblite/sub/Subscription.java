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

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import com.heliosapm.tsdblite.jmx.ManagedDefaultExecutorServiceFactory;
import com.heliosapm.tsdblite.metric.AppMetricMXBean;
import com.heliosapm.tsdblite.metric.MetricCache;
import com.heliosapm.tsdblite.metric.Trace;
import com.heliosapm.utils.jmx.notifcations.ProxySubscriptionService;
import com.heliosapm.utils.tuples.NVP;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.DefaultEventExecutor;

/**
 * <p>Title: Subscription</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.sub.Subscription</code></p>
 */

public class Subscription implements NotificationFilter, NotificationListener {
	/** The subscription pattern */
	final ObjectName pattern;
	/** The subscription type */
	final SubscriptionEvent subType;
	/** The subscription long hash code */
	final long hashCode;
	/** The channels that are subscribed to this subscription */
	final ChannelGroup channelGroup = new DefaultChannelGroup("Subscription", subscriptionEventExecutor);
	/** Indicates if the object name is an actual pattern or if it refers to a single ObjectName */
	final boolean wildcard;
	/** The metric MBeanServer */
	final MBeanServer mbeanServer;
	/** The proxy subscription service */
	final ProxySubscriptionService proxySubservice;
	
	final boolean forObjectName;
	final boolean forNewObjectName;
	
	
	/** The UTF8 character set */
	public static final Charset UTF8 = Charset.forName("UTF8");
	/** The hashing function to compute hashes for subscription */
	private static final HashFunction SUBSCRIPTION_HASHER = Hashing.murmur3_128();
	
	/** A placeholder subscription */
	private static final Subscription PLACEHOLDER = new Subscription();
	
	/** A cache of subscriptions keyed by the subscription long hash code */
	private static final NonBlockingHashMapLong<Subscription> subscriptions = new NonBlockingHashMapLong<Subscription>(128, false);
	/** Executor service for the subscription channel group */
	private static final ExecutorService subscriptionChannelService = new ManagedDefaultExecutorServiceFactory("subscription").newExecutorService(4);
	/** The event executor for the subscription channel group */
	private static final DefaultEventExecutor subscriptionEventExecutor  = new DefaultEventExecutor(subscriptionChannelService); 

	
	private static enum SubscriptionFunnel implements Funnel<NVP<ObjectName, SubscriptionEvent>> {
	    /** The funnel instance */
	    INSTANCE;
			/**
			 * {@inheritDoc}
			 * @see com.google.common.hash.Funnel#funnel(java.lang.Object, com.google.common.hash.PrimitiveSink)
			 */
			@Override
			public void funnel(final NVP<ObjectName, SubscriptionEvent> sub, final PrimitiveSink into) {
				try {
					final ObjectName on = sub.getKey();
					final int subType = sub.getValue().ordinal();
					final boolean patternList = on.isPropertyListPattern();
					into.putString(on.getDomain().trim(), UTF8);
					for(Map.Entry<String, String> entry : on.getKeyPropertyList().entrySet()) {
						into.putString(entry.getKey().trim(), UTF8);
						into.putString(entry.getValue().trim(), UTF8);
					}
					if(patternList) into.putString("*", UTF8);
					into.putInt(subType);
				} catch (Exception ex) {
					throw new RuntimeException("Failed to extract hashcode from sub [" + sub.getKey() + "/" + sub.getValue() + "]", ex);
				}
	    }
	  }
	

	
	/**
	 * Acquires the Subscription for the passed pattern and subscription type
	 * @param pattern The subscription pattern
	 * @param subType the subscription type
	 * @return the Subscription
	 */
	static Subscription get(final ObjectName pattern, final SubscriptionEvent subType, final ProxySubscriptionService proxySubservice) {
		if(pattern==null) throw new IllegalArgumentException("The passed ObjectName was null");
		if(subType==null) throw new IllegalArgumentException("The passed SubscriptionEvent was null");
		final long hashCode = SUBSCRIPTION_HASHER.hashObject(new NVP<ObjectName, SubscriptionEvent>(pattern, subType), SubscriptionFunnel.INSTANCE).padToLong();
		Subscription sub = subscriptions.put(hashCode, PLACEHOLDER);
		if(sub==null || sub==PLACEHOLDER) {
			sub = new Subscription(pattern, subType, hashCode, proxySubservice);
			subscriptions.replace(hashCode, sub);
		}
		return sub;		
	}
	
	private Subscription() {
		pattern = null;
		subType = null;	
		hashCode = -1;
		wildcard = false;
		mbeanServer = null;
		forObjectName = false;
		forNewObjectName = false;
		proxySubservice = null;
	}
	
	
	/**
	 * Creates a new Subscription
	 */
	private Subscription(final ObjectName pattern, final SubscriptionEvent subType, final long hashCode, final ProxySubscriptionService proxySubservice) {
		this.pattern = pattern;
		this.subType = subType;
		this.hashCode = hashCode;
		this.wildcard = pattern.isPattern();
		forObjectName = subType.ordinal() < 3;
		forNewObjectName = subType.ordinal() < 2;
		mbeanServer = MetricCache.getInstance().getMetricMBeanServerInstance();
		this.proxySubservice = proxySubservice;
		if(forObjectName) {
			try {
				mbeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, this, null);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to register subscription listener [" + toString() + "]", ex);
			}			
		} else if(subType==SubscriptionEvent.DATA4METRICS) {
			try {
				proxySubservice.subscribe(pattern, null, this, this, null);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to register subscription listener [" + toString() + "]", ex);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder("Subscription [").append(subType).append(":").append(pattern).append("]").toString();
	}
	
	/**
	 * Indicates if this subscription is a wildcard
	 * @return true if this subscription is a wildcard, false otherwise
	 */
	public boolean isWildcard() {
		return wildcard;
	}
	
	public void onNewMetric(final ObjectName on) {
		channelGroup.write(on);
	}
	
	public void onExpiredMetric(final ObjectName on) {
		channelGroup.write(on);
	}
	
	public void onMetricSubmission(final Trace trace) {
		channelGroup.write(trace);
	}

	/**
	 * Adds a channel to this subscription
	 * @param channel The channel to add
	 * @return true if the channel was added, false otherwise
	 */
	public boolean addChannel(final Channel channel) {
		if(channel!=null && channel.isOpen()) {
			return channelGroup.add(channel);
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(final Notification n, final Object handback) {
		if(forObjectName) {
			MBeanServerNotification m = (MBeanServerNotification)n;
			if(MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(m.getType())) {
				onNewMetric(m.getMBeanName());
			} else {
				onExpiredMetric(m.getMBeanName());
			}
		} else {
			onMetricSubmission((Trace)n.getUserData());
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification n) {
		if(forObjectName) {
			if(n instanceof MBeanServerNotification) {
				final ObjectName on = ((MBeanServerNotification)n).getMBeanName();
				return (wildcard && pattern.apply(on));				
			}
		} else {
			final Object source = n.getSource();
			if(source instanceof ObjectName) {
				final ObjectName on = (ObjectName)source;
				if(pattern.apply(on)) {
					if(AppMetricMXBean.NOTIF_NEW_METRIC.equals(n.getType())) return true;
				}
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (hashCode ^ (hashCode >>> 32));
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
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (hashCode != other.hashCode)
			return false;
		return true;
	}

}
