/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2015, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.tsdblite.handlers;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.DATE;
import static io.netty.handler.codec.http.HttpHeaderNames.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpHeaderNames.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdblite.Constants;
import com.heliosapm.utils.config.ConfigurationHelper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

/**
 * <p>Title: HttpStaticFileServerHandler</p>
 * <p>Description: HTTP Static content file handler.
 * Copied and modified from Netty examples library.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Netty Development Team
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.HttpStaticFileServerHandler</code></p>
 */
@Sharable
public class HttpStaticFileServerHandler extends HttpRequestHandler {
	/** The singleton instance */
	private static volatile HttpStaticFileServerHandler instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	

	/** The date format for cache headers */
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	/** The time zone for the cache header date values */
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    /** The configured cache period in seconds */
    public static final int HTTP_CACHE_SECONDS;
    /** Regex to match allowed file names that can be served */
    public static final Pattern ALLOWED_FILE_NAME; 
    /** Regex to match insecure files that should not be served */
    public static final Pattern INSECURE_URI;    
    /** The classpath resource prefix for provided static content */
    public static final String CONTENT_PREFIX = "www";
    

    /** Instance logger */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    /** The static content root directory name */
    protected final String staticRoot;
    /** The static content root directory file */
    protected final File staticRootDir;
    
    

    static {
    	HTTP_CACHE_SECONDS = ConfigurationHelper.getIntSystemThenEnvProperty(Constants.CONF_HTTP_CACHE_SECONDS, Constants.DEFAULT_HTTP_CACHE_SECONDS);
    	ALLOWED_FILE_NAME = Pattern.compile(ConfigurationHelper.getSystemThenEnvProperty(Constants.CONF_HTTP_ALLOWED_FILENAMES, Constants.DEFAULT_HTTP_ALLOWED_FILENAMES));
    	INSECURE_URI = Pattern.compile(ConfigurationHelper.getSystemThenEnvProperty(Constants.CONF_HTTP_INSECURE_FILENAMES, Constants.DEFAULT_HTTP_INSECURE_FILENAMES));    	
    }
    
	/**
	 * Acquires and returns the HttpStaticFileServerHandler singleton
	 * @return the HttpStaticFileServerHandler singleton
	 */
	public static HttpStaticFileServerHandler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new HttpStaticFileServerHandler();
				}
			}
		}
		return instance;
	}
    
    
    /**
	 * Creates a new HttpStaticFileServerHandler
	 */
	private HttpStaticFileServerHandler() {
		final String codeSourcePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		File file = new File(codeSourcePath);
		final boolean isJar = codeSourcePath.endsWith(".jar") && file.exists() && file.canRead();
		if(isJar) {
			staticRoot = ConfigurationHelper.getSystemThenEnvProperty(Constants.CONF_HTTP_CONTENT_ROOT, Constants.DEFAULT_HTTP_CONTENT_ROOT);
		} else {
			staticRoot = "./src/main/resources/www";
		}
		staticRootDir = new File(staticRoot);
		staticRootDir.mkdirs();
		if(!staticRootDir.exists() || !staticRootDir.isDirectory()) {
			log.error("Failed to create or find the static root directory [" + staticRoot + "]");
			throw new RuntimeException("Failed to create or find the static root directory [" + staticRoot + "]");
		}
		if(isJar) {
			loadContent(staticRootDir.getAbsolutePath());
		}
		log.info("\n\t============================================================\n\tStarted FileServer\n\tRoot: [{}]\n\t============================================================\n", staticRootDir.getAbsolutePath());
	}
	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.tsdblite.handlers.HttpRequestHandler#process(com.heliosapm.tsdblite.handlers.TSDBHttpRequest)
	 */
	@Override
	protected void process(final TSDBHttpRequest request) {
		try {
			messageReceived(request.context(), (FullHttpRequest)request.getRequest());
		} catch (Exception ex) {
			sendError(request.context(), BAD_REQUEST);
		}
	}
	
	
    private void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        if (request.method() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        final String uri = request.uri().replace("/api/s", "/");
        final String path = uri;
        if (path == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        File file = new File(staticRootDir, path);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return;
        }

        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                sendListing(ctx, file);
            } else {
                sendRedirect(ctx, uri + '/');
            }
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        // Cache Validation
        String ifModifiedSince = request.headers().getAndConvert(IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return;
            }
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        HttpHeaderUtil.setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        setDateAndCacheHeaders(response, file);
        if (HttpHeaderUtil.isKeepAlive(request)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture =
                    ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            sendFileFuture =
                    ctx.write(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
                            ctx.newProgressivePromise());
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
        }

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });

        // Decide whether to close the connection or not.
        if (!HttpHeaderUtil.isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        ctx.flush();
    }

//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        log.error("Caught unexpected exception", cause);
//        if (ctx.channel().isActive()) {
//            sendError(ctx, INTERNAL_SERVER_ERROR);
//        }
//    }

    

    private String sanitizeUri(final String xuri) {
    	String uri = xuri;
        // Decode the path.
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        // Convert file separators.
        //uri = uri.replace('/', File.separatorChar);

        // Simplistic dumb security check.
        // You will have to do something serious in the production environment.
        if (uri.contains(File.separator + '.') ||
            uri.contains('.' + File.separator) ||
            uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.' ||
            INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

        // Convert to absolute path.
        return staticRoot + "/" + uri;
    }

    

    private static void sendListing(ChannelHandlerContext ctx, File dir) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");

        String dirPath = dir.getPath();
        StringBuilder buf = new StringBuilder()
            .append("<!DOCTYPE html>\r\n")
            .append("<html><head><title>")
            .append("Listing of: ")
            .append(dirPath)
            .append("</title></head><body>\r\n")

            .append("<h3>Listing of: ")
            .append(dirPath)
            .append("</h3>\r\n")

            .append("<ul>")
            .append("<li><a href=\"../\">..</a></li>\r\n");

        for (File f: dir.listFiles()) {
            if (f.isHidden() || !f.canRead()) {
                continue;
            }

            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }

            buf.append("<li><a href=\"")
               .append(name)
               .append("\">")
               .append(name)
               .append("</a></li>\r\n");
        }

        buf.append("</ul></body></html>\r\n");
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
        response.headers().set(LOCATION, newUri);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     *
     * @param ctx
     *            Context
     */
    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response
     *            HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param file
     *            file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }
    
    
	/**
	 * Loads the Static UI content files from the classpath JAR to the configured static root directory
	 * @param the name of the content directory to write the content to
	 */
	private void loadContent(String contentDirectory) {	  
		File gpDir = new File(contentDirectory);
		final long startTime = System.currentTimeMillis();
		int filesLoaded = 0;
		int fileFailures = 0;
		int fileNewer = 0;
		long bytesLoaded = 0;
		String codeSourcePath = HttpStaticFileServerHandler.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		File file = new File(codeSourcePath);
		if( codeSourcePath.endsWith(".jar") && file.exists() && file.canRead() ) {
			JarFile jar = null;
			ByteBuf contentBuffer = Unpooled.directBuffer(300000); 					
			try {
				jar = new JarFile(file);
				final Enumeration<JarEntry> entries = jar.entries(); 
				while(entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					final String name = entry.getName();
					if (name.startsWith(CONTENT_PREFIX + "/")) { 
						final int contentSize = (int)entry.getSize();
						final long contentTime = entry.getTime();
						if(entry.isDirectory()) {
							new File(gpDir, name).mkdirs();
							continue;
						}
						File contentFile = new File(gpDir, name.replace(CONTENT_PREFIX + "/", ""));
						if( !contentFile.getParentFile().exists() ) {
							contentFile.getParentFile().mkdirs();
						}
						if( contentFile.exists() ) {
							if( contentFile.lastModified() >= contentTime ) {
								log.debug("File in directory was newer [{}]", name);
								fileNewer++;
								continue;
							}
							contentFile.delete();
						}
						log.debug("Writing content file [{}]", contentFile );
						contentFile.createNewFile();
						if( !contentFile.canWrite() ) {
							log.warn("Content file [{}] not writable", contentFile);
							fileFailures++;
							continue;
						}
						FileOutputStream fos = null;
						InputStream jis = null;
						try {
							fos = new FileOutputStream(contentFile);
							jis = jar.getInputStream(entry);
							contentBuffer.writeBytes(jis, contentSize);
							contentBuffer.readBytes(fos, contentSize);
							fos.flush();
							jis.close(); jis = null;
							fos.close(); fos = null;
							filesLoaded++;
							bytesLoaded += contentSize;
							log.debug("Wrote content file [{}] + with size [{}]", contentFile, contentSize );
						} finally {
							if( jis!=null ) try { jis.close(); } catch (Exception ex) {}
							if( fos!=null ) try { fos.close(); } catch (Exception ex) {}
						}
					}  // not content
				} // end of while loop
				final long elapsed = System.currentTimeMillis()-startTime;
				StringBuilder b = new StringBuilder("\n\n\t===================================================\n\tStatic Root Directory:[").append(contentDirectory).append("]");
				b.append("\n\tTotal Files Written:").append(filesLoaded);
				b.append("\n\tTotal Bytes Written:").append(bytesLoaded);
				b.append("\n\tFile Write Failures:").append(fileFailures);
				b.append("\n\tExisting File Newer Than Content:").append(fileNewer);
				b.append("\n\tElapsed (ms):").append(elapsed);
				b.append("\n\t===================================================\n");
				log.info(b.toString());
			} catch (Exception ex) {
				log.error("Failed to export ui content", ex);			  
			} finally {
				if( jar!=null ) try { jar.close(); } catch (Exception x) { /* No Op */}
			}
		}  else {	// end of was-not-a-jar
			log.warn("\n\tThe TSDBLite classpath is not a jar file, so there is no content to unload.\n\tBuild the OpenTSDB jar and run 'java -jar <jar> --d <target>'.");
		}
	}
    
}
