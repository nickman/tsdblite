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
import io.netty.handler.codec.http.FullHttpRequest;

import com.heliosapm.tsdblite.json.JSON;
import com.heliosapm.tsdblite.json.JSONException;
import com.heliosapm.tsdblite.metric.MetricCache;
import com.heliosapm.tsdblite.metric.Trace;


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
		log.info("Processing [{}]", request.getRequest());
		final FullHttpRequest req = request.getRequest();	
		if(!request.hasContent()) {
			request.send400("No content sent for route [", request.getRoute(), "]");
			return;
		}
		final ByteBuf content = req.content();
		final Trace[] traces;
		try {
			if(content.getByte(0)=='{') {
				traces = new Trace[]{JSON.parseToObject(req.content(), Trace.class)};
			} else {
				traces = JSON.parseToObject(req.content(), Trace[].class);
			}
		} catch (JSONException jex) {
			request.send400("Invalid JSON payload for route [", request.getRoute(), "]:", jex.toString());
			return;
		}
		
		for(Trace trace: traces) {
			log.debug("TRACE: {}", trace);
			metricCache.submit(trace);
		}
		request.send204();		
	}

	

}
