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

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

import com.heliosapm.tsdblite.json.JSON;
import com.heliosapm.tsdblite.metric.Trace;


/**
 * <p>Title: SubmitTracesHandler</p>
 * <p>Description: Accepts JSON trace submissions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.SubmitTracesHandler</code></p>
 */

public class SubmitTracesHandler extends HttpRequestHandler {

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.handlers.HttpRequestHandler#process(com.heliosapm.tsdblite.handlers.TSDBHttpRequest)
	 */
	@Override
	protected void process(final TSDBHttpRequest request) {
		final Channel channel = request.getChannel();
		final FullHttpRequest req = request.getRequest();		
		final Trace[] traces = JSON.parseToObject(req.content(), Trace[].class);
		for(Trace trace: traces) {
			log.info("TRACE: {}", trace);
		}
	}

	

}
