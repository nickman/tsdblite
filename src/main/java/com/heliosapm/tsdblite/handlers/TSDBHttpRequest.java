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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.regex.Pattern;

/**
 * <p>Title: TSDBHttpRequest</p>
 * <p>Description: Wraps up a channel and the http request it is carrying</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.TSDBHttpRequest</code></p>
 */

public class TSDBHttpRequest {
	/** The incoming HTTP request */
	protected final FullHttpRequest request;
	/** The channel the request came in on */
	protected final Channel channel;
	/** The request path */
	protected final String path; 
	/** The routing key */
	protected final String route;
	
	/** The path splitter regex */
	public static final Pattern PATH_SPLIT = Pattern.compile("/|\\?");
	
	/**
	 * Creates a new TSDBHttpRequest
	 * @param request The incoming HTTP request
	 * @param channel The channel the request came in on
	 */
	protected TSDBHttpRequest(final FullHttpRequest request, final Channel channel) {
		this.request = request;
		this.channel = channel;
		final QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
		path = decoder.path();
		final StringBuilder b = new StringBuilder();
		final String[] parts = PATH_SPLIT.split(path);
		for(String part: parts) {
			if(part==null || part.trim().isEmpty()) continue;
			b.append("/").append(part);
		}
		route = b.toString();
		
	}

	/**
	 * Returns the HTTP request
	 * @return the HTTP request
	 */
	public FullHttpRequest getRequest() {
		return request;
	}

	/**
	 * Returns the channel
	 * @return the channel
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * Returns 
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns 
	 * @return the route
	 */
	public String getRoute() {
		return route;
	}
	
	

	
	
}
