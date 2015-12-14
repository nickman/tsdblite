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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.StringValueExp;
import javax.management.remote.JMXServiceURL;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import com.heliosapm.tsdblite.Constants;
import com.heliosapm.tsdblite.jmx.ManagedDefaultExecutorServiceFactory;
import com.heliosapm.tsdblite.jmx.Util;
import com.heliosapm.tsdblite.metric.AppMetric.SubNotif;
import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.time.SystemClock;
import com.heliosapm.utils.tuples.NVP;

import jsr166e.LongAdder;

/**
 * <p>Title: MetricCache</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.MetricCache</code></p>
 */

public class MetricCache implements MetricCacheMXBean {
	/** The singleton instance */
	private static volatile MetricCache instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The metric cache, keyed by the long hash code */
	protected final NonBlockingHashMapLong<AppMetric> metricCache = new NonBlockingHashMapLong<AppMetric>(8096, false);
	/** The metrics MBeanServer */
	protected final MBeanServer metricMBeanServer;
	/** The UTF8 character set */
	public static final Charset UTF8 = Charset.forName("UTF8");
	/** The hashing function to compute hashes for metric names */
	public static final HashFunction METRIC_NAME_HASHER = Hashing.murmur3_128();
	
	/** A counter for bad metric submissions */
	private final LongAdder badMetrics = new LongAdder();
	/** A counter for expired metrics */
	private final LongAdder expiredMetrics = new LongAdder();
	/** The last expiry dispatch elapsed time in ms */
	private final AtomicLong lastExpiryDispatchTime = new AtomicLong();
	/** The last expiry completion elapsed time in ms */
	private final AtomicLong lastExpiryTime = new AtomicLong();
	
	
	/** The expiry period on which expired metrics are scanned for in ms. */
	final long expiryPeriod;
	/** The maximum elapsed time an inactive metric can live for  in ms. */
	final long expiry;
	
	/** The attribute names the expiry inquiry will request from each metric */
	private static final String[] EXPIRY_ATTRIBUTES = new String[]{"MetricHashCode", "LastSubmission"};

	
	/** The expiry task pool */
	private final ExecutorService expiryService = new ManagedDefaultExecutorServiceFactory("expiry").newExecutorService(4);
	/** The expiry task scheduler thread */
	private final Thread expiryThread;
	/** The JMX query to find all registered instances of {@link com.heliosapm.tsdblite.metric.AppMetricMXBean} */
	private final QueryExp metricBeanQuery = Query.isInstanceOf(new StringValueExp("com.heliosapm.tsdblite.metric.AppMetricMXBean"));
	
	/** Placeholder metric */
	public final Metric PLACEHOLDER = new Metric();
	/** An empty tag map const */
	public static final SortedMap<String, String> EMPTY_TAG_MAP = Collections.unmodifiableSortedMap(new TreeMap<String, String>());
	

	
	
	/**
	 * <p>Title: MetricNVPFunnel</p>
	 * <p>Description: Funnel to compute hash codes for an unbuilt metric still in JSON form</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.metric.MetricCache.MetricNVPFunnel</code></p>
	 */
	private static enum MetricNVPFunnel implements Funnel<NVP<String, Map<String, String>>> {
	    /** The funnel instance */
	    INSTANCE;
			/**
			 * {@inheritDoc}
			 * @see com.google.common.hash.Funnel#funnel(java.lang.Object, com.google.common.hash.PrimitiveSink)
			 */
			@Override
			public void funnel(final NVP<String, Map<String, String>> metric, final PrimitiveSink into) {
		    	into.putString(metric.getKey().trim(), UTF8);
		    	final Map<String, String> map = metric.getValue();
		    	if(map!=null && !map.isEmpty()) {
		    		final TreeMap<String, String> tmap = new TreeMap<String, String>(TagKeySorter.INSTANCE);
		    		tmap.putAll(map);
			    	for(Map.Entry<String, String> entry: tmap.entrySet()) {
			    		into.putString(entry.getKey().trim(), UTF8)
			    		.putString(entry.getValue().trim(), UTF8);
			    	}
		    		
		    	}
			}
	  }
	
	
	/**
	 * <p>Title: MetricJSONFunnel</p>
	 * <p>Description: Funnel to compute hash codes for metrics while still in JSON form</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.metric.MetricCache.MetricJSONFunnel</code></p>
	 */
	public static enum MetricJSONFunnel implements Funnel<JsonNode> {
	    /** The funnel instance */
	    INSTANCE;
			/**
			 * {@inheritDoc}
			 * @see com.google.common.hash.Funnel#funnel(java.lang.Object, com.google.common.hash.PrimitiveSink)
			 */
			@Override
			public void funnel(final JsonNode metric, final PrimitiveSink into) {
				try {
					into.putString(metric.get("metric").textValue().trim(), UTF8);
					final JsonNode tags = metric.get("tags");
					if(tags!=null) {
						for(final Iterator<String> keyIter = tags.fieldNames(); keyIter.hasNext();) {
							final String key = keyIter.next();
							into.putString(key.trim(), UTF8);
							into.putString(tags.get(key).textValue().trim(), UTF8);
						}
					}
				} catch (Exception ex) {
					throw new RuntimeException("Failed to extract metricName/tags from JsonNode [" + metric + "]", ex);
				}
	    }
	  }
	
	/**
	 * Returns the long hash code for the passed metric JSON node
	 * @param metricNode a metric JSON node
	 * @return the long hash code
	 */
	public static long hashCode(final JsonNode metricNode) {
		return METRIC_NAME_HASHER.hashObject(metricNode, MetricJSONFunnel.INSTANCE).padToLong();
	}
	
	/**
	 * Returns the long hash code for a metric name built from the passed metric name and tags
	 * @param metricName The metric name
	 * @param tags The tags. These will be correctly sorted by the hasher.
	 * @return the long hash code
	 */
	public static long hashCode(final String metricName, final Map<String, String> tags) {
		return METRIC_NAME_HASHER.hashObject(new NVP<String, Map<String, String>>(metricName, tags), MetricNVPFunnel.INSTANCE).padToLong();
	}

	/**
	 * Acquires and returns the MetricCache singleton
	 * @return the MetricCache singleton
	 */
	public static MetricCache getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new MetricCache();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Acquires the metric for the passed metric name and tags, creating and caching it if required
	 * @param metricName The metric name
	 * @param tags The metric tags
	 * @return the metric
	 */
	public Metric getMetric(final String metricName, final Map<String, String> tags) {
		final long hashCode = hashCode(metricName, tags);
		AppMetric appMetric = metricCache.putIfAbsent(hashCode, AppMetric.PLACEHOLDER);
		if(appMetric==null || appMetric==AppMetric.PLACEHOLDER) {			
			appMetric = new AppMetric(new Metric(metricName, tags, hashCode));
			metricCache.replace(hashCode, appMetric);			
			JMXHelper.registerMBean(metricMBeanServer, appMetric.getMetricInstance().toObjectName(), appMetric);
		}
		return appMetric.getMetricInstance();		
	}
	
	/**
	 * Returns the metric MBeanServer
	 * @return the metric MBeanServer
	 */
	public MBeanServer getMetricServer() {
		return metricMBeanServer;
	}

	/**
	 * Acquires the metric for the passed json node, creating and caching it if required
	 * @param node The JSON node
	 * @return the metric
	 */
	public Metric getMetric(final JsonNode node) {
		try {
			final long hashCode = hashCode(node);
			AppMetric appMetric = metricCache.putIfAbsent(hashCode, AppMetric.PLACEHOLDER);
			if(appMetric==null || appMetric==AppMetric.PLACEHOLDER) {			
				appMetric = new AppMetric(new Metric(node, hashCode));
				metricCache.replace(hashCode, appMetric);			
				JMXHelper.registerMBean(metricMBeanServer, appMetric.getMetricInstance().toHostObjectName(), appMetric);
			}
			return appMetric.getMetricInstance();
		} catch (Exception ex) {
			badMetrics.increment();
			return null;
		}
	}
	
	private MetricCache() {
		 
		final String jmxDomain = ConfigurationHelper.getSystemThenEnvProperty(Constants.CONF_METRICS_MSERVER, Constants.DEFAULT_METRICS_MSERVER);
		if(!JMXHelper.getHeliosMBeanServer().getDefaultDomain().equals(jmxDomain)) {
			metricMBeanServer = JMXHelper.createMBeanServer(jmxDomain, true);
			final int port = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.CONF_METRICS_JMXMP_PORT, Constants.DEFAULT_METRICS_JMXMP_PORT);
			if(port > -1) {
				final String iface = ConfigurationHelper.getSystemThenEnvProperty(Constants.CONF_METRICS_JMXMP_IFACE, Constants.DEFAULT_METRICS_JMXMP_IFACE);
				final JMXServiceURL surl = JMXHelper.fireUpJMXMPServer(iface, port, metricMBeanServer);
				log.info("Metrics MBeanServer [{}] available at [{}]", jmxDomain, surl);
			}
		} else {
			metricMBeanServer = JMXHelper.getHeliosMBeanServer();
		}
		expiry = ConfigurationHelper.getLongSystemThenEnvProperty(Constants.CONF_METRIC_EXPIRY, Constants.DEFAULT_METRIC_EXPIRY);
		expiryPeriod = ConfigurationHelper.getLongSystemThenEnvProperty(Constants.CONF_METRIC_EXPIRY_PERIOD, Constants.DEFAULT_METRIC_EXPIRY_PERIOD);
		expiryThread = new Thread(new Runnable(){
			@Override
			public void run() {
				while(true) {
					
					SystemClock.sleep(expiryPeriod);
					final long now = System.currentTimeMillis();
					final ObjectName[] metricObjectNames = JMXHelper.query(JMXHelper.ALL_MBEANS_FILTER, metricBeanQuery);
					final Collection<Future<?>> taskFutures = new ArrayList<Future<?>>(metricObjectNames.length);					
					for(final ObjectName on : metricObjectNames) {
						taskFutures.add(expiryService.submit(new Runnable(){
							@Override
							public void run() {
								try {
									final Map<String, Object> attrMap = JMXHelper.getAttributes(on, EXPIRY_ATTRIBUTES);
									final long lastActivity = (Long)attrMap.get("LastSubmission");
									if(now - lastActivity > expiry) {
										metricMBeanServer.unregisterMBean(on);
										expiredMetrics.increment();
										final long hc = (Long)attrMap.get("MetricHashCode");
										metricCache.remove(hc);
									}
								} catch (Exception x) { /* No Op */ }
							}
						}));
					}
					final long dispatchElapsed = System.currentTimeMillis() - now;
					lastExpiryDispatchTime.set(dispatchElapsed);
					log.debug("Expiry Dispatch for [{}] Metrics Completed in [{}] ms.", metricObjectNames.length, dispatchElapsed);
					for(Future<?> f: taskFutures) {
						try { f.get(); } catch (Exception x) {/* No Op */}
					}
					final long expiryElapsed = System.currentTimeMillis() - now;
					lastExpiryTime.set(expiryElapsed);
					log.debug("Expiry Completed in [{}] ms.", expiryElapsed);
				}
			}
		}, "MetricExpiryThread");
		expiryThread.setDaemon(true);
		expiryThread.setPriority(Thread.MAX_PRIORITY);
		expiryThread.start();
		JMXHelper.registerMBean(this, OBJECT_NAME);
	}
	
	
	/**
	 * Submits a trace instance
	 * @param trace a trace instance
	 */
	public void submit(final Trace trace) {
		if(trace!=null) {
			final AppMetric appMetric = metricCache.get(trace.getHashCode());
			appMetric.submit(trace);
		}
	}
	
	/**
	 * Returns the AppMetric for the passed ObjectName
	 * @param on The metric name as an ObjectName
	 * @return the AppMetric or null if it was not found
	 */
	public AppMetric getAppMetric(final ObjectName on) {
		if(on==null) throw new IllegalArgumentException("The passed ObjectName was null");		
		return metricCache.get(Util.hashCode(on));
	}
	
	/**
	 * Returns a SubNotif for the passed metric name
	 * @param on the metric name as an ObjectName
	 * @return the SubNotif or null if the AppMetric was not found
	 */
	public SubNotif getSubNotif(final ObjectName on) {
		final AppMetric am = getAppMetric(on);
		if(am!=null) return am.getSubNotif();
		return null;
	}
	
	
	/**
	 * <p>Title: MetricMBean</p>
	 * <p>Description: JMX MBean interface for Metric instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.metric.MetricCache.MetricMBean</code></p>
	 */
	public interface MetricMBean {
		/**
		 * Returns the metric name
		 * @return the metric name
		 */
		public String getMetricName();
		
		/**
		 * Returns the metric tags
		 * @return the metric tags
		 */
		public SortedMap<String, String> getTags();
		
		/**
		 * Returns the metric tags
		 * @return the metric tags
		 */
		public String getTagStr();
		
		
		/**
		 * Returns the metric hash 
		 * @return the metric hash
		 */
		public long getHashCode();
		
	}
	
	/**
	 * <p>Title: Metric</p>
	 * <p>Description:Represents a unique metric name and set of tags </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.metric.MetricCache.Metric</code></p>
	 */
	public class Metric implements MetricMBean {
		/** The metric name */
		protected final String metricName;
		/** The metric tags */
		protected final SortedMap<String, String> tags;
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
		private Metric(final String metricName, final Map<String, String> tags, final long hashCode) {
			if(metricName==null || metricName.trim().isEmpty()) throw new IllegalArgumentException("The passed metric name was null or empty");
			this.metricName = metricName.trim();
			if(tags==null || tags.isEmpty()) {
				this.tags = EMPTY_TAG_MAP;
			} else {
				final TreeMap<String, String> tmp = new TreeMap<String, String>(TagKeySorter.INSTANCE);
				for(Map.Entry<String, String> entry: tags.entrySet()) {
					tmp.put(entry.getKey().trim(), entry.getValue().trim());
				}		
				this.tags = Collections.unmodifiableSortedMap(tmp);
			}
			this.hashCode = hashCode;
		}
		
		/**
		 * Creates a new Metric
		 * @param node The JSON node
		 * @param hashCode The long hash code
		 */
		private Metric(final JsonNode node, final long hashCode) {
			metricName = node.get("metric").textValue();
			final JsonNode tags = node.get("tags");
			if(tags!=null) {
				final TreeMap<String, String> tmp = new TreeMap<String, String>(TagKeySorter.INSTANCE);
				for(final Iterator<String> keyIter = tags.fieldNames(); keyIter.hasNext();) {
					final String key = keyIter.next();
					tmp.put(key.trim(), tags.get(key).textValue().trim());
				}
				this.tags = Collections.unmodifiableSortedMap(tmp);
			} else {
				this.tags = EMPTY_TAG_MAP;
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
		public SortedMap<String, String> getTags() {
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

	/**
	 * Returns the number of metrics in the metric cache
	 * @return the metric cache size
	 */
	@Override
	public long getMetricCacheSize() {
		return metricCache.size();
	}

	/**
	 * Returns the default domain of the metrics MBeanServer
	 * @return the metric MBeanServer default domain
	 */
	@Override
	public String getMetricMBeanServer() {
		return metricMBeanServer.getDefaultDomain();
	}
	
	/**
	 * Returns the MBeanServer where metrics are being registered
	 * @return the MBeanServer where metrics are being registered
	 */
	public MBeanServer getMetricMBeanServerInstance() {
		return metricMBeanServer;
	}
	
	/**
	 * Returns the number of registered MBeans in the metrics MBeanServer.
	 * This will be more than the number of metrics.
	 * @return the number of registered MBeans in the metrics MBeanServer.
	 */
	@Override
	public int getMetricMBeanServerMBeanCount() {
		return metricMBeanServer.getMBeanCount();
	}
	

	/**
	 * Returns the cummulative number of bad metric submissions
	 * @return the cummulative number of bad metric submissions
	 */
	@Override
	public long getBadMetrics() {
		return badMetrics.longValue();
	}

	/**
	 * Returns the cummulative number of expired metrics
	 * @return the cummulative number of expired metrics
	 */
	@Override
	public long getExpiredMetrics() {
		return expiredMetrics.longValue();
	}

	/**
	 * Returns the elapsed time of the last expiry dispatch in ms.
	 * @return the elapsed time of the last expiry dispatch in ms.
	 */
	@Override
	public long getLastExpiryDispatchTime() {
		return lastExpiryDispatchTime.get();
	}

	/**
	 * Returns the elapsed time of the last expiry completion in ms.
	 * @return the elapsed time of the last expiry completion in ms.
	 */
	@Override
	public long getLastExpiryTime() {
		return lastExpiryTime.get();
	}

	/**
	 * Returns the expiry period in ms.
	 * @return the expiry period in ms.
	 */
	@Override
	public long getExpiryPeriod() {
		return expiryPeriod;
	}

	/**
	 * Returns the metric expiry in ms.
	 * @return the metric expiry in ms.
	 */
	@Override
	public long getExpiry() {
		return expiry;
	}
	

}
