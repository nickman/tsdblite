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
package com.heliosapm.tsdblite.events;

/**
 * <p>Title: Event</p>
 * <p>Description: A tree of event names to keep a consistent one-stop-shop for all event keys and values</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.events.Event</code></p>
 */

public enum Event {
	/** Event emitted when a new data submission is received for a subscribed metric */
	NEWSUBMISSION("nsub"),
	/** Event emitted when a new metric is created for a subscribed metric pattern */
	NEWMETRIC("nmet"),
	/** Event emitted when a subscribed metric goes stale */
	RETIREDMETRIC("rmet");
	
	private Event(final String code) {
		this.code = code;
	}
	
	/** The value of the event type in a JSON doc */
	public final String code;
	
	/** The key of the event type in a JSON doc */
	public static final String KEY = "rtype";
	/** The key of the event data in a JSON doc */
	public static final String DATA = "data";
	
}
