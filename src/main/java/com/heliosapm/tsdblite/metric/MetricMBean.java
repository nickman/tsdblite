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

import java.util.SortedMap;

/**
 * <p>Title: MetricMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.metric.MetricMBean</code></p>
 */

public interface MetricMBean {
	/**
	 * Returns the metric name
	 * @return the metric name
	 */
	public String getMetricName();
	
	/**
	 * Returns the metric tags
	 * @return the metric tags
	 */
	public SortedMap<String, String> getTags();
	
	/**
	 * Returns the metric tags
	 * @return the metric tags
	 */
	public String getTagStr();
	
	
	/**
	 * Returns the metric hash 
	 * @return the metric hash
	 */
	public long getHashCode();

}