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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

/**
 * <p>Title: HttpToJsonAdapterHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.http.HttpToJsonAdapterHandler</code></p>
 */
@Sharable
public class HttpToJsonAdapterHandler extends MessageToMessageDecoder<HttpRequest> {
	@Override
	protected void decode(ChannelHandlerContext ctx, HttpRequest msg, List<Object> out) throws Exception {
		if(msg instanceof FullHttpRequest) {
			ByteBuf b = ((FullHttpRequest)msg).content().copy();
			out.add(b);
		}		
	}



//
//@Override
//	protected void messageReceived(final ChannelHandlerContext ctx, final FullHttpRequest msg) throws Exception {
//		ChannelPromise promise = new DefaultChannelPromise(ctx.channel());
//		write(ctx, msg.content(), promise);		
//	}
}
