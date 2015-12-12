/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.tsdblite.sub;

import javax.management.ObjectName;

import com.heliosapm.tsdblite.metric.MetricCache;
import com.heliosapm.utils.jmx.notifcations.ProxySubscriptionService;

/**
 * <p>Title: SubscriptionManager</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.sub.SubscriptionManager</code></p>
 */

public class SubscriptionManager {
	/** The singleton instance */
	private static volatile SubscriptionManager instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/**
	 * Acquires and returns the SubscriptionManager singleton
	 * @return the SubscriptionManager singleton
	 */
	public static SubscriptionManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SubscriptionManager();
				}
			}
		}
		return instance;
	}
	
	
	
	private final ProxySubscriptionService proxySubService; 
	
	
	/**
	 * Creates a new SubscriptionManager
	 */
	private SubscriptionManager() {		
		proxySubService = new ProxySubscriptionService(MetricCache.getInstance().getMetricMBeanServerInstance(), null);
	}

	public ProxySubscriptionService getProxySub() {
		return proxySubService;
	}
	
	/**
	 * Acquires the Subscription for the passed pattern and subscription type
	 * @param pattern The subscription pattern
	 * @param subType the subscription type
	 * @return the Subscription
	 */
	public Subscription get(final ObjectName pattern, final SubscriptionEvent subType) {
		return Subscription.get(pattern, subType, proxySubService);
	}

	
}
