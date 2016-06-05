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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
import com.heliosapm.tsdblite.Constants;
import com.heliosapm.tsdblite.jmx.ManagedDefaultExecutorServiceFactory;
import com.heliosapm.tsdblite.jmx.Util;
import com.heliosapm.tsdblite.metric.AppMetric.SubNotif;
import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.lang.StringHelper;
import com.heliosapm.utils.time.SystemClock;

import jsr166e.LongAdder;
import net.openhft.hashing.LongHashFunction;

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
	/** The hasher to compute long hash codes for metric names */
	public static final LongHashFunction METRIC_HASHER = LongHashFunction.murmur_3();

	
	/** A counter for bad metric submissions */
	private final LongAdder badMetrics = new LongAdder();
	/** A counter for expired metrics */
	private final LongAdder expiredMetrics = new LongAdder();
	/** The last expiry dispatch elapsed time in ms */
	private final AtomicLong lastExpiryDispatchTime = new AtomicLong();
	/** The last expiry completion elapsed time in ms */
	private final AtomicLong lastExpiryTime = new AtomicLong();
	
	/** Registered metas */
	private final NonBlockingHashMapLong<MetricMeta> metas = new NonBlockingHashMapLong<MetricMeta>(1024, false); 
	
	
	/** The expiry period on which expired metrics are scanned for in ms. */
	final long expiryPeriod;
	/** The maximum elapsed time an inactive metric can live for  in ms. */
	final long expiry;
	
	/** The attribute names the expiry inquiry will request from each metric */
	private static final String[] EXPIRY_ATTRIBUTES = new String[]{"MetricHashCode", "LastActivity"}; // "LastSubmission"
	
	/** The configuration key for the maximum number of tags per IMetricName */
	public static final String CONF_MAX_TAGS = "tracing.tags.max";
	/** The default maximum number of tags per IMetricName */
	public static final int DEFAULT_MAX_TAGS = 8;
	
	/** The configuration key for the minimum number of tags per IMetricName */
	public static final String CONF_MIN_TAGS = "tracing.tags.min";
	/** The default minimum number of tags per IMetricName */
	public static final int DEFAULT_MIN_TAGS = 1;
	
	
	/** The maximum number of tags per IMetricName */
	public static final int MAX_TAGS;
	/** The minimum number of tags per IMetricName */
	public static final int MIN_TAGS;
	
	/** Consistent sorting */
	public static final Collator ROOT_COLLATOR = Collator.getInstance(Locale.ROOT);
	
	
	static {
		MAX_TAGS = ConfigurationHelper.getIntSystemThenEnvProperty(CONF_MAX_TAGS, DEFAULT_MAX_TAGS);
		MIN_TAGS = ConfigurationHelper.getIntSystemThenEnvProperty(CONF_MIN_TAGS, DEFAULT_MIN_TAGS);
	}
	

	
	/** The expiry task pool */
	private final ExecutorService expiryService = new ManagedDefaultExecutorServiceFactory("expiry").newExecutorService(4);
	/** The expiry task scheduler thread */
	private final Thread expiryThread;
	/** The JMX query to find all registered instances of {@link com.heliosapm.tsdblite.metric.AppMetricMXBean} */
	private final QueryExp metricBeanQuery = Query.isInstanceOf(new StringValueExp("com.heliosapm.tsdblite.metric.AppMetricMXBean"));
	
	/** An empty tag map const */
	public static final SortedMap<String, String> EMPTY_TAG_MAP = Collections.unmodifiableSortedMap(new TreeMap<String, String>());
	

	
	
	
	/**
	 * Returns the long hash code for the passed metric JSON node
	 * @param metricNode a metric JSON node
	 * @return the long hash code
	 */
	public static long hashCode(final JsonNode metricNode) {
		if(metricNode==null) throw new IllegalArgumentException("The passed node was null");
		try {
			final TreeMap<String, String> cleanedTags = new TreeMap<String, String>(ROOT_COLLATOR);
			final JsonNode tags = metricNode.get("tags");
			for(final Iterator<String> keyIter = tags.fieldNames(); keyIter.hasNext();) {
				final String key = keyIter.next();
				cleanedTags.put(key, tags.get(key).textValue());
			}
			return hashCode(metricNode.get("metric").textValue(), cleanedTags);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to calc hash for metric node [" + metricNode + "]", ex);
		}
	}
	
	/**
	 * Returns the long hash code for a metric name built from the passed metric name and tags
	 * @param metricName The metric name
	 * @param tags The tags. These will be correctly sorted by the hasher.
	 * @return the long hash code
	 */
	public static long hashCode(final String metricName, final Map<String, String> tags) {		
		return METRIC_HASHER.hashChars(
			StringHelper.getStringBuilder()
				.append(clean(tags))
				.append(clean(metricName, "metric name")
			)
		);
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
	 * Validates and cleans the tag keys and values in the passed map, updating the cummulative byte count in the passed arr.
	 * @param tags The tag map to clean
	 * @return the cleaned map
	 */
	public static SortedMap<String, String> clean(final Map<String, String> tags) {
		if(tags==null) throw new IllegalArgumentException("The passed tags map was empty");
		final int size = tags.size();
		if(size < MIN_TAGS) throw new IllegalArgumentException("The passed tags map had [" + size + "] tags, but the minimum is [" + MIN_TAGS + "]");
		if(size > MAX_TAGS) throw new IllegalArgumentException("The passed tags map had [" + size + "] tags, but the maximum is [" + MAX_TAGS + "]");
		final TreeMap<String, String> cleanedTags = new TreeMap<String, String>(ROOT_COLLATOR);
		for(Map.Entry<String, String> entry: tags.entrySet()) {
			clean(cleanedTags, entry.getKey(), entry.getValue());
		}
		return cleanedTags;
	}
	
	private static void clean(final Map<String, String> putTarget, final String key, final String value) {
		if(putTarget.put(clean(key, "Tag Key"), clean(value, "Tag Value"))!=null) {
			throw new RuntimeException("Duplicate key during clean: key[" + key + "], cleanedKey:[" + clean(key, "Tag Key") + "]");
		}
	}
	/**
	 * Standard tag key, tag value and metric name string cleaner
	 * @param s The string to clean
	 * @param field The name of the field being cleaned for exception reporting
	 * @return the cleaned string
	 */
	public static String clean(final String s, final String field) {
		if(s==null || s.trim().isEmpty()) throw new IllegalArgumentException("The passed " + field + " was null or empty");
		return s.trim().toLowerCase().replace(':', ';');
	}	

	/**
	 * Standard tag key, tag value and metric name string cleaner
	 * @param s The string to clean
	 * @return the cleaned string
	 */
	public static String clean(final String s) {
		return clean(s, "value");
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
				final ObjectName on = appMetric.getMetricInstance().toHostObjectName();
				//FIXME:  make this config
//				JMXHelper.registerMBean(metricMBeanServer, on, appMetric);
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
					final AtomicInteger expiredMetricCount = new AtomicInteger(0);
					SystemClock.sleep(expiryPeriod);
					final long startTime = System.currentTimeMillis();
					final ObjectName[] metricObjectNames = JMXHelper.query(JMXHelper.ALL_MBEANS_FILTER, metricBeanQuery);					
					final Collection<Future<?>> taskFutures = new ArrayList<Future<?>>(metricObjectNames.length);					
					for(final ObjectName on : metricObjectNames) {
						taskFutures.add(expiryService.submit(new Runnable(){
							@Override
							public void run() {
								try {
									final long now = System.currentTimeMillis();
									final Map<String, Object> attrMap = JMXHelper.getAttributes(on, EXPIRY_ATTRIBUTES);
									if(!attrMap.containsKey("LastActivity")) {
										log.warn("No LA for [{}], AttrMap: {}", on, attrMap);
										return;
									}
									final long lastActivity = (Long)attrMap.get("LastActivity");
									final long age = now - lastActivity; 
									if(age > expiry) {
										expiredMetricCount.incrementAndGet();
										metricMBeanServer.unregisterMBean(on);
										expiredMetrics.increment();
										final long hc = (Long)attrMap.get("MetricHashCode");
										metricCache.remove(hc);										
									}
								} catch (Exception x) { 
									log.error("Expiry Task Failure", x);
								}
							}
						}));
					}
					final long dispatchElapsed = System.currentTimeMillis() - startTime;
					lastExpiryDispatchTime.set(dispatchElapsed);
					int fails = 0;
					for(Future<?> f: taskFutures) {
						try { f.get(); } catch (Exception x) {fails++;}
					}
					final long expiryElapsed = System.currentTimeMillis() - startTime;
					final int exp = expiredMetricCount.get();
					if(exp != 0) {
						log.info("Expiry Dispatch for [{}] Metrics Completed in [{}] ms. Expired [{}] metrics.", metricObjectNames.length, dispatchElapsed, exp);
					}
					
					lastExpiryTime.set(expiryElapsed);
					if(log.isDebugEnabled()) log.debug("Expiry Completed in [{}] ms. Tasks: {}, Fails: {}", expiryElapsed, taskFutures.size(), fails);
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
			log.info("Trace: {}", trace);
//			final Map<String, String> p = metaPairs.get(appMetric.getMetricHashCode());
//			if(p!=null) {
//				log.info("AppMetric matched Meta: {}", p);
//			}
		}
	}
	
	
	/**
	 * Registers a new meta-source ObjectName
	 * @param meta The meta ObjectName
	 * @param name The meta key
	 * @param value The meta value
	 */
	public void submit(final ObjectName meta, final String name, final String value) {
		if(meta!=null) {
			final long hashCode = hashCode(meta.getDomain(), meta.getKeyPropertyList());
			MetricMeta m = metas.putIfAbsent(hashCode, MetricMeta.PLACEHOLDER);
			if(m==null) {
				m = new MetricMeta(hashCode, name, value);
				JMXHelper.registerMBean(metricMBeanServer, meta, m);
				metas.replace(hashCode, m);
			} else {
				m.addPair(name, value);
			}
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
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.metric.MetricCacheMXBean#getMetasCount()
	 */
	@Override
	public int getMetasCount() {
		return metas.size();
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
