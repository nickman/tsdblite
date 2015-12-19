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
package com.heliosapm.tsdblite.handlers.json;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heliosapm.tsdblite.json.JSON;
import com.heliosapm.tsdblite.metric.MetricCache;
import com.heliosapm.tsdblite.metric.Trace;
import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: SplitTraceInputHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.json.SplitTraceInputHandler</code></p>
 */
@Sharable
public class SplitTraceInputHandler extends SimpleChannelInboundHandler<ByteBuf> {
	
	/** A UTF8 character set */
	protected static final Charset UTF8 = Charset.forName("UTF8");
	
	/** The endpoint where metrics are submitted to */
	protected final MetricCache metricCache;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	
	/**
	 * Creates a new SplitTraceInputHandler
	 */
	public SplitTraceInputHandler() {
		this.metricCache = MetricCache.getInstance();
	}

	
	
	@Override
	protected void messageReceived(final ChannelHandlerContext ctx, final ByteBuf msg) throws Exception {
		if(log.isTraceEnabled()) {
			log.trace(msg.toString(UTF8));
		}		
		//log.info(msg.toString(UTF8));
		final JsonNode node = JSON.parseToNode(msg, UTF8);
		
		if(node.has("metric")) {
			if(node.has("timestamp")) {
				metricCache.submit(
						JSON.parseToObject(node, Trace.class)
				);				
			}
		} else if(node.has("Metric")) {			
//			final ObjectName on = metaObjectName(node);
//			final String name = metaObject(node);
//			final Map<String, String> metaNvps = JSON.from(node);
			System.out.println(JSON.serializeToString(node));
//			log.info("META: [{}] : [{}]", name, metaNvps);
//			if(on!=null) {			
//				metricCache.submit(on, metaNvps.get("Name"), metaNvps.get("Value"));
//			}
		}
	}
	
	protected ObjectName metaObjectName(final JsonNode node) {
		try {
			final String metric = node.get("Metric").textValue();
			final ObjectNode tags = (ObjectNode)node.get("Tags");
			if(tags!=null && tags.size()>0) {
				Map<String, String> t = new HashMap<String, String>(tags.size());
				for(Iterator<String> n = tags.fieldNames(); n.hasNext();) {
					final String key = n.next();
					final String value = tags.get(key).asText();
					t.put(key, value);
				}
				return toHostObjectName(JMXHelper.objectName(metric, t));
//				return JMXHelper.objectName(metric, t);
			} else {
				return JMXHelper.objectName(toHostObjectName(JMXHelper.objectName(metric + ":*")).toString());				
//				return JMXHelper.objectName(metric + ":type=static");
			}
		} catch (Exception ex) {
			return null;
		}
	}
	
	protected String metaObject(final JsonNode node) {
		try {
			final String metric = node.get("Metric").textValue();
			final ObjectNode tags = (ObjectNode)node.get("Tags");
			if(tags!=null && tags.size()>0) {
				Map<String, String> t = new HashMap<String, String>(tags.size());
				for(Iterator<String> n = tags.fieldNames(); n.hasNext();) {
					final String key = n.next();
					final String value = tags.get(key).asText();
					t.put(key, value);
				}
				return metric + ":" + t;
//				return JMXHelper.objectName(metric, t);
			} else {
				return metric + ":*";
//				return JMXHelper.objectName(toHostObjectName(JMXHelper.objectName(metric + ":*")).toString());				
//				return JMXHelper.objectName(metric + ":type=static");
			}
		} catch (Exception ex) {
			return null;
		}
	}
	
	public ObjectName toHostObjectName(final ObjectName on) {
		final String s = toHostObjectNameName(on);
		return JMXHelper.objectName(s);
	}
	
	public String toHostObjectNameName(final ObjectName on) {
		final StringBuilder b = new StringBuilder("meta.");
		TreeMap<String, String> tgs = new TreeMap<String, String>(on.getKeyPropertyList());
		final String metricName = on.getDomain();
		String h = tgs.remove("host");
		String a = tgs.remove("app");
		final String host = h; //==null ? "*" : h;
		final String app = a==null ? "ANYAPP" : a;
		final int segIndex = metricName.indexOf('.');			
		final String seg = segIndex==-1 ? metricName : metricName.substring(0, segIndex);
		if(host!=null) {
			b.append(host).append(".").append(seg).append(":");
		}
		if(app!=null) {
			tgs.put("app", app);
//			b.append(app).append(".").append(seg).append(":");
		}
		
//		if(segIndex!=-1) {
//			tgs.put("app", metricName.substring(segIndex+1));
//		}
		for(Map.Entry<String, String> entry: tgs.entrySet()) {
			b.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
		}
		b.deleteCharAt(b.length()-1);
		return b.toString();
	}	
	

}
