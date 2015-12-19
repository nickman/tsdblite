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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.management.ObjectName;

import com.fasterxml.jackson.databind.JsonNode;
import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: Metric</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.Metric</code></p>
 */

public class Metric implements MetricMBean {

	/** Placeholder metric */
	public static final Metric PLACEHOLDER = new Metric();

	
	/** The metric name */
	protected final String metricName;
	/** The metric tags */
	protected final TreeMap<String, String> tags;
	/** The long hash code for this metric */
	protected final long hashCode;
	
	
	
	private Metric() {
		metricName = null;
		tags = null;
		hashCode = 0;
	}
	
	/**
	 * Creates a new Metric
	 * @param metricName The metric name
	 * @param tags The metric tags
	 * @param hashCode The long hash code
	 */
	Metric(final String metricName, final Map<String, String> tags, final long hashCode) {
		if(metricName==null || metricName.trim().isEmpty()) throw new IllegalArgumentException("The passed metric name was null or empty");
		this.metricName = metricName.trim();
		if(tags==null || tags.isEmpty()) {
			this.tags = new TreeMap<String, String>();
		} else {
			final TreeMap<String, String> tmp = new TreeMap<String, String>(TagKeySorter.INSTANCE);
			for(Map.Entry<String, String> entry: tags.entrySet()) {
				tmp.put(entry.getKey().trim(), entry.getValue().trim());
			}		
			this.tags = tmp;
		}
		this.hashCode = hashCode;
	}
	
	/**
	 * Creates a new Metric
	 * @param node The JSON node
	 * @param hashCode The long hash code
	 */
	Metric(final JsonNode node, final long hashCode) {
		metricName = node.get("metric").textValue();
		final JsonNode tags = node.get("tags");
		if(tags!=null) {
			final TreeMap<String, String> tmp = new TreeMap<String, String>(TagKeySorter.INSTANCE);
			for(final Iterator<String> keyIter = tags.fieldNames(); keyIter.hasNext();) {
				final String key = keyIter.next();
				tmp.put(key.trim(), tags.get(key).textValue().trim());
			}
			this.tags = tmp;
		} else {
			this.tags = new TreeMap<String, String>();
		}
		this.hashCode = hashCode;
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
	 * Renders the tags in a simple map format
	 * @return the rendered map
	 */
	public String tagsToStr() {
		final StringBuilder b = new StringBuilder();
		for(Map.Entry<String, String> entry: tags.entrySet()) {
			b.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
		}
		return b.deleteCharAt(b.length()-1).toString();
	}
	
	/**
	 * Generates a JMX ObjectName for this metric
	 * @return a JMX ObjectName
	 */
	public ObjectName toObjectName() {			
		return JMXHelper.objectName(new StringBuilder(metricName)
			.append(":").append(tagsToStr())
		);
	}
	
	public ObjectName toHostObjectName() {
		final StringBuilder b = new StringBuilder("metrics.");
		TreeMap<String, String> tgs = new TreeMap<String, String>(tags);
		String h = tgs.remove("host");
		String a = tgs.remove("app");
		final String host = h==null ? "unknownhost" : h;
		final int segIndex = metricName.indexOf('.');			
		final String seg = segIndex==-1 ? metricName : metricName.substring(0, segIndex);
		b.append(host).append(".").append(seg).append(":");
		if(segIndex!=-1) {
			tgs.put("app", metricName.substring(segIndex+1));
		}
		for(Map.Entry<String, String> entry: tgs.entrySet()) {
			b.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
		}
		b.deleteCharAt(b.length()-1);
		return JMXHelper.objectName(b);
	}
	
	/**
	 * Returns the metric name
	 * @return the metric name
	 */
	@Override
	public String getMetricName() {
		return metricName;
	}
	
	/**
	 * Returns the metric tags
	 * @return the metric tags
	 */
	@Override
	public TreeMap<String, String> getTags() {
		return tags;
	}
	
	@Override
	public String getTagStr() {
		return tagsToStr();
	}
	
	/**
	 * Returns the metric hash 
	 * @return the metric hash
	 */
	@Override
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
