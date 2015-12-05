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
package com.heliosapm.tsdblite.metric;

import java.util.Comparator;

/**
 * <p>Title: TagKeySorter</p>
 * <p>Description: A metric tag key sorter that sorts <b><code>host</code></b> first, 
 * then <b><code>host</code></b> and then all other keys alphabetically.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.TagKeySorter</code></p>
 */

public class TagKeySorter implements Comparator<String> {
	/** A shareable static instance */
	public static final TagKeySorter INSTANCE = new TagKeySorter();
	
	/** The host tag key */
	public static final String HOST = "host";
	/** The app tag key */
	public static final String APP = "app";
	
	/**
	 * Creates a new TagKeySorter
	 */
	private TagKeySorter() {
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(final String o1, final String o2) {
		if(o1.equals(o2)) return 0;
		final boolean o1Host = HOST.equalsIgnoreCase(o1);
		if(o1Host) return -1;
		final boolean o2Host = HOST.equalsIgnoreCase(o2);
		if(o2Host) return 1;
		final boolean o1App = APP.equalsIgnoreCase(o1);
		if(o1App) return -1;
		final boolean o2App = APP.equalsIgnoreCase(o2);
		if(o2App) return 1;
		if(o1.equalsIgnoreCase(o2)) return -1;
		return o1.toLowerCase().compareTo(o2.toLowerCase());
	}

}
