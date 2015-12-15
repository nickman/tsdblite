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
package com.heliosapm.tsdblite.handlers.websock;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.heliosapm.tsdblite.handlers.http.HttpRequestManager;
import com.heliosapm.tsdblite.json.JSON;
import com.heliosapm.utils.collections.FluentMap;
import com.heliosapm.utils.collections.FluentMap.MapType;

/**
 * <p>Title: WebSocketServerHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.WebSocketServerHandler</code></p>
 */

public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
	/** A map of websock handlers keyed by the op code */
	protected final Map<String, WebSocketRequestHandler> handlers = new ConcurrentHashMapV8<String, WebSocketRequestHandler>();
	/**
	 * Creates a new WebSocketServerHandler
	 */
	public WebSocketServerHandler() {
		super(true);
		handlers.putAll(SubscriptionRequestHandlers.registerAll());
	}

    private static final String WEBSOCKET_PATH = "/ws";

    private WebSocketServerHandshaker handshaker;

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
        	final HttpRequest httpR = (HttpRequest)msg;
            //handleHttpRequest(ctx, (FullHttpRequest) msg);
        	if(WEBSOCKET_PATH.equals(httpR.uri())) {
        		if(httpR instanceof FullHttpRequest) {
        			handleHttpRequest(ctx, (FullHttpRequest)httpR);        			
        		}
        	} else {
        		HttpRequestManager.getInstance().messageReceived(ctx, (HttpRequest)msg);
        	}
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        // Allow only GET methods.
        if (req.method() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        if ("/favicon.ico".equals(req.uri())) {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            sendHttpResponse(ctx, req, res);
            return;
        }
        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, true);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            ctx.writeAndFlush(new TextWebSocketFrame("{\"session\" : \"" + ctx.channel().id().asShortText() + "\"}"));
        }
    }

    private void handleWebSocketFrame(final ChannelHandlerContext ctx, final WebSocketFrame frame) {

        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        final JsonNode node = JSON.parseToNode(frame.content());
        long rid = -1;
        String session = "";
        String op = null;
        try {
        	rid = node.get("rid").asLong();
        } catch (Exception ex) { /* No Op */ }
        try {
        	session = node.get("session").asText();
        } catch (Exception ex) { /* No Op */ }
        try {
        	op = node.get("op").asText();
        } catch (Exception ex) { /* No Op */ }
        
        if(op==null) {
        	sendWebSockError(ctx, rid, session, "Request had no op code", null);
        	return;
        }
        final WebSocketRequestHandler handler = handlers.get(op.toLowerCase().trim());
        if(handler==null) {
        	sendWebSockError(ctx, rid, session, "Unrecognized op code: [" + op + "]", null);        	
        }
        try {
        	handler.processRequest(new WebSocketRequest(ctx, ctx.channel(), node));
        } catch (Exception ex) {
        	sendWebSockError(ctx, rid, session, "Failed to process WebSock Request: [" + op + "]", ex);
        }
        return;
    }
    
    private static void sendWebSockError(final ChannelHandlerContext ctx, final Number rid, final String session, final String error, final Throwable t) {
    	final String ts;
    	if(t != null) {
    		final StringWriter sw = new StringWriter();
    		final PrintWriter pw = new PrintWriter(sw, true);
    		sw.flush();
    		t.printStackTrace(pw);
    		ts = sw.toString();
    	} else {
    		ts = null;
    	}

  		ctx.writeAndFlush(new TextWebSocketFrame(
  	  		JSON.serializeToBuf(
  	  				FluentMap.newMap(MapType.LINK, String.class, Object.class)
  	      		.fput("error", error)	
  	      		.fput("rid", rid)
  	      		.sfput("session", session)
  	      		.sfput("trace", ts)
  	      		.asMap(LinkedHashMap.class)    				
  	  		)  				
  		));
    	
    }

    private static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpHeaderUtil.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaderUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location =  req.headers().get(HOST) + WEBSOCKET_PATH;
        return "ws://" + location;
    }

}
