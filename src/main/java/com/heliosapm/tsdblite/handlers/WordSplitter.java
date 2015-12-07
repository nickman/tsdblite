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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * <p>Title: WordSplitter</p>
 * <p>Description: Accepts a line from the plain text input and splits it into a String array.
 * Based on OpenTSDB's WordSplitter.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.handlers.WordSplitter</code></p>
 */
@Sharable
public class WordSplitter extends MessageToMessageDecoder<ByteBuf> {
	
	private static final Charset CHARSET = Charset.forName("ISO-8859-1");
	

	/**
	 * {@inheritDoc}
	 * @see io.netty.handler.codec.MessageToMessageDecoder#decode(io.netty.channel.ChannelHandlerContext, java.lang.Object, java.util.List)
	 */
	@Override
	protected void decode(final ChannelHandlerContext ctx, final ByteBuf msg, final List<Object> out) throws Exception {
		out.add(splitString(msg.toString(CHARSET), ' '));		
	}
	
	  /**
	   * Optimized version of {@code String#split} that doesn't use regexps.
	   * This function works in O(5n) where n is the length of the string to
	   * split.
	   * @param s The string to split.
	   * @param c The separator to use to split the string.
	   * @return A non-null, non-empty array.
	   */
	  public static String[] splitString(final String s, final char c) {
	    final char[] chars = s.toCharArray();
	    int num_substrings = 1;
	    for (final char x : chars) {
	      if (x == c) {
	        num_substrings++;
	      }
	    }
	    final String[] result = new String[num_substrings];
	    final int len = chars.length;
	    int start = 0;  // starting index in chars of the current substring.
	    int pos = 0;    // current index in chars.
	    int i = 0;      // number of the current substring.
	    for (; pos < len; pos++) {
	      if (chars[pos] == c) {
	        result[i++] = new String(chars, start, pos - start);
	        start = pos + 1;
	      }
	    }
	    result[i] = new String(chars, start, pos - start);
	    return result;
	  }
	
	

}
