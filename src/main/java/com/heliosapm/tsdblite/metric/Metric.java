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

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;

/**
 * <p>Title: Metric</p>
 * <p>Description: Represents a unique metric name</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.Metric</code></p>
 */

public class Metric {
	/** The metric name */
	protected final String metricName;
	/** The metric tags */
	protected final Map<String, String> tags;
	/** The long hash code for this metric */
	protected final long hashCode;
	
	/** The UTF8 character set */
	public static final Charset UTF8 = Charset.forName("UTF8");
	/** The hashing function to compute hashes for metric names */
	public static final HashFunction METRIC_NAME_HASHER = Hashing.murmur3_128();

	/**
	 * <p>Title: MetricFunnel</p>
	 * <p>Description: A hashing funnel to generate a [hopefully] unique long hash code for each metric</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.metric.Metric.MetricFunnel</code></p>
	 */
	public static enum MetricFunnel implements Funnel<Metric> {
    /** The funnel instance */
    INSTANCE;
		/**
		 * {@inheritDoc}
		 * @see com.google.common.hash.Funnel#funnel(java.lang.Object, com.google.common.hash.PrimitiveSink)
		 */
		@Override
		public void funnel(final Metric metric, final PrimitiveSink into) {
    	into.putString(metric.metricName, UTF8);
    	for(Map.Entry<String, String> entry: metric.tags.entrySet()) {
    		into.putString(entry.getKey(), UTF8)
    		.putString(entry.getValue(), UTF8);
    	}
    }
  }
	
	
	/**
	 * Creates a new Metric
	 * @param metricName The metric name
	 * @param tags The metric tags
	 */
	public Metric(final String metricName, final Map<String, String> tags) {
		if(metricName==null || metricName.trim().isEmpty()) throw new IllegalArgumentException("The passed metric name was null or empty");
		this.metricName = metricName.trim();
		if(tags==null || tags.isEmpty()) {
			this.tags = Collections.emptyMap();
		} else {
			final TreeMap<String, String> tmp = new TreeMap<String, String>(TagKeySorter.INSTANCE);
			for(Map.Entry<String, String> entry: tags.entrySet()) {
				tmp.put(entry.getKey().trim(), entry.getValue().trim());
			}		
			this.tags = Collections.unmodifiableSortedMap(tmp);
		}
		hashCode = METRIC_NAME_HASHER.hashObject(this, MetricFunnel.INSTANCE).padToLong();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder(metricName).append(":{");
		if(!tags.isEmpty()) {
			for(Map.Entry<String, String> entry: tags.entrySet()) {
				b.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
			}
			b.deleteCharAt(b.length()-1);			
		}
		return b.append("}").toString();
	}
	
	/**
	 * Returns the metric name
	 * @return the metric name
	 */
	public String getMetricName() {
		return metricName;
	}
	
	/**
	 * Returns the metric tags
	 * @return the metric tags
	 */
	public Map<String, String> getTags() {
		return tags;
	}
	
	/**
	 * Returns the metric hash 
	 * @return the metric hash
	 */
	public long getHashCode() {
		return hashCode;
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
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Metric other = (Metric) obj;
		if (hashCode != other.hashCode)
			return false;
		return true;
	}
	
	

	
	
}
