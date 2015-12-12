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
package com.heliosapm.tsdblite.sub;

import javax.management.NotificationListener;

/**
 * <p>Title: NotificationListenerFactory</p>
 * <p>Description: Defines a factory that creates {@link javax.management.NotificationListener}s for {@link Subscription}s</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.sub.NotificationListenerFactory</code></p>
 */

public interface NotificationListenerFactory {
	/**
	 * Creates a notification listener for the passed Subscription
	 * @param sub The subscription to create the listener for
	 * @return a {@link javax.management.NotificationListener} which might also implement {@link javax.management.NotificationFilter}
	 */
	public NotificationListener createListener(final Subscription sub);
}
