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
package com.heliosapm.tsdblite.jmx;

import java.util.Date;

import javax.management.ObjectName;

import com.heliosapm.tsdblite.metric.MetricCache.Metric;
import com.heliosapm.tsdblite.metric.Trace;

/**
 * <p>Title: RegisteredMetric</p>
 * <p>Description: Represents a JMX exposed Metric</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.jmx.RegisteredMetric</code></p>
 */

public class RegisteredMetric {
	/** The metric */
	protected final Metric metric;
	/** The JMX ObjectName the metric is registered under */
	protected final ObjectName objectName;
	/** The last value submitted */
	protected Number lastValue = null;
	/** The timestamp of the last value submitted */
	protected long lastSubmission = -1L;
	
	
	/**
	 * Creates a new RegisteredMetric
	 * @param metric The metric
	 */
	public RegisteredMetric(final Metric metric) {
		this.metric = metric;
		objectName = this.metric.toObjectName();
	}

	/**
	 * Submits a new trace for this metric
	 * @param trace The trace to apply
	 */
	public void submit(final Trace trace) {
		lastValue = trace.getValue();
		lastSubmission = trace.getTimestampMs();
	}

	/**
	 * Returns the last value submitted or null if no submissions have occurred
	 * @return the last value submitted
	 */
	public Number getLastValue() {
		return lastValue;
	}

	/**
	 * Returns the timestamp of the last submission as a long UTC 
	 * @return the timestamp of the last submission
	 */
	public long getLastSubmission() {
		return lastSubmission;
	}

	/**
	 * Returns the timestamp of the last submission as a java Date 
	 * @return the timestamp of the last submission or null if one has not occurred
	 */
	public Date getLastSubmissionDate() {
		return lastSubmission==-1L ? null : new Date(lastSubmission);
	}

	/**
	 * Returns the metric
	 * @return the metric
	 */
	public Metric getMetric() {
		return metric;
	}


	/**
	 * Returns the JMX ObjectName for this metric 
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}
	
	

}
