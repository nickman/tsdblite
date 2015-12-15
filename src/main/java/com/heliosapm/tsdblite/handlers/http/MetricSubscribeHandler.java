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


/**
 * <p>Title: MetricSubscribeHandler</p>
 * <p>Description: HTTP request handler to subscribe to metric data feeds</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.MetricSubscribeHandler</code></p>
 */

public class MetricSubscribeHandler extends HttpRequestHandler {

	/**
	 * Creates a new MetricSubscribeHandler
	 */
	public MetricSubscribeHandler() {
		super();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.handlers.http.HttpRequestHandler#process(com.heliosapm.tsdblite.handlers.http.TSDBHttpRequest)
	 */
	@Override
	protected void process(final TSDBHttpRequest request) {
		//request.getRequest().
	}

}
