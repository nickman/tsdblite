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
package com.heliosapm.tsdblite.handlers.websock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heliosapm.tsdblite.json.JSON;

/**
 * <p>Title: WebSocketRequest</p>
 * <p>Description: Represents a partially unwrapped JSON WebSocket request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.websock.WebSocketRequest</code></p>
 */

public class WebSocketRequest {
	/** The request id */
	final long rid;
	/** The request op name */
	final String op;
	/** The request session id */
	final String session;
	/** The request args */
	final Object[] args;
	/** The request name/value pairs map */
	final HashMap<String, Object> map;
	/** The chanel handler context that the request came in on */
	final ChannelHandlerContext ctx;
	/** The chanel that the request came in on */
	final Channel channel;
	/** The parsed JSON node */
	final JsonNode node;
	
	/** Empty object array const */
	public static final Object[] EMPTY_OBJ_ARR = {};
	/** Empty map const */
	private static final HashMap<String, Object> EMPTY_MAP_CONST = new HashMap<String, Object>(0); 

	/**
	 * Creates a new WebSocketRequest
	 * @param ctx The chanel handler context that the request came in on
	 * @param channel The chanel that the request came in on
	 * @param node The parsed JSON node
	 */
	public WebSocketRequest(final ChannelHandlerContext ctx, final Channel channel, final JsonNode node) {
		if(ctx==null) throw new IllegalArgumentException("The passed ChannelHandlerContext was null");
		if(channel==null) throw new IllegalArgumentException("The passed Channel was null");
		if(node==null) throw new IllegalArgumentException("The passed JsonNode was null");
		this.ctx = ctx;
		this.channel = channel;
		this.node = node;
    long _rid = -1;
    String _session = "";
    String _op = null;
    Object[] _args = null;
    HashMap<String, Object> _map = null;
    try {
    	_rid = node.get("rid").asLong();
    } catch (Exception ex) {
    	_rid = -1L;
    }    try {
    	_op = node.get("op").asText();
    } catch (Exception ex) {
    	_op = null;
    }

    try {
    	_session = node.get("session").asText();
    } catch (Exception ex) {
    	_session = null;
    }
    try {
    	_op = node.get("op").asText();
    } catch (Exception ex) {
    	_op = null;
    }
    try {
    	_args = JSON.parseToObject(node.get("args"), Object[].class);
    } catch (Exception ex) {
    	_args = EMPTY_OBJ_ARR;
    }
    try {
    	_map = JSON.parseToObject(node.get("map"), EMPTY_MAP_CONST.getClass());
    } catch (Exception ex) {
    	_map = new HashMap<String, Object>(0);
    }
    
    rid = _rid;
    session = _session;
    op = _op;
    args = _args;
    map = _map;
	}
	
	/**
	 * Sends the passed JSON node as the response to this request
	 * The <b><code>rerid</code></b> is added to the response so the caller
	 * can correlate it's request to this response.
	 * @param responseNode The response node.
	 * @return The write future
	 */
	public ChannelFuture sendResponse(final ObjectNode responseNode) {
		if(responseNode==null) throw new IllegalArgumentException("The passed ObjectNode was null");
		responseNode.set("rerid", JsonNodeFactory.instance.numberNode(rid));
		return ctx.writeAndFlush(new TextWebSocketFrame(
	  		JSON.serializeToBuf(responseNode)  				
		));
	}
	
	public ChannelFuture sendResponseMap(final Map<String, Object> response) {
		if(response==null) throw new IllegalArgumentException("The passed Object was null");
		final ObjectNode resp = (ObjectNode)JSON.serializeToNode(response);
		return sendResponse(resp);
	}

	public ChannelFuture sendResponse(final String key, Object response) {
		final String _key = (key==null || key.trim().isEmpty()) ? "response" : key.trim();
		if(response==null) throw new IllegalArgumentException("The passed Object was null");
		return sendResponseMap(Collections.singletonMap(_key, response));
	}
	
	public ChannelFuture sendResponse(Object response) {
		return sendResponse("response", response);
	}
	
	/**
	 * Returns the request id 
	 * @return the rid
	 */
	public long getRid() {
		return rid;
	}

	/**
	 * Returns the request op name
	 * @return the op
	 */
	public String getOp() {
		return op;
	}

	/**
	 * Returns the request session
	 * @return the session
	 */
	public String getSession() {
		return session;
	}
	
	/**
	 * Returns thr raw request node
	 * @return the request node
	 */
	public JsonNode getRequestNode() {
		return node;
	}
	

	/**
	 * Returns a copy of the args
	 * @return the args
	 */
	public Object[] getArgs() {
		return args.clone();
	}
	
	/**
	 * Returns the read-only name/value pair map
	 * @return the read-only name/value pair map
	 */
	public Map<String, Object> getMap() {
		return Collections.unmodifiableMap(map);
	}
	
	/**
	 * Returns the arg value for the specified index
	 * @param index The arg index
	 * @param type The expected type of the value
	 * @return The value
	 */
	public <T> T getArg(final int index, final Class<T> type) {
		if(index < 0 || index > args.length-1) throw new IllegalArgumentException("Invalid index [" + index + "] for arg array of length [" + args.length + "]");
		if(type==null) throw new IllegalArgumentException("The passed type was null");
		final Object o = args[index];
		return o==null ? null : type.cast(o);
	}
	
	/**
	 * Returns the value for the specified name
	 * @param name The map key
	 * @param type The expected type of the value
	 * @return The value
	 */
	public <T> T getValue(final String name, final Class<T> type) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		if(type==null) throw new IllegalArgumentException("The passed type was null");
		final Object o = map.get(name.trim());
		return o==null ? null : type.cast(o);
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder("WebSockReq [s:")
			.append(session==null ? "<null>" : session)
			.append(", rid:").append(rid)
			.append(", op:").append(op==null ? "<null>" : op)
			.append(", args:").append(Arrays.toString(args))
			.append("]")
			.toString();
	}

	
}
