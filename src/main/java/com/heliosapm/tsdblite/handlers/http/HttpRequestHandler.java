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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: HttpRequestHandler</p>
 * <p>Description: The base class to implement http request handlers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.HttpRequestHandler</code></p>
 */

public abstract class HttpRequestHandler {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Processes the passed TSDBHttpRequest
	 * @param request The incoming TSDBHttpRequest
	 */
	protected abstract void process(final TSDBHttpRequest request);

}
