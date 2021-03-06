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
package com.heliosapm.tsdblite.handlers.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.heliosapm.tsdblite.handlers.json.SplitMetaInputHandler;
import com.heliosapm.tsdblite.handlers.json.SplitTraceInputHandler;
import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: HttpRequestManager</p>
 * <p>Description: Accepts and routes all http requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.HttpRequestManager</code></p>
 */
@Sharable
public class HttpRequestManager extends SimpleChannelInboundHandler<HttpRequest> {
	/** The singleton instance */
	private static volatile HttpRequestManager instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	protected final SplitTraceInputHandler traceHandler = new SplitTraceInputHandler();
	protected final SplitMetaInputHandler metaHandler = new SplitMetaInputHandler();
	protected final HttpToJsonAdapterHandler jsonAdapter = new HttpToJsonAdapterHandler();
	protected final LoggingHandler loggingHandler = new LoggingHandler(getClass(), LogLevel.INFO);
	
	/** A map of HTTP request handlers keyed by thye uri */
	protected final Map<String, HttpRequestHandler> requestHandlers = new HashMap<String, HttpRequestHandler>();
	
	protected final ByteBuf favicon;
	protected final int favSize;
	
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
	
	private static final AttributeKey<String> SWITCHED = AttributeKey.newInstance("swiched"); 

	/**
	 * Creates a new HttpRequestManager
	 */
	private HttpRequestManager() {
		favicon = Unpooled.unmodifiableBuffer(Unpooled.copiedBuffer(URLHelper.getBytesFromURL(getClass().getClassLoader().getResource("www/favicon.ico"))));
		favSize = favicon.readableBytes();
		log.info("Loaded favicon: [{}] Bytes", favSize);
		requestHandlers.put("/api/put", new SubmitTracesHandler());		
		requestHandlers.put("/api/s", HttpStaticFileServerHandler.getInstance());
	}
	
	static final Charset UTF8 = Charset.forName("UTF8");
	
	/**
	 * {@inheritDoc}
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final HttpRequest msg) throws Exception {
		try {
			final String uri = msg.uri();
			final Channel channel = ctx.channel();
			if(uri.endsWith("/favicon.ico")) {
				final DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, favicon);
				resp.headers().set(HttpHeaders.CONTENT_TYPE, "image/x-icon");
				resp.headers().setInt(HttpHeaders.CONTENT_LENGTH, favSize);				
				ctx.writeAndFlush(resp);
				return;
			} else if(uri.equals("/api/put") || uri.equals("/api/metadata")) {				
				final ChannelPipeline p = ctx.pipeline();
//				p.addLast(loggingHandler, jsonAdapter, new JsonObjectDecoder(true), traceHandler);
				p.addLast(jsonAdapter, new JsonObjectDecoder(true), traceHandler);
//				if(msg instanceof FullHttpRequest) {
//					ByteBuf b = ((FullHttpRequest)msg).content().copy();
//					ctx.fireChannelRead(b);
//				}
				ctx.fireChannelRead(msg);
				p.remove("requestManager");
				log.info("Switched to JSON Trace Processor");
				return;
			}
			final TSDBHttpRequest r = new TSDBHttpRequest(msg, ctx.channel(), ctx);
			final HttpRequestHandler handler = requestHandlers.get(r.getRoute());
			if(handler==null) {
				r.send404().addListener(new GenericFutureListener<Future<? super Void>>() {
					public void operationComplete(Future<? super Void> f) throws Exception {
						log.info("404 Not Found for {} Complete: success: {}", r.getRoute(), f.isSuccess());
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
	
    /**
     * {@inheritDoc}
     * @see io.netty.channel.ChannelHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)
     */
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable t) throws Exception {    	
    	log.error("Uncaught exception", t);
    	//super.exceptionCaught(ctx, t);
    }

	

}
