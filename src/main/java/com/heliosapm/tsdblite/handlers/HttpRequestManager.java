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

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: HttpRequestManager</p>
 * <p>Description: Accepts and routes all http requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.HttpRequestManager</code></p>
 */
@Sharable
public class HttpRequestManager extends SimpleChannelInboundHandler<FullHttpRequest> {
	/** The singleton instance */
	private static volatile HttpRequestManager instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** A map of HTTP request handlers keyed by thye uri */
	protected final Map<String, HttpRequestHandler> requestHandlers = new HashMap<String, HttpRequestHandler>();
	
	/**
	 * Acquires and returns the HttpRequestManager singleton
	 * @return the HttpRequestManager singleton
	 */
	public static HttpRequestManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new HttpRequestManager();
				}
			}
		}
		return instance;
	}
	

	/**
	 * Creates a new HttpRequestManager
	 */
	private HttpRequestManager() {
		requestHandlers.put("/api/put", new SubmitTracesHandler());
	}
	
	/**
	 * {@inheritDoc}
	 * @see io.netty.channel.SimpleChannelInboundHandler#messageReceived(io.netty.channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void messageReceived(final ChannelHandlerContext ctx, final FullHttpRequest msg) throws Exception {
		try {
			final TSDBHttpRequest r = new TSDBHttpRequest(msg, ctx.channel(), ctx);
			final HttpRequestHandler handler = requestHandlers.get(r.getRoute());
			if(handler==null) {
				r.send404().addListener(new GenericFutureListener<Future<? super Void>>() {
					public void operationComplete(Future<? super Void> f) throws Exception {
						log.info("404 Complete: success: {}", f.isSuccess());
						if(!f.isSuccess()) {
							log.error("Error sending 404", f.cause());
						}
					};
				});
				return;
			}
			handler.process(r);			
		} catch (Exception ex) {
			log.error("HttpRequest Routing Error", ex);
		}
	}
	

}
