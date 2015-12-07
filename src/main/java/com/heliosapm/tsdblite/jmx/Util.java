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
package com.heliosapm.tsdblite.jmx;

import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import javax.management.ObjectName;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import com.heliosapm.tsdblite.metric.TagKeySorter;


/**
 * <p>Title: Util</p>
 * <p>Description: Some JMX utility functions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.jmx.Util</code></p>
 */

public abstract class Util {
	
	/** The UTF8 character set */
	public static final Charset UTF8 = Charset.forName("UTF8");
	/** The hashing function to compute hashes for ObjectNames */
	public static final HashFunction OBJECT_NAME_HASHER = Hashing.murmur3_128();
	

	/**
	 * <p>Title: ObjectNameFunnel</p>
	 * <p>Description: Funnel to compute deterministic hash codes for ObjectNames</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.tsdblite.jmx.ObjectNameFunnel</code></p>
	 */
	private static enum ObjectNameFunnel implements Funnel<ObjectName> {
	    /** The funnel instance */
	    INSTANCE;
			/**
			 * {@inheritDoc}
			 * @see com.google.common.hash.Funnel#funnel(java.lang.Object, com.google.common.hash.PrimitiveSink)
			 */
			@Override
			public void funnel(final ObjectName objectName, final PrimitiveSink into) {
		    	into.putString(objectName.getDomain(), UTF8);
		    	final Hashtable<String, String> props = objectName.getKeyPropertyList();
		    	if(!props.isEmpty()) {
		    		final TreeMap<String, String> tmap = new TreeMap<String, String>(TagKeySorter.INSTANCE);
		    		tmap.putAll(props);
			    	for(Map.Entry<String, String> entry: tmap.entrySet()) {
			    		into.putString(entry.getKey().trim(), UTF8)
			    		.putString(entry.getValue().trim(), UTF8);
			    	}		    		
		    	}
		    	if(objectName.isPropertyListPattern()) {
		    		into.putString("true", UTF8);
		    	}
			}
	  }
	
	/**
	 * Returns the long hash code for the passed ObjectName
	 * @param objectName The ObjectName to hash
	 * @return the long hash code
	 */
	public static long hashCode(final ObjectName objectName) {
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null");
		return OBJECT_NAME_HASHER.hashObject(objectName, ObjectNameFunnel.INSTANCE).padToLong();
	}	
}
