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
package com.heliosapm.tsdblite.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import com.heliosapm.tsdblite.json.JSON;
import com.heliosapm.tsdblite.json.JSONException;
import com.heliosapm.tsdblite.metric.MetricCache;
import com.heliosapm.tsdblite.metric.Trace;
import com.heliosapm.utils.time.SystemClock;
import com.heliosapm.utils.time.SystemClock.ElapsedTime;


/**
 * <p>Title: SubmitTracesHandler</p>
 * <p>Description: Accepts JSON trace submissions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.SubmitTracesHandler</code></p>
 */

public class SubmitTracesHandler extends HttpRequestHandler {
	
	/** The endpoint where metrics are submitted to */
	final MetricCache metricCache;

	/**
	 * Creates a new SubmitTracesHandler
	 */
	public SubmitTracesHandler() {
		super();
		metricCache = MetricCache.getInstance();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.handlers.HttpRequestHandler#process(com.heliosapm.tsdblite.handlers.TSDBHttpRequest)
	 */
	@Override
	protected void process(final TSDBHttpRequest request) {
		log.debug("Processing [{}]", request.getRequest());		
		if(!request.hasContent()) {
			request.send400("No content sent for route [", request.getRoute(), "]");
			return;
		}
		final ByteBuf content = request.getContent();
		final Trace[] traces;
		try {
			if(content.getByte(0)=='{') {
				traces = new Trace[]{JSON.parseToObject(content, Trace.class)};
			} else {
				traces = JSON.parseToObject(content, Trace[].class);
			}
		} catch (JSONException jex) {
			log.error("Failed to parse JSON payload", jex);
			request.send400("Invalid JSON payload for route [", request.getRoute(), "]:", jex.toString());
			return;
		}
		final ElapsedTime et = SystemClock.startClock();
		for(Trace trace: traces) {
			//log.debug("TRACE: {}", trace);
			metricCache.submit(trace);
		}		
		request.send204().addListener(new GenericFutureListener<Future<? super Void>>() {
			public void operationComplete(final Future<? super Void> f) throws Exception {
				if(f.isSuccess()) {
					log.info("Traces Processed: {}", et.printAvg("traces", traces.length));
				} else {
					log.error("Traces failed", f.cause());
				}
			};
		});		
	}

	

}
