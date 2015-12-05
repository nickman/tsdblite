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

import javax.management.MXBean;

/**
 * <p>Title: ForkJoinPoolManagement</p>
 * <p>Description: MXBean definition for ForkJoin thread pools</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.jmx.ForkJoinPoolManagement</code></p>
 */
@MXBean
public interface ForkJoinPoolManagement {
	
	/**
	 * Returns the targeted parallelism level of this pool.
	 * @return the targeted parallelism level of this pool
	 */
	public int getParallelism();
	
	/**
	 * Returns the number of worker threads that have started but not
	 * yet terminated.  The result returned by this method may differ
	 * from {@link #getParallelism} when threads are created to
	 * maintain parallelism when others are cooperatively blocked.
	 * @return the number of worker threads
	 */
	public int getPoolSize();
	
	/**
	 * Returns {@code true} if this pool uses local first-in-first-out
	 * scheduling mode for forked tasks that are never joined.
	 * @return {@code true} if this pool uses async mode, false otherwise
	 */
	public boolean isAsyncMode();
	
	/**
     * Returns an estimate of the number of worker threads that are
     * not blocked waiting to join tasks or for other managed
     * synchronization. This method may overestimate the
	 * number of running threads.
	 * @return the number of worker threads
	 */
	public int getRunningThreadCount();
	
	/**
	 * Returns an estimate of the number of threads that are currently 
	 * stealing or executing tasks. This method may overestimate the\
	 * number of active threads.
	 * @return the number of active threads
	 */
	public int getActiveThreadCount();
	
    /**
     * Returns {@code true} if all worker threads are currently idle.
     * An idle worker is one that cannot obtain a task to execute
     * because none are available to steal from other threads, and
     * there are no pending submissions to the pool. This method is
     * conservative; it might not return {@code true} immediately upon
     * idleness of all threads, but will eventually become true if
     * threads remain inactive.
     * @return {@code true} if all threads are currently idle
     */
	public boolean isQuiescent();
	
    /**
     * Returns an estimate of the total number of tasks stolen from
     * one thread's work queue by another. The reported value
     * underestimates the actual total number of steals when the pool
     * is not quiescent. This value may be useful for monitoring and
     * tuning fork/join programs: in general, steal counts should be
     * high enough to keep threads busy, but low enough to avoid
     * overhead and contention across threads.
     * @return the number of steals
     */
	public long getStealCount();
	
    /**
     * Returns an estimate of the total number of tasks currently held
     * in queues by worker threads (but not including tasks submitted
     * to the pool that have not begun executing). This value is only
     * an approximation, obtained by iterating across all threads in
     * the pool. This method may be useful for tuning task
     * granularities.
     * @return the number of queued tasks
     */	
	public long getQueuedTaskCount();
	
    /**
     * Returns an estimate of the number of tasks submitted to this
     * pool that have not yet begun executing.  This method may take
     * time proportional to the number of submissions.
     * @return the number of queued submissions
     */	
	public int getQueuedSubmissionCount();
	
    /**
     * Returns {@code true} if there are any tasks submitted to this
     * pool that have not yet begun executing.
     * @return {@code true} if there are any queued submissions
     */	
	public boolean isQueuedSubmissions();
	
    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * @return {@code true} if all tasks have completed following shut down
     */	
	public boolean isTerminated();
	
    /**
     * Returns {@code true} if the process of termination has
     * commenced but not yet completed.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, or are waiting for I/O,
     * causing this executor not to properly terminate. (See the
     * advisory notes for class ForkJoinTask stating that
     * tasks should not normally entail blocking operations.  But if
     * they do, they must abort them on interrupt.)
     * @return {@code true} if terminating but not yet terminated
     */	
	public boolean isTerminating();
	
    /**
     * Returns {@code true} if this pool has been shut down.
     * @return {@code true} if this pool has been shut down
     */
	public boolean isShutdown();
	
	
	
}
