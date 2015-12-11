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

import io.netty.util.internal.chmv8.ForkJoinPool;
import io.netty.util.internal.chmv8.ForkJoinPool.ForkJoinWorkerThreadFactory;
import io.netty.util.internal.chmv8.ForkJoinTask;
import io.netty.util.internal.chmv8.ForkJoinWorkerThread;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jsr166e.LongAdder;

/**
 * <p>Title: ManagedForkJoinPool</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.jmx.ManagedForkJoinPool</code></p>
 */

public class ManagedForkJoinPool extends ForkJoinPool implements ForkJoinWorkerThreadFactory, UncaughtExceptionHandler {
	/** The JVM core count */
	public static final int CORES = Runtime.getRuntime().availableProcessors();
	
	/** A counter of submitted forkJoinTasks */
	final LongAdder forkJoinTasks = new LongAdder();
	/** A counter of submitted runnables */
	final LongAdder runnableTasks = new LongAdder();
	/** A counter of submitted callables */
	final LongAdder callableTasks = new LongAdder();
	
	/**
	 * Creates a new ManagedForkJoinPool
	 */
	public ManagedForkJoinPool() {
		super();
	}
	
	private ManagedForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler,
			boolean asyncMode) {
		super(parallelism, factory, handler, asyncMode);
	}
	
	/**
	 * Creates a new ManagedForkJoinPool
	 * @param parallelism
	 * @param factory
	 * @param handler
	 * @param asyncMode
	 */
	public ManagedForkJoinPool(int parallelism, boolean asyncMode) {
		this(parallelism, this, this, asyncMode);
		
	}
	
	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		// TODO Auto-generated method stub		
	}



	/**
	 * Creates a new ManagedForkJoinPool
	 * @param parallelism
	 */
	public ManagedForkJoinPool(int parallelism) {
		super(parallelism);
		// TODO Auto-generated constructor stub
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
	public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
		// TODO Auto-generated method stub
		return super.submit(task);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.util.internal.chmv8.ForkJoinPool#submit(java.util.concurrent.Callable)
	 */
	@Override
	public <T> ForkJoinTask<T> submit(Callable<T> task) {
		// TODO Auto-generated method stub
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
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
		// TODO Auto-generated method stub
		return super.invokeAll(tasks);
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#invokeAny(java.util.Collection)
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		return super.invokeAny(tasks);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		// TODO Auto-generated method stub
		return super.invokeAny(tasks, timeout, unit);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return super.invokeAll(tasks, timeout, unit);
	}

	
}
