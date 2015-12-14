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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.net.HttpHeaders;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * <p>Title: TSDBHttpRequest</p>
 * <p>Description: Wraps up a channel and the http request it is carrying</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.TSDBHttpRequest</code></p>
 */

public class TSDBHttpRequest  {
	/** The incoming HTTP request */
	protected final HttpRequest request;
	/** The channel the request came in on */
	protected final Channel channel;
	/** The channel handler context of the http request router */
	protected final ChannelHandlerContext ctx;
	/** The request path */
	protected final String path;
	/** The request path elements */
	protected final String[] pathElements; 
	
	/** The routing key */
	protected final String route;
	/** The lazilly created Query decoder */
	protected volatile QueryStringDecoder decoder = null;
	/** The decoded parameters */
	protected volatile Map<String,List<String>> params = null;
	
	/** The path splitter regex */
	public static final Pattern PATH_SPLIT = Pattern.compile("/|\\?");	
	/** An empty buffer const */
	public static final ByteBuf EMPTY_BUFF = Unpooled.EMPTY_BUFFER;
	/** UTF8 Character Set */
	public static final Charset UTF8 = Charset.forName("UTF8");
	
	/**
	 * Creates a new TSDBHttpRequest
	 * @param request The incoming HTTP request
	 * @param channel The channel the request came in on
	 * @param ctx The http request router's channel handler context
	 */
	protected TSDBHttpRequest(final HttpRequest request, final Channel channel, final ChannelHandlerContext ctx) {
		this.request = request;
		this.channel = channel;
		this.ctx = ctx;
		final QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
		path = decoder.path();
		final StringBuilder b = new StringBuilder("/api/");
		pathElements = PATH_SPLIT.split(path);
		for(String part: pathElements) {
			if(part==null || part.trim().isEmpty() || "api".equals(part)) {
				continue;
			}
			b.append(part);
			break;
		}
		route = b.toString();		
	}
	
	/**
	 * Decodes the uri
	 */
	protected void decode() {
		if(decoder==null) {
			decoder = new QueryStringDecoder(request.uri());
		}
	}
	
	/**
	 * Returns the query parameters
	 * @return the query parameters
	 */
	public Map<String,List<String>> getParameters() {
		if(params==null) {
			decode();
			params = decoder.parameters();
		}
		return params;
	}
	
	/**
	 * Returns the named parameter
	 * @param key The parameter name
	 * @return The parameter value or null if not found
	 */
	public String getParameter(final String key) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty");
		final List<String> list = getParameters().get(key.trim());
		if(list==null) return null;
		return list.get(0);
	}

	/**
	 * Returns the HTTP request
	 * @return the HTTP request
	 */
	public HttpRequest getRequest() {
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
	 * Returns the path 
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	
	/**
	 * Returns the path elements
	 * @return the path elements
	 */
	public String[] getPathElements() {
		return pathElements;
	}
	
	/**
	 * Returns the route
	 * @return the route
	 */
	public String getRoute() {
		return route;
	}
	
	/**
	 * Sends a 404 (Not Found) response
	 * @return the write completion future
	 */
	public ChannelFuture send404() {
		return sendResponse(response(HttpResponseStatus.NOT_FOUND, "No route for [", route, "]"));
	}
	
	/**
	 * Sends a 40o (Bad Request) response
	 * @param msgs An optional array of message segments to be concatenated and set as the response body
	 * @return the write completion future
	 */
	public ChannelFuture send400(final String...msgs) {
		return sendResponse(response(HttpResponseStatus.BAD_REQUEST, msgs));
	}
	
	
	/**
	 * Sends a 204 (No Content) response
	 * @return the write completion future
	 */
	public ChannelFuture send204() {
		return sendResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT));
	}
	
	private static HttpResponse response(final HttpResponseStatus status, final String...msgs) {
		final ByteBuf buf = join(msgs);
		if(buf.readableBytes()==0) {
			return new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		}
		final DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);
		resp.headers().setInt(HttpHeaders.CONTENT_LENGTH, buf.readableBytes());
		resp.headers().set(HttpHeaders.CONTENT_TYPE, "text/plain");
		return resp;
	}
	
	private static ByteBuf join(final String...msgs) {
		final String s;
		if(msgs.length == 0 || msgs[0]==null) {
			return EMPTY_BUFF;
		}
		if(msgs.length == 1) {
			s = msgs[0];
		} else {
			final StringBuilder b = new StringBuilder();
			for(String msg : msgs) {
				b.append(msg);
			}
			s = b.toString();
		}
		return Unpooled.copiedBuffer(s.getBytes(UTF8));
	}
	
	/**
	 * Indicates if the request has a readable content body
	 * @return true if the request has a readable content body, false otherwise
	 */
	public boolean hasContent() {
		if(request instanceof FullHttpRequest) {
			final ByteBuf content = ((FullHttpRequest)request).content();
			if(content==null) return false;
			if(content.readableBytes()<1) return false;
			return true;			
		}
		return false;
	}
	
	/**
	 * Returns the http request content
	 * @return the http request content
	 */
	public ByteBuf getContent() {
		if(request instanceof FullHttpRequest) {
			final ByteBuf bb = ((FullHttpRequest)request).content();
			return bb==null ? EMPTY_BUFF :bb;
		}
		return EMPTY_BUFF;
	}
	
	/**
	 * Sends an HTTP response to the caller
	 * @param response The response to send
	 * @return the response write completion future
	 */
	public ChannelFuture sendResponse(final HttpResponse response) {
		if(response==null) {
			ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR));
			throw new IllegalArgumentException("The passed HTTP response was null");		
		}
		
		return ctx.writeAndFlush(response);
	}

	/**
	 * Returns the channel handler context
	 * @return the channel handler context
	 */
	public ChannelHandlerContext context() {
		return ctx;
	}

	
	
}
