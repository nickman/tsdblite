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
package com.heliosapm.tsdblite.metric;

import io.netty.util.internal.chmv8.ConcurrentHashMapV8;

import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: MetricMeta</p>
 * <p>Description: Represents a map of meta key value pairs</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.MetricMeta</code></p>
 */

public class MetricMeta implements MetricMetaMXBean {
	/** The meta's long hash code */
	final long hashCode;
	/** The timestamp of he last update */
	final AtomicLong lastUpdate = new AtomicLong(-1L);
	/** The meta name value pairs */
	final ConcurrentHashMapV8<String, String> pairs;

	/** A MetricMeta place holder */
	static final MetricMeta PLACEHOLDER = new MetricMeta();
	
	private MetricMeta() {
		hashCode = -1;
		pairs = null;
	}
	
	/**
	 * Creates a new MetricMeta
	 * @param hashCode The meta's long hash code
	 * @param name The name of the meta pair to initialize with
	 * @param value The value of the meta pair to initialize with
	 */
	MetricMeta(final long hashCode, final String name, final String value) {
		this.hashCode = hashCode;
		pairs = new ConcurrentHashMapV8<String, String>(8);
		addPair(name, value);
		lastUpdate.set(System.currentTimeMillis());
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.MetricMetaMXBean#getHashCode()
	 */
	@Override
	public long getHashCode() {
		return hashCode;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.MetricMetaMXBean#getLastUpdateTs()
	 */
	@Override
	public long getLastUpdateTs() {
		return lastUpdate.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.MetricMetaMXBean#getLastUpdate()
	 */
	@Override
	public Date getLastUpdate() {
		return new Date(lastUpdate.get());
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.MetricMetaMXBean#getMeta()
	 */
	@Override
	public SortedMap<String, String> getMeta() {
		return new TreeMap<String, String>(pairs);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.MetricMetaMXBean#getSize()
	 */
	@Override
	public int getSize() {		
		return pairs.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.MetricMetaMXBean#addPair(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean addPair(final String name, final String value) {
		if(name!=null && !name.trim().isEmpty() && value!=null && !value.trim().isEmpty()) {
			if(pairs.putIfAbsent(name.trim(), value.trim())==null) {
				lastUpdate.set(System.currentTimeMillis());
				return true;
			}
		}
		return false;
	}
	
}
