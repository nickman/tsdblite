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
package com.heliosapm.tsdblite.metric;

import java.util.Date;
import java.util.SortedMap;

/**
 * <p>Title: MetricMetaMXBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.MetricMetaMXBean</code></p>
 */

public interface MetricMetaMXBean {
	
	/**
	 * Returns the meta's long hash code
	 * @return the meta's long hash code
	 */
	public long getHashCode();
	
	/**
	 * Returns the timestamp of the last update to this meta as a UTC long
	 * @return the timestamp of the last update to this meta as a UTC long
	 */
	public long getLastUpdateTs();
	
	/**
	 * Returns the timestamp of the last update to this meta as a java date
	 * @return the timestamp of the last update to this meta as a java date
	 */
	public Date getLastUpdate();

	/**
	 * Returns the meta's name value pairs
	 * @return the meta's name value pairs
	 */
	public SortedMap<String, String> getMeta();
	
	/**
	 * Returns the number of key/value pairs
	 * @return the number of key/value pairs
	 */
	public int getSize();
	
	/**
	 * Adds a meta name value pair
	 * @param name The pair name
	 * @param value The pair value
	 * @return true if the pair was added, false otherwise
	 */
	public boolean addPair(final String name, final String value);

}
