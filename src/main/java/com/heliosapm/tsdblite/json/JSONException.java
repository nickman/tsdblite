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
package com.heliosapm.tsdblite.json;

/**
 * <p>Title: JSONException</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.json.JSONException</code></p>
 */

public class JSONException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = 8443258749169694791L;

	/**
	 * Creates a new JSONException
	 */
	public JSONException() {

	}

	/**
	 * Creates a new JSONException
	 * @param message The exception message
	 */
	public JSONException(String message) {
		super(message);
	}

	/**
	 * Creates a new JSONException
	 * @param cause The underlying exception cause
	 */
	public JSONException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new JSONException
	 * @param message The exception message
	 * @param cause The underlying exception cause
	 */
	public JSONException(String message, Throwable cause) {
		super(message, cause);
	}

}

