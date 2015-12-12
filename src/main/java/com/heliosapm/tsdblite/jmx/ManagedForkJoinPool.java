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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.utils.jmx.JMXHelper;

import io.netty.util.internal.chmv8.ForkJoinPool;
import io.netty.util.internal.chmv8.ForkJoinTask;
import jsr166e.LongAdder;

/**
 * <p>Title: ManagedForkJoinPool</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.jmx.ManagedForkJoinPool</code></p>
 */

public class ManagedForkJoinPool extends ForkJoinPool implements ManagedForkJoinPoolMXBean {
	/** The JVM core count */
	public static final int CORES = Runtime.getRuntime().availableProcessors();
	
	/** Static class logger */
	protected static final Logger log = LoggerFactory.getLogger(ManagedForkJoinPool.class);
	/** A counter of submitted forkJoinTasks */
	final LongAdder forkJoinTasks = new LongAdder();
	/** A counter of submitted runnables */
	final LongAdder runnableTasks = new LongAdder();
	/** A counter of submitted callables */
	final LongAdder callableTasks = new LongAdder();
	/** The name of this pool */
	final String poolName;
	/** The JMX ObjectName of this pool's management interface */
	final ObjectName objectName;
	
	
	/**
	 * Creates a new ManagedForkJoinPool
	 * @param name The name of this pool and the prefix for created threads
	 * @param parallelism the parallelism level of the pool
	 * @param asyncMode If true, establishes local first-in-first-out scheduling mode for forked tasks that are never joined. 
	 * This mode may be more appropriate than default locally stack-based mode in applications in which worker threads only process event-style asynchronous tasks. 
	 * For default value, use false.
	 */
	public ManagedForkJoinPool(final String name, final int parallelism, final boolean asyncMode) {
		super(
			parallelism, 
			new ManagedForkJoinWorkerThreadFactory(name, Thread.NORM_PRIORITY, true),  
			new UncaughtExceptionHandler(){
				@Override
				public void uncaughtException(final Thread t, final Throwable e) {
					log.error("Uncaught Exception in ManagedForkJoinPool [{}] on Thread [{}]", name, t, e);
				}
			}, 
			asyncMode
		);
		poolName = name;
		objectName = JMXHelper.objectName(String.format(OBJECT_NAME_TEMPLATE, poolName));
		JMXHelper.registerMBean(this, objectName);
	}


	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#invoke(io.netty.util.internal.chmv8.ForkJoinTask)
	 */
	@Override
	public <T> T invoke(final ForkJoinTask<T> task) {
		forkJoinTasks.increment();
		return super.invoke(task);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#execute(io.netty.util.internal.chmv8.ForkJoinTask)
	 */
	@Override
	public void execute(final ForkJoinTask<?> task) {
		forkJoinTasks.increment();
		super.execute(task);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#execute(java.lang.Runnable)
	 */
	@Override
	public void execute(final Runnable task) {
		runnableTasks.increment();
		super.execute(task);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#submit(io.netty.util.internal.chmv8.ForkJoinTask)
	 */
	@Override
	public <T> ForkJoinTask<T> submit(final ForkJoinTask<T> task) {
		forkJoinTasks.increment();
		return super.submit(task);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#submit(java.util.concurrent.Callable)
	 */
	@Override
	public <T> ForkJoinTask<T> submit(Callable<T> task) {
		callableTasks.increment();
		return super.submit(task);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#submit(java.lang.Runnable, java.lang.Object)
	 */
	@Override
	public <T> ForkJoinTask<T> submit(final Runnable task, final T result) {
		runnableTasks.increment();
		return super.submit(task, result);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#submit(java.lang.Runnable)
	 */
	@Override
	public ForkJoinTask<?> submit(final Runnable task) {
		runnableTasks.increment();
		return super.submit(task);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#invokeAll(java.util.Collection)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) {
		callableTasks.add(tasks.size());
		return super.invokeAll(tasks);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#invokeAny(java.util.Collection)
	 */
	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		callableTasks.add(tasks.size());
		return super.invokeAny(tasks);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		callableTasks.add(tasks.size());
		return super.invokeAny(tasks, timeout, unit);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
		callableTasks.add(tasks.size());
		return super.invokeAll(tasks, timeout, unit);
	}


	@Override
	public boolean isAsyncMode() {
		return getAsyncMode();
	}


	@Override
	public boolean isQueuedSubmissions() {
		return hasQueuedSubmissions();
	}
	
	@Override
	public long getCallableTasks() {
		return callableTasks.longValue();
	}
	
	@Override
	public long getRunnableTasks() {
		return runnableTasks.longValue();
	}
	
	@Override
	public long getForkJoinTasks() {
		return forkJoinTasks.longValue();
	}
	
	@Override
	public void resetCounters() {
		forkJoinTasks.reset();
		runnableTasks.reset();
		callableTasks.reset();		
	}

	
}
