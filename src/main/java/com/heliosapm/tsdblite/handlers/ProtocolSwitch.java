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
package com.heliosapm.tsdblite.handlers;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdblite.Constants;
import com.heliosapm.tsdblite.Server;
import com.heliosapm.tsdblite.handlers.http.HttpStaticFileServerHandler;
import com.heliosapm.tsdblite.handlers.text.StringArrayTraceDecoder;
import com.heliosapm.tsdblite.handlers.text.WordSplitter;
import com.heliosapm.tsdblite.handlers.websock.WebSocketServerHandler;
import com.heliosapm.tsdblite.jmx.ManagedDefaultExecutorServiceFactory;
import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.jmx.JMXHelper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;

/**
 * <p>Title: ProtocolSwitch</p>
 * <p>Description: Channel handler to sniff out the first few received bytes of a new connection and 
 * modify the pipeline to handle the discovered protocol accordingly. This is copied largely from the 
 * <a href="http://netty.io/5.0/xref/io/netty/example/portunification/PortUnificationServerHandler.html">PortUnification</a> example from Netty.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.ProtocolSwitch</code></p>
 */

public class ProtocolSwitch extends ByteToMessageDecoder {
	/** Indicates if we should be checking for GZip */
	private final boolean detectGzip;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** Executor service factory */
	protected static final DefaultExecutorServiceFactory executorServiceFactory = new DefaultExecutorServiceFactory(ProtocolSwitch.class);
	/** The netty channel group thread pool */
	protected static final ExecutorService eventPool;
	/** The event executor */
	protected static final DefaultEventExecutorGroup eventExecutorGroup; 

	
    private static final StringEncoder PLAINTEXT_ENCODER = new StringEncoder();
    private static final WordSplitter PLAINTEXT_DECODER = new WordSplitter();
    private static final StringArrayTraceDecoder TRACE_DECODER = new StringArrayTraceDecoder();
    /** The child channel logging handler */
    @SuppressWarnings("unused")
	private static final LoggingHandler loggingHandler = new LoggingHandler(ProtocolSwitch.class, LogLevel.INFO); 	
    
	 /** The event pool JMX ObjectName */
	public static final ObjectName EVENT_POOL_ON = JMXHelper.objectName(Server.class.getPackage().getName() + ":service=ForkJoinPool,name=EventPool");

    
	static {
		final int eventThreads = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.CONF_NETTY_EVENT_THREADS, Constants.DEFAULT_NETTY_EVENT_THREADS);
		eventPool = new ManagedDefaultExecutorServiceFactory("eventPool").newExecutorService(eventThreads);
		eventExecutorGroup = new DefaultEventExecutorGroup(eventThreads, eventPool);
		HttpStaticFileServerHandler.getInstance();
//		ManagedForkJoinPool.register(eventPool, EVENT_POOL_ON);		
	}


	
	/**
	 * Creates a new ProtocolSwitch
	 * @param detectGzip true to enable gzip detection, false otherwise
	 */
	public ProtocolSwitch(final boolean detectGzip) {
		this.detectGzip = detectGzip;
	}

	/**
	 * Creates a new ProtocolSwitch with gzip detection turned on
	 */
	public ProtocolSwitch() {
		this(true);
	}

	/**
	 * {@inheritDoc}
	 * @see io.netty.handler.codec.ByteToMessageDecoder#decode(io.netty.channel.ChannelHandlerContext, io.netty.buffer.ByteBuf, java.util.List)
	 */
	@Override
	protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
		// Will use the first five bytes to detect a protocol.
		if (in.readableBytes() < 5) {
			log.info("No ProtocolSwitch. Bytes: {}", in.readableBytes());
			return;
		}
        final int magic1 = in.getUnsignedByte(in.readerIndex());
        final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
        if (detectGzip && isGzip(magic1, magic2)) {
            enableGzip(ctx);
            log.info("Enabled GZIp on channel [{}]", ctx.channel().id().asShortText());
        } else if (isHttp(magic1, magic2)) {
            switchToHttp(ctx);
            log.info("Switched to HTTP on channel [{}]", ctx.channel().id().asShortText());
        } else if (isText(magic1, magic2)) {
        	switchToPlainText(ctx);
        	log.info("Switched to PlainText on channel [{}]", ctx.channel().id().asShortText());
        } else {
        	log.error("No protocol recognized on [{}]", ctx.channel().id().asLongText());
            in.clear();
            ctx.close();
        }        	
	}
	
    private void enableGzip(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("gzipdeflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        p.addLast("gzipinflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        p.addLast("2ndPhaseSwitch", new ProtocolSwitch(false));
        p.remove(this);
        log.info("enabled gzip: [{}]", ctx.channel().id());
    }

    private void switchToHttp(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();    

        //p.addLast("logging", loggingHandler);
//        p.addLast(new HttpObjectAggregator(1048576));
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        p.addLast("httpCodec", sourceCodec);
//        HttpServerUpgradeHandler.UpgradeCodec upgradeCodec = new Http2ServerUpgradeCodec(new HelloWorldHttp2Handler());
//        HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, Collections.singletonList(upgradeCodec), 65536);
//        p.addLast("http2Upgrader", upgradeHandler);		          
        
//        p.addLast("encoder", new HttpResponseEncoder());
//        p.addLast("decoder", new HttpRequestDecoder());
//        p.addLast("deflater", new HttpContentCompressor(1));
//        p.addLast("inflater", new HttpContentDecompressor());
        p.addLast(new HttpObjectAggregator(1048576 * 2));
        
        //p.addLast("logging", loggingHandler);
        
        
        //p.addLast("encoder", new HttpResponseEncoder());
        
        
        //p.addLast("logging", loggingHandler);
        p.addLast("logging", loggingHandler);
        //WebSocketServerHandler
        p.addLast(eventExecutorGroup, "requestManager", new WebSocketServerHandler());
//        p.addLast(eventExecutorGroup, "requestManager", HttpRequestManager.getInstance());
//        p.addLast("requestManager", HttpRequestManager.getInstance());        
        p.remove(this);
    }
    

    
    private void switchToPlainText(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("framer", new LineBasedFrameDecoder(1024));
        p.addLast("encoder", PLAINTEXT_ENCODER);
        p.addLast("decoder", PLAINTEXT_DECODER);
        p.addLast("traceDecoder", TRACE_DECODER);        
        p.remove(this);    	
        log.info("switched to plain text: [{}]", ctx.channel().id());
    }
	
    /**
     * {@inheritDoc}
     * @see io.netty.channel.ChannelHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)
     */
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable t) throws Exception {    	
    	log.error("Uncaught exception", t);
    	//super.exceptionCaught(ctx, t);
    }
	
	
    /**
     * Examines the passed unsigned bytes to see if they match the GZip signature
     * @param magic1 The first unsigned byte in the data to test
     * @param magic2 The second unsigned byte in the data to test
     * @return true if the signature matches gzip, false otherwise
     */
    public boolean isGzip(final int magic1, final int magic2) {
        if (detectGzip) {
            return magic1 == 31 && magic2 == 139;
        }
        return false;
    }

    /**
     * Examines the passed unsigned bytes to see if they match a possible HTTP request
     * @param magic1 The first unsigned byte in the data to test
     * @param magic2 The second unsigned byte in the data to test
     * @return true if the signature matches an HTTP request, false otherwise
     */
    public static boolean isHttp(final int magic1, final int magic2) {
        return
            magic1 == 'G' && magic2 == 'E' || // GET
            magic1 == 'P' && magic2 == 'O' || // POST
            magic1 == 'P' && magic2 == 'U' || // PUT
            magic1 == 'H' && magic2 == 'E' || // HEAD
            magic1 == 'O' && magic2 == 'P' || // OPTIONS
            magic1 == 'P' && magic2 == 'A' || // PATCH
            magic1 == 'D' && magic2 == 'E' || // DELETE
            magic1 == 'T' && magic2 == 'R' || // TRACE
            magic1 == 'C' && magic2 == 'O';   // CONNECT
    }	
    
    /**
     * Examines the passed unsigned bytes to see if they match a possible plain text request
     * @param magic1 The first unsigned byte in the data to test
     * @param magic2 The second unsigned byte in the data to test
     * @return true if the signature matches a plain text request, false otherwise
     */
    public static boolean isText(final int magic1, final int magic2) {
    	if(isHttp(magic1, magic2)) return false;
    	return (magic1 >= 32 && magic1 <= 126 && magic2 >= 32 && magic2 <= 126);
    }
}
