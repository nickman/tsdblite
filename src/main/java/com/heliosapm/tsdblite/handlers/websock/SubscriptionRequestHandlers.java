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

import java.util.HashMap;
import java.util.Map;


/**
 * <p>Title: SubscriptionRequestHandlers</p>
 * <p>Description: Event subscription handlers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.websock.SubscriptionRequestHandlers</code></p>
 */

public abstract class SubscriptionRequestHandlers {

	
	public static class WebSocketPingRequestHandler implements WebSocketRequestHandler {
		@Override
		public void processRequest(final WebSocketRequest request) throws Exception {
			request.sendResponse("pong");			
		}
	}
	
	
	
	public static Map<String, WebSocketRequestHandler> registerAll() {
		final Map<String, WebSocketRequestHandler> map = new HashMap<String, WebSocketRequestHandler>();
		map.put("ping", new WebSocketPingRequestHandler());
		return map;
	}
	

	private SubscriptionRequestHandlers() {
	}

}
