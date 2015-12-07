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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.heliosapm.tsdblite.Constants;
import com.heliosapm.tsdblite.json.JSON;
import com.heliosapm.tsdblite.metric.MetricCache.Metric;

/**
 * <p>Title: Trace</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.Trace</code></p>
 */

public class Trace implements Comparable<Trace>{
	/** The metric  */
	protected final Metric metric;
	/** The value type indicator */
	protected final boolean doubleType;
	/** The long value */
	protected final long longValue;
	/** The double value */
	protected final double doubleValue;
	/** The metric timestamp in ms. */
	protected final long timestampMs;
	

	/** I reckon any value higher than this number means ms. Anything lower is ms. */
	public static final long MAX_SECS_TIME = 315550800000L;
											 
	
	
	
	
	/** The UTF8 character set */
	public static final Charset UTF8 = Charset.forName("UTF8");
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final Trace t) {
		if(timestampMs == t.timestampMs) {
			final long me = getHashCode();
			final long you = t.getHashCode();
			return me <= you ? -1 : 1;
		}
		return timestampMs < t.timestampMs ? -1 : 1;
	}
	
	/**
	 * Creates a new Trace
	 * @param metricName The metric name (concated meric name segments)
	 * @param tags The metric tags
	 * @param doubleType true for a double type value, false for a long type double
	 * @param longValue The long value
	 * @param doubleValue The double value
	 * @param timestampMs The timestamp in ms.
	 */
	private Trace(final String metricName, final Map<String, String> tags, final boolean doubleType, final long longValue, final double doubleValue, final long timestampMs) {
		this.metric = MetricCache.getInstance().getMetric(metricName, tags);
		this.doubleType = doubleType;
		this.longValue = longValue;
		this.doubleValue = doubleValue;
		this.timestampMs = toMs(timestampMs);
	}
	
	/**
	 * Creates a new Trace
	 * @param metric The metric
	 * @param doubleType true for a double type value, false for a long type double
	 * @param longValue The long value
	 * @param doubleValue The double value
	 * @param timestampMs The timestamp in ms.
	 */
	public Trace(final Metric metric, final boolean doubleType, final long longValue, final double doubleValue, final long timestampMs) {
		this.metric = metric;
		this.doubleType = doubleType;
		this.longValue = longValue;
		this.doubleValue = doubleValue;
		this.timestampMs = toMs(timestampMs);
	}
	
	/**
	 * Returns the passed long timestamp as millis, converting if necessary if the value seems like it might be in seconds.
	 * @param value The timestamp
	 * @return the timestamp in ms.
	 */
	public static long toMs(final long value) {
		return value <= MAX_SECS_TIME ? TimeUnit.MILLISECONDS.convert(value, TimeUnit.SECONDS) : value;
	}
	
	/**
	 * Creates a new long value Trace
	 * @param metricName The metric name (concated meric name segments)
	 * @param tags The metric tags
	 * @param longValue The long value
	 * @param timestampMs The timestamp in ms.
	 */
	public Trace(final String metricName, final Map<String, String> tags, final long longValue, final long timestampMs) {
		this(metricName, tags, false, longValue, -1d, timestampMs);
	}
	
	/**
	 * Returns the trace value
	 * @return the trace value
	 */
	public Number getValue() {
		return doubleType ? doubleValue : longValue;
	}
	
	/**
	 * Creates a new double value Trace
	 * @param metricName The metric name (concated meric name segments)
	 * @param tags The metric tags
	 * @param doubleValue The double value
	 * @param timestampMs The timestamp in ms.
	 */
	public Trace(final String metricName, final Map<String, String> tags, final double doubleValue, final long timestampMs) {
		this(metricName, tags, true, -1L, doubleValue, timestampMs);
	}
	
	@SuppressWarnings("javadoc")
	public static void main(String[] args) {
		log("Trace Test");
		Map<String, String> tags = new HashMap<String, String>(4);
		tags.put("host", "localhost");
		tags.put("app", "test");
		tags.put("cpu", "" + 1);
		tags.put("type", "combined");
		final Trace trace = new Trace("sys.cpu", tags, false, 34, -1, System.currentTimeMillis());
		log("toString: " + trace);
		String json = JSON.serializeToString(trace);
		log("JSON: " + json);
		final Trace t = JSON.parseToObject(json, Trace.class);
		log("fromJson: " + t);
		log("=====================================");
		final Trace[] traces = new Trace[Constants.CORES];
		final Random r = new Random(System.currentTimeMillis());
		for(int i = 0; i < Constants.CORES; i++) {
			tags = new HashMap<String, String>(4);
			tags.put("host", "localhost");
			tags.put("app", "test");
			tags.put("cpu", "" + i);
			tags.put("type", "combined");
			traces[i] = new Trace("sys.cpu", tags, false, Math.abs(r.nextInt(100)), -1, System.currentTimeMillis());
			log("toString:" + traces[i]);			
		}
		json = JSON.serializeToString(traces);
		log("JSON: " + json);
		Trace[] ts = JSON.parseToObject(json, Trace[].class);
		for(Trace x: ts) {
			log("fromJson: " + x);
		}
	}
	
	@SuppressWarnings("javadoc")
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
//	/**
//	 * Creates a new Trace from the state of a tracer
//	 * @param tracer the tracer to read the state from
//	 */
//	public Trace(final Tracer tracer) {
//		this.metricName = tracer.buildMetricName();
//		this.tags = new TreeMap<String, String>(TagKeySorter.INSTANCE);
//		this.tags.putAll(tracer.getTags());
//		this.doubleType = tracer.isDouble();
//		this.longValue = tracer.getLongValue();
//		this.doubleValue = tracer.getDoubleValue();
//		long ts = tracer.getMsTime();
//		this.timestampMs = ts==-1L ? System.currentTimeMillis() : ts;
//		tagKeyStack.addAll(tracer.getTagKeyStack());
//	}
	


	/**
	 * Returns the metric name
	 * @return the metricName
	 */
	public String getMetricName() {
		return metric.getMetricName();
	}

	/**
	 * Returns the metric tags
	 * @return the tags
	 */
	public Map<String, String> getTags() {
		return metric.getTags();
	}

	/**
	 * Returns the value type indicator
	 * @return true for a double type, false for a long type
	 */
	public boolean isDoubleType() {
		return doubleType;
	}

	/**
	 * Returns the long value
	 * @return the long value
	 */
	public long getLongValue() {
		return longValue;
	}

	/**
	 * Returns the double value
	 * @return the double value
	 */
	public double getDoubleValue() {
		return doubleValue;
	}

	/**
	 * Returns the metric timestamp
	 * @return the timestampMs
	 */
	public long getTimestampMs() {		
		return timestampMs;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder("Trace: [");
		b.append(metric.getMetricName()).append(":").append("{");
		for(Map.Entry<String, String> entry: metric.getTags().entrySet()) {
			b.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
		}
		b.deleteCharAt(b.length()-1).append("}")
		.append(" ts:").append(new Date(timestampMs))
		.append(" value:");
		if(doubleType) b.append(doubleValue);
		else b.append(longValue);
		return b.append("]").toString();
	}
	
	/**
	 * Returns the long hash code for this trace's fully qualified metric name
	 * @return the long hash code
	 */
	public long getHashCode() {
		return metric.getHashCode();
	}

	
	/** Nasty hack to inject whether or not we want times to be reported in UNIX time or ms. */
	public static final ThreadLocal<Boolean> traceInSeconds = new ThreadLocal<Boolean>();
	
	/**
	 * <p>Title: TraceArraySerializer</p>
	 * <p>Description: Jackson JSON serializer for Trace array instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.nash.tracing.Trace.TraceArraySerializer</code></p>
	 */
	public static class TraceArraySerializer extends JsonSerializer<Trace[]> {
		private static final TraceSerializer TS = new TraceSerializer();
		/**
		 * {@inheritDoc}
		 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(final Trace[] traces, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonProcessingException {
			try {
				jgen.writeStartArray();
				if(traces!=null && traces.length > 0) {
					for(int i = 0; i < traces.length; i++) {
						final Trace t = traces[i];
						if(t!=null) {
							TS.serialize(t, jgen, provider);
						}
					}
				}
			} finally {
				jgen.writeEndArray();
				traceInSeconds.remove();
			}			
		}
	}
	
	/**
	 * <p>Title: TraceSerializer</p>
	 * <p>Description: Jackson JSON serializer for Trace instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.metric.Trace.TraceSerializer</code></p>
	 */
	public static class TraceSerializer extends JsonSerializer<Trace> {		
		/**
		 * {@inheritDoc}
		 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(final Trace trace, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeFieldName("metric");			
			jgen.writeString(trace.getMetricName());
			
			jgen.writeFieldName("timestamp");			
			jgen.writeNumber(trace.timestampMs);
			
			jgen.writeFieldName("value");			
			jgen.writeNumber(trace.doubleType ? trace.doubleValue : trace.longValue);
			jgen.writeObjectFieldStart("tags");
			for(Map.Entry<String, String> entry: trace.getTags().entrySet()) {
				jgen.writeFieldName(entry.getKey());
				jgen.writeString(entry.getValue());
			}
			jgen.writeEndObject();
			jgen.writeEndObject();
			
		}
	}
	
	/**
	 * <p>Title: TraceDeserializer</p>
	 * <p>Description: Jackson JSON deserializer for Trace instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.metric.Trace.TraceDeserializer</code></p>
	 */
	public static class TraceDeserializer extends JsonDeserializer<Trace> {
		/**
		 * {@inheritDoc}
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public Trace deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final JsonNode node = p.getCodec().readTree(p);			
			final Metric metric = MetricCache.getInstance().getMetric(node);			
			final Number v = node.get("value").numberValue();
			final boolean isDouble = (v instanceof Double);
			return new Trace(metric, isDouble, v.longValue(), v.doubleValue(), node.get("timestamp").longValue());
		}
	}
	
	/**
	 * <p>Title: TraceArrayDeserializer</p>
	 * <p>Description: Jackson JSON deserializer for Trace array instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.metric.TraceArrayDeserializer</code></p>
	 */
	public static class TraceArrayDeserializer extends JsonDeserializer<Trace[]> {
		/**
		 * {@inheritDoc}
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public Trace[] deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final ArrayNode node = p.getCodec().readTree(p);
			final int size = node.size();
			final Set<Trace> traces = new LinkedHashSet<Trace>(size);
			for(int i = 0; i < size; i++) {
				traces.add(JSON.parseToObject(node.get(i), Trace.class));
			}
			return traces.toArray(new Trace[size]);
		}
	}
	
	
}
