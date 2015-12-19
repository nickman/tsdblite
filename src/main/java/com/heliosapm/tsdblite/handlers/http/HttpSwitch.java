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
package com.heliosapm.tsdblite.handlers.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutorGroup;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.heliosapm.tsdblite.handlers.json.SplitTraceInputHandler;
import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: HttpSwitch</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.http.HttpSwitch</code></p>
 */
@Sharable
public class HttpSwitch extends MessageToMessageDecoder<HttpRequest> {
	final EventExecutorGroup eventExecutorGroup;
	final HttpToJsonAdapterHandler httpToJson = new HttpToJsonAdapterHandler();
	final SplitTraceInputHandler traceHandler = new SplitTraceInputHandler();
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	protected final ByteBuf favicon;
	protected final int favSize;

	
	protected final LoggingHandler loggingHandler = new LoggingHandler(getClass(), LogLevel.INFO);

	
	final Set<String> pureJsonEndPoints = new HashSet<String>(Arrays.asList(
		"put", "metadata/put"	
	));
	
	/**
	 * Creates a new HttpSwitch
	 */
	public HttpSwitch(final EventExecutorGroup eventExecutorGroup) {
		this.eventExecutorGroup = eventExecutorGroup;
		favicon = Unpooled.unmodifiableBuffer(Unpooled.copiedBuffer(URLHelper.getBytesFromURL(getClass().getClassLoader().getResource("www/favicon.ico"))));
		favSize = favicon.readableBytes();
		
	}
	
	static final Charset UTF8 = Charset.forName("UTF8");
	
	@Override
	protected void decode(final ChannelHandlerContext ctx, final HttpRequest msg, final List<Object> out) throws Exception {
		final String uri = msg.uri();
		log.info("-----------------------> URI [{}]", uri);
		if(uri.endsWith("/favicon.ico")) {
			final DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, favicon);
			resp.headers().set(HttpHeaders.CONTENT_TYPE, "image/x-icon");
			resp.headers().setInt(HttpHeaders.CONTENT_LENGTH, favSize);				
			ctx.writeAndFlush(resp);
			return;
		}			
		ReferenceCountUtil.retain(msg);
		final ChannelPipeline p = ctx.pipeline();
		
		final int index = uri.indexOf("/api/");
		final String endpoint = index==-1 ? "" : uri.substring(5);
		if(index != -1 && pureJsonEndPoints.contains(endpoint) ) {
			log.info("Switching to PureJSON handler");
			p.addLast(eventExecutorGroup, "httpToJson", httpToJson);
//			p.addLast("jsonLogger", loggingHandler);
			p.addLast("jsonDecoder", new JsonObjectDecoder(true));
//			p.addLast("jsonLogger", loggingHandler);
			p.addLast("traceHandler", traceHandler);
			p.remove(this);
			if(msg instanceof FullHttpMessage) {
				out.add(msg);
			}
			
		} else {
			log.info("Switching to Http Request Manager");
			out.add(msg);
			p.addLast(eventExecutorGroup, "requestManager", HttpRequestManager.getInstance());
			p.remove(this);			
		}
	}	

	/**
	 * Sends a 204 (No Content) response
	 * @param ctx The channel handler context to write the response to
	 * @return the write completion future
	 */
	public ChannelFuture send204(final ChannelHandlerContext ctx) {
		return sendResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT));
	}
	
	/**
	 * Sends an HTTP response to the caller
	 * @param ctx The channel handler context to write the response to
	 * @param response The response to send
	 * @return the response write completion future
	 */
	public ChannelFuture sendResponse(final ChannelHandlerContext ctx, final HttpResponse response) {
		if(response==null) {
			ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR));
			throw new IllegalArgumentException("The passed HTTP response was null");		
		}	
		return ctx.writeAndFlush(response);
	}
	



}
