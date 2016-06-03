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
package com.heliosapm.tsdblite;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.ExecutorService;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdblite.handlers.ProtocolSwitch;
import com.heliosapm.tsdblite.jmx.ManagedDefaultExecutorServiceFactory;
import com.heliosapm.tsdblite.metric.MetricCache;
import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.jmx.JMXHelper;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import jsr166e.LongAdder;

/**
 * <p>Title: Server</p>
 * <p>Description: The netty core server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.Server</code></p>
 */

public class Server extends ChannelInitializer<SocketChannel> implements ServerMXBean {
	/** The singleton instance */
	private static volatile Server instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The listening port */
	protected final int port;
	/** The listening bind interface */
	protected final String iface;
	/** The netty boss thread pool */
	protected final ExecutorService bossPool;
	/** The netty worker thread pool */
	protected final ExecutorService workerPool;
	/** The netty channel group thread pool */
	protected final ExecutorService channelGroupPool;
	
	/** The netty boss event loop */
	 protected final EventLoopGroup bossGroup;
	 /** The netty worker event loop */
	 protected final EventLoopGroup workerGroup;
	 /** The channel group evcent executor */
	 protected final DefaultEventExecutor groupExecutor;
	 /** The netty nio selector provider */
	 protected final SelectorProvider selectorProvider = SelectorProvider.provider();
	 /** The server bootstrap */
	 protected final ServerBootstrap bootStrap;
	 /** The channel group holding all the child channels */
	 protected final ChannelGroup channelGroup;
	 
	 /** The server channel */
	protected NioServerSocketChannel serverChannel = null;
	 
	 /** The server channel logging handler */
	protected final LoggingHandler loggingHandler;
	
	/** A counter of created channels */
	protected final LongAdder createdChannels = new LongAdder();
	 
	 /** The boss pool JMX ObjectName */
	public static final ObjectName BOSS_POOL_ON = JMXHelper.objectName(Server.class.getPackage().getName() + ":service=ForkJoinPool,name=BossPool");
	 /** The worker pool JMX ObjectName */
	public static final ObjectName WORKER_POOL_ON = JMXHelper.objectName(Server.class.getPackage().getName() + ":service=ForkJoinPool,name=WorkerPool");
	 /** The channel group pool JMX ObjectName */
	public static final ObjectName CGROUP_POOL_ON = JMXHelper.objectName(Server.class.getPackage().getName() + ":service=ForkJoinPool,name=ChannelGroupPool");
	 
	/**
	 * Acquires and returns the Server singleton
	 * @return the Server singleton
	 */
	public static Server getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Server();
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Creates a new Server
	 */
	private Server() {
		log.info("Configuring Netty Server....");
		
		String serverLevel = ConfigurationHelper.getSystemThenEnvProperty(Constants.CONF_NETTY_SERVER_LOGLEVEL, Constants.DEFAULT_NETTY_SERVER_LOGLEVEL);
		loggingHandler = new LoggingHandler(getClass(), LogLevel.valueOf(serverLevel.trim().toUpperCase()));
		iface = ConfigurationHelper.getSystemThenEnvProperty(Constants.CONF_NETTY_IFACE, Constants.DEFAULT_NETTY_IFACE);
		port = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.CONF_NETTY_PORT, Constants.DEFAULT_NETTY_PORT);
		int bossThreads = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.CONF_NETTY_BOSS_THREADS, Constants.DEFAULT_NETTY_BOSS_THREADS);
		int workerThreads = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.CONF_NETTY_WORKER_THREADS, Constants.DEFAULT_NETTY_WORKER_THREADS);
		int groupThreads = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.CONF_NETTY_CGROUP_THREADS, Constants.DEFAULT_NETTY_CGROUP_THREADS);
		bossPool = new ManagedDefaultExecutorServiceFactory("bossPool").newExecutorService(bossThreads);
//		ForkJoinPoolManager.register(bossPool, BOSS_POOL_ON);
		workerPool = new ManagedDefaultExecutorServiceFactory("workerPool").newExecutorService(workerThreads);
//		ForkJoinPoolManager.register(workerPool, WORKER_POOL_ON);
		channelGroupPool = new ManagedDefaultExecutorServiceFactory("groupPool").newExecutorService(groupThreads);
//		ForkJoinPoolManager.register(channelGroupPool, CGROUP_POOL_ON);
		bossGroup = new NioEventLoopGroup(bossThreads, bossPool, selectorProvider);
		workerGroup = new NioEventLoopGroup(bossThreads, workerPool, selectorProvider);
		bootStrap = new ServerBootstrap();
		groupExecutor = new DefaultEventExecutor(channelGroupPool);
		channelGroup = new DefaultChannelGroup("TSDBLite", groupExecutor);
		MetricCache.getInstance(); // fire up the metric cache before we start taking calls	
		log.info("Selector: {}", selectorProvider.getClass().getName());
		bootStrap.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.handler(loggingHandler)
			.childHandler(this);
		try {
			serverChannel = (NioServerSocketChannel)bootStrap.bind(iface, port).sync().channel();
		} catch (Exception ex) {			
			stop();
			log.error("Failed to bind Netty server on [{}:{}]", iface, port, ex);
			throw new RuntimeException("Failed to bind Netty server", ex);
		}
		JMXHelper.registerMBean(this, OBJECT_NAME);
		log.info("\n\t======================================\n\tNetty Server started on [{}:{}]\n\t======================================", iface, port);
	}
	
	
	/**
	 * Stops the server and cleans up all resources
	 */
	public void stop() {		
		try { channelGroup.close().awaitUninterruptibly(); } catch (Exception x) {/* No Op */}
		try { bossGroup.shutdownGracefully().awaitUninterruptibly(); } catch (Exception x) {/* No Op */}
		try { workerGroup.shutdownGracefully().awaitUninterruptibly(); } catch (Exception x) {/* No Op */}
		try { groupExecutor.shutdownGracefully().awaitUninterruptibly(); } catch (Exception x) {/* No Op */}
		try { JMXHelper.unregisterMBean(BOSS_POOL_ON); } catch (Exception x) {/* No Op */}
		try { JMXHelper.unregisterMBean(WORKER_POOL_ON); } catch (Exception x) {/* No Op */}
		try { JMXHelper.unregisterMBean(CGROUP_POOL_ON); } catch (Exception x) {/* No Op */}
		log.info("\n\t======================================\n\tNetty Server Stopped\n\t======================================");
		instance = null;
	}
	
	/**
	 * {@inheritDoc}
	 * @see io.netty.channel.ChannelInitializer#initChannel(io.netty.channel.Channel)
	 */
	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		createdChannels.increment();
		channelGroup.add(ch);		
		ch.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
			@Override
			public void operationComplete(Future<? super Void> future) throws Exception {
				log.info("\n\t==============================\n\tChannel Closed [{}]\n\t==============================", ch.id());
//				log.error("Close Back trace", new Exception());
//				if(future.cause()!=null) {
//					log.error("Close fail", future.cause());					
//				}
			}
		});
		ch.pipeline().addLast("IdleState", new IdleStateHandler(0, 0, 60));
		ch.pipeline().addLast("ProtocolSwitch", new ProtocolSwitch());
	}
	
	/**
	 * {@inheritDoc}
	 * @see io.netty.channel.ChannelHandlerAdapter#userEventTriggered(io.netty.channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			log.info("\n\t ****************** Idle Event on Channel [{}]", ctx.channel().id().asShortText());
		}
		super.userEventTriggered(ctx, evt);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.ServerMXBean#getCreatedChannels()
	 */
	@Override
	public long getCreatedChannels() {
		return createdChannels.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.ServerMXBean#getCurrentChannels()
	 */
	@Override
	public int getCurrentChannels() {
		return channelGroup.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.ServerMXBean#getSelector()
	 */
	@Override
	public String getSelector() {
		return selectorProvider.getClass().getName();
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.ServerMXBean#getPort()
	 */
	@Override
	public int getPort() {
		return port;
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.ServerMXBean#getIface()
	 */
	@Override
	public String getIface() {
		return iface;
	}

}
