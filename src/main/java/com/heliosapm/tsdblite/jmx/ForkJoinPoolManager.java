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
package com.heliosapm.tsdblite.jmx;

import javax.management.ObjectName;

import com.heliosapm.utils.jmx.JMXHelper;

import io.netty.util.internal.chmv8.ForkJoinPool;

/**
 * <p>Title: ForkJoinPoolManager</p>
 * <p>Description: A management wrapper for {@link ForkJoinPool} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.jmx.ForkJoinPoolManager</code></p>
 */

public class ForkJoinPoolManager implements ForkJoinPoolManagement {
	/** The pool being managed */
	private final ForkJoinPool pool;
	
	
	/**
	 * Creates a new ForkJoinPoolManager
	 * @param pool The pool being managed
	 * @param objectName The JMX ObjectName to register this manager with
	 */
	@SuppressWarnings("unused")
	public static void register(final ForkJoinPool pool, final ObjectName objectName) {
		new ForkJoinPoolManager(pool, objectName);
	}

	/**
	 * Creates a new ForkJoinPoolManager
	 * @param pool The pool being managed
	 * @param objectName The JMX ObjectName to register this manager with
	 */
	public ForkJoinPoolManager(final ForkJoinPool pool, final ObjectName objectName) {
		if(pool==null) throw new IllegalArgumentException("The passed ForkJoinPool was null");
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null");
		this.pool = pool;
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register ForkJoinPoolManager with ObjectName [" + objectName + "]", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#getParallelism()
	 */
	@Override
	public int getParallelism() {
		return pool.getParallelism();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#getPoolSize()
	 */
	@Override
	public int getPoolSize() {
		return pool.getPoolSize();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#isAsyncMode()
	 */
	@Override
	public boolean isAsyncMode() {
		return pool.getAsyncMode();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#getRunningThreadCount()
	 */
	@Override
	public int getRunningThreadCount() {
		return pool.getRunningThreadCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#getActiveThreadCount()
	 */
	@Override
	public int getActiveThreadCount() {
		return pool.getActiveThreadCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#isQuiescent()
	 */
	@Override
	public boolean isQuiescent() {
		return pool.isQuiescent();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#getStealCount()
	 */
	@Override
	public long getStealCount() {
		return pool.getStealCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#getQueuedTaskCount()
	 */
	@Override
	public long getQueuedTaskCount() {
		return pool.getQueuedTaskCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#getQueuedSubmissionCount()
	 */
	@Override
	public int getQueuedSubmissionCount() {
		return pool.getQueuedSubmissionCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#isQueuedSubmissions()
	 */
	@Override
	public boolean isQueuedSubmissions() {
		return pool.hasQueuedSubmissions();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#isTerminated()
	 */
	@Override
	public boolean isTerminated() {
		return pool.isTerminated();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#isTerminating()
	 */
	@Override
	public boolean isTerminating() {
		return pool.isTerminating();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement#isShutdown()
	 */
	@Override
	public boolean isShutdown() {
		return pool.isShutdown();
	}
	
	
}
