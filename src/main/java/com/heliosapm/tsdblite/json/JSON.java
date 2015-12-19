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
package com.heliosapm.tsdblite.json;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.heliosapm.tsdblite.metric.Trace;
import com.heliosapm.tsdblite.metric.Trace.TraceArrayDeserializer;
import com.heliosapm.tsdblite.metric.Trace.TraceDeserializer;
import com.heliosapm.tsdblite.metric.Trace.TraceSerializer;
import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: JSON</p>
 * <p>Description: JSON Utilities</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.tsdblite.json.JSON</code></p>
 */

public class JSON {
	public static final Charset UTF8 = Charset.forName("UTF8");
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
	static {
		jsonMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
		registerSerializer(Trace.class, new TraceSerializer());
		registerDeserializer(Trace.class, new TraceDeserializer());
		
		registerDeserializer(Trace.class, new TraceDeserializer());		
		registerDeserializer(Trace[].class, new TraceArrayDeserializer());
	}
	
	
	public static Map<String, String> from(final JsonNode node) {
		if(node==null) return EMPTY_MAP;
		final int size = node.size();
		if(size==0) return EMPTY_MAP;
		final Map<String, String> map = new HashMap<String, String>(node.size());
		for(Iterator<String> iter = node.fieldNames(); iter.hasNext();) {
			final String key = iter.next();
			final JsonNode v = node.get(key);
			if(v.isTextual()) {
				map.put(key, v.textValue());
			}
		}
		return map;
	}
	
	/**
	 * Registers a deser for the passed class
	 * @param clazz The class for which a deser is being registered
	 * @param deser The deserializer
	 */
	public static <T> void registerDeserializer(final Class<T> clazz, final JsonDeserializer<T> deser) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");		
		if(deser==null) throw new IllegalArgumentException("The passed deserializer for [" + clazz.getName() + "] was null");
		final SimpleModule module = new SimpleModule();
		module.addDeserializer(clazz, deser);
		jsonMapper.registerModule(module);					
	}
	
	/**
	 * Registers a ser for the passed class
	 * @param clazz The class for which a ser is being registered
	 * @param ser The serializer
	 */
	public static <T> void registerSerializer(final Class<T> clazz, final JsonSerializer<T> ser) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		if(ser==null) throw new IllegalArgumentException("The passed serializer for [" + clazz.getName() + "] was null");
		final SimpleModule module = new SimpleModule();
		module.addSerializer(clazz, ser);
		jsonMapper.registerModule(module);					
	}
	
	
	/**
	 * Registers a ser/deser pair for the passed class
	 * @param clazz The class for which a ser/deser pair is being registered
	 * @param deser The deserializer
	 * @param ser The serializer
	 */
	public static <T> void registerSerialization(final Class<T> clazz, final JsonDeserializer<T> deser, final JsonSerializer<T> ser) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		if(ser==null) throw new IllegalArgumentException("The passed serializer for [" + clazz.getName() + "] was null");		
		if(deser==null) throw new IllegalArgumentException("The passed deserializer for [" + clazz.getName() + "] was null");
		final SimpleModule module = new SimpleModule();
		int added = 0;
		module.addSerializer(clazz, ser);
		module.addDeserializer(clazz, deser);
		if(added>0)	jsonMapper.registerModule(module);		
	}
	
	/**
	 * Registers a module with the shared json mapper
	 * @param module The module to register
	 */
	public static void registerModule(final Module module) {
		if(module==null) throw new IllegalArgumentException("The passed module was null");
		jsonMapper.registerModule(module);
	}

	/**
	 * Deserializes a JSON formatted string to a specific class type
	 * <b>Note:</b> If you get mapping exceptions you may need to provide a 
	 * TypeReference
	 * @param json The string to deserialize
	 * @param pojo The class type of the object used for deserialization
	 * @return An object of the {@code pojo} type
	 * @throws IllegalArgumentException if the data or class was null or parsing 
	 * failed
	 * @throws JSONException if the data could not be parsed
	 */
	public static final <T> T parseToObject(final String json, final Class<T> pojo) {
		if (json == null || json.isEmpty())
			throw new IllegalArgumentException("Incoming data was null or empty");
		if (pojo == null)
			throw new IllegalArgumentException("Missing class type");

		try {
			return jsonMapper.readValue(json, pojo);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (JsonMappingException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}
	
	/**
	 * Deserializes a JSON/UTF8 formatted byte array to a specific class type
	 * <b>Note:</b> If you get mapping exceptions you may need to provide a 
	 * TypeReference
	 * @param json The buffer to deserialize from
	 * @param pojo The class type of the object used for deserialization
	 * @return An object of the {@code pojo} type
	 * @throws IllegalArgumentException if the data or class was null or parsing 
	 * failed
	 * @throws JSONException if the data could not be parsed
	 */
	public static final <T> T parseToObject(final ByteBuf json, final Class<T> pojo) {
		return parseToObject(json, pojo, UTF8);
	}
	
	
	/**
	 * Deserializes a JSON formatted byte array to a specific class type
	 * <b>Note:</b> If you get mapping exceptions you may need to provide a 
	 * TypeReference
	 * @param json The buffer to deserialize from
	 * @param pojo The class type of the object used for deserialization
	 * @param charset The character set of the content type in the buffer
	 * @return An object of the {@code pojo} type
	 * @throws IllegalArgumentException if the data or class was null or parsing 
	 * failed
	 * @throws JSONException if the data could not be parsed
	 */
	public static final <T> T parseToObject(final ByteBuf json, final Class<T> pojo, final Charset charset) {
		if (json == null)
			throw new IllegalArgumentException("Incoming buffer was null");
		if (pojo == null)
			throw new IllegalArgumentException("Missing class type");
		if(charset==null) throw new IllegalArgumentException("Missing charset");
		InputStream is = new ByteBufInputStream(json);
		InputStreamReader isr = new InputStreamReader(is, charset);
		try {
			return jsonMapper.readValue(isr, pojo);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (JsonMappingException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
			if(isr!=null) try { isr.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	
	/**
	 * Deserializes a JSON formatted ByteBuf to a JsonNode
	 * @param json The buffer to deserialize from
	 * @param charset The optional character set of the content type in the buffer which defaults to UTF8
	 * @return The parsed JsonNode
	 * @throws IllegalArgumentException if buffer was null
	 * @throws JSONException if the data could not be parsed
	 */
	public static final JsonNode parseToNode(final ByteBuf json, final Charset charset) {
		if (json == null)
			throw new IllegalArgumentException("Incoming buffer was null");
		InputStream is = new ByteBufInputStream(json);
		InputStreamReader isr = new InputStreamReader(is, charset==null ? UTF8 : charset);
		try {
			return jsonMapper.readTree(is);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (JsonMappingException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
			if(isr!=null) try { isr.close(); } catch (Exception x) {/* No Op */}
		}
	}

	/**
	 * Deserializes a JSON formatted ByteBuf to a JsonNode
	 * @param json The buffer to deserialize from which is assumed to be UTF8
	 * @return The parsed JsonNode
	 * @throws IllegalArgumentException if buffer was null
	 * @throws JSONException if the data could not be parsed
	 */
	public static final JsonNode parseToNode(final ByteBuf json) {
		return parseToNode(json, UTF8);
	}
	
	
	
	/**
	 * Deserializes a JSON node to a specific class type
	 * <b>Note:</b> If you get mapping exceptions you may need to provide a 
	 * TypeReference
	 * @param json The node to deserialize
	 * @param pojo The class type of the object used for deserialization
	 * @return An object of the {@code pojo} type
	 * @throws IllegalArgumentException if the data or class was null or parsing 
	 * failed
	 * @throws JSONException if the data could not be parsed
	 */
	public static final <T> T parseToObject(final JsonNode json, final Class<T> pojo) {
		if (json == null)
			throw new IllegalArgumentException("Incoming data was null or empty");
		if (pojo == null)
			throw new IllegalArgumentException("Missing class type");
		return jsonMapper.convertValue(json, pojo);		
	}
	
	
	/**
	 * Parses the passed stringy and returns the resulting JsonNode
	 * @param cs the stringy to parse
	 * @return the JsonNode
	 */
	public static JsonNode parse(final CharSequence cs) {
		if(cs==null) throw new IllegalArgumentException("The passed CharSequence was null or empty");
		try {
			return jsonMapper.readTree(cs.toString().trim());
		} catch (Exception ex) {
			throw new JSONException(ex);
		}
	}
	
	/**
	 * Returns a json generator that writes to the passed output stream
	 * @param os The output sream to write to
	 * @return the generator
	 */
	public static JsonGenerator generatorFor(final OutputStream os) {
		if(os==null) throw new IllegalArgumentException("The passed OutputStream was null");
		try {
			return getFactory().createGenerator(os);
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}


	/**
	 * Deserializes a JSON formatted byte array to a specific class type
	 * <b>Note:</b> If you get mapping exceptions you may need to provide a 
	 * TypeReference
	 * @param json The byte array to deserialize
	 * @param pojo The class type of the object used for deserialization
	 * @return An object of the {@code pojo} type
	 * @throws IllegalArgumentException if the data or class was null or parsing 
	 * failed
	 * @throws JSONException if the data could not be parsed
	 */
	public static final <T> T parseToObject(final byte[] json,
			final Class<T> pojo) {
		if (json == null)
			throw new IllegalArgumentException("Incoming data was null");
		if (pojo == null)
			throw new IllegalArgumentException("Missing class type");
		try {
			return jsonMapper.readValue(json, pojo);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (JsonMappingException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}
	

	/**
	 * Deserializes a JSON formatted string to a specific class type
	 * @param json The string to deserialize
	 * @param type A type definition for a complex object
	 * @return An object of the {@code pojo} type
	 * @throws IllegalArgumentException if the data or type was null or parsing
	 * failed
	 * @throws JSONException if the data could not be parsed
	 */
	@SuppressWarnings("unchecked")
	public static final <T> T parseToObject(final String json,
			final TypeReference<T> type) {
		if (json == null || json.isEmpty())
			throw new IllegalArgumentException("Incoming data was null or empty");
		if (type == null)
			throw new IllegalArgumentException("Missing type reference");
		try {
			return (T)jsonMapper.readValue(json, type);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (JsonMappingException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}

	/**
	 * Deserializes a JSON formatted byte array to a specific class type
	 * @param json The byte array to deserialize
	 * @param type A type definition for a complex object
	 * @return An object of the {@code pojo} type
	 * @throws IllegalArgumentException if the data or type was null or parsing
	 * failed
	 * @throws JSONException if the data could not be parsed
	 */
	@SuppressWarnings("unchecked")
	public static final <T> T parseToObject(final byte[] json,
			final TypeReference<T> type) {
		if (json == null)
			throw new IllegalArgumentException("Incoming data was null");
		if (type == null)
			throw new IllegalArgumentException("Missing type reference");
		try {
			return (T)jsonMapper.readValue(json, type);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (JsonMappingException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}

	/**
	 * Parses a JSON formatted string into raw tokens for streaming or tree
	 * iteration
	 * <b>Warning:</b> This method can parse an invalid JSON object without
	 * throwing an error until you start processing the data
	 * @param json The string to parse
	 * @return A JsonParser object to be used for iteration
	 * @throws IllegalArgumentException if the data was null or parsing failed
	 * @throws JSONException if the data could not be parsed
	 */
	public static final JsonParser parseToStream(final String json) {
		if (json == null || json.isEmpty())
			throw new IllegalArgumentException("Incoming data was null or empty");
		try {
			return jsonMapper.getFactory().createParser(json);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}
	
	/**
	 * Parses the passed stringy into a JsonNode
	 * @param jsonStr The stringy to parse
	 * @return the parsed JsonNode
	 */
	public static JsonNode parseToNode(final CharSequence jsonStr) {
		if (jsonStr == null) throw new IllegalArgumentException("Incoming data was null");
		final String str = jsonStr.toString().trim();
		if (str.isEmpty()) throw new IllegalArgumentException("Incoming data was empty");
		try {
			return jsonMapper.readTree(str);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		}		
	}
	
	
	/**
	 * Parses a JSON formatted byte array into raw tokens for streaming or tree
	 * iteration
	 * <b>Warning:</b> This method can parse an invalid JSON object without
	 * throwing an error until you start processing the data
	 * @param json The byte array to parse
	 * @return A JsonParser object to be used for iteration
	 * @throws IllegalArgumentException if the data was null or parsing failed
	 * @throws JSONException if the data could not be parsed
	 */
	public static final JsonParser parseToStream(final byte[] json) {
		if (json == null)
			throw new IllegalArgumentException("Incoming data was null");
		try {
			return jsonMapper.getFactory().createParser(json);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}

	/**
	 * Parses a JSON formatted inputs stream into raw tokens for streaming or tree
	 * iteration
	 * <b>Warning:</b> This method can parse an invalid JSON object without
	 * throwing an error until you start processing the data
	 * @param json The input stream to parse
	 * @return A JsonParser object to be used for iteration
	 * @throws IllegalArgumentException if the data was null or parsing failed
	 * @throws JSONException if the data could not be parsed
	 */
	public static final JsonParser parseToStream(final InputStream json) {
		if (json == null)
			throw new IllegalArgumentException("Incoming data was null");
		try {
			return jsonMapper.getFactory().createParser(json);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}

	/**
	 * Serializes the given object to a JSON string
	 * @param object The object to serialize
	 * @return A JSON formatted string
	 * @throws IllegalArgumentException if the object was null
	 * @throws JSONException if the object could not be serialized
	 */
	public static final String serializeToString(final Object object) {
		if (object == null)
			throw new IllegalArgumentException("Object was null");
		try {
			return jsonMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new JSONException(e);
		}
	}
	
	/**
	 * Converts the passed object to a JsonNode
	 * @param object The object to convert
	 * @return the resulting JsonNode
	 */
	public static final JsonNode serializeToNode(final Object object) {
		if (object == null)
			throw new IllegalArgumentException("Object was null");
		return jsonMapper.convertValue(object, JsonNode.class);
	}

	/**
	 * Serializes the given object to a JSON byte array
	 * @param object The object to serialize
	 * @return A JSON formatted byte array
	 * @throws IllegalArgumentException if the object was null
	 * @throws JSONException if the object could not be serialized
	 */
	public static final byte[] serializeToBytes(final Object object) {
		if (object == null)
			throw new IllegalArgumentException("Object was null");
		try {
			return jsonMapper.writeValueAsBytes(object);
		} catch (JsonProcessingException e) {
			throw new JSONException(e);
		}
	}
	
	public static void serialize(final Object object, final OutputStream os) {
		if (object == null) throw new IllegalArgumentException("Object was null");
		if (os == null) throw new IllegalArgumentException("OutputStream was null");
		try {
			jsonMapper.writeValue(os, object);
			os.flush();
		} catch (Exception ex) {
			throw new JSONException(ex);
		}
	}
	
	
	/**
	 * Serializes the passed object as JSON into the passed buffer
	 * @param obj The object to serialize
	 * @param buf The buffer to serialize into. If null, a new buffer is created
	 * @return The buffer the object was written to
	 */
	public static ByteBuf serializeToBuf(final Object obj, final ByteBuf buf) {
		if(obj==null) throw new IllegalArgumentException("The passed object was null");
		final ByteBuf b = buf==null ? Unpooled.directBuffer(2048) : buf;
		OutputStream os = null;
		try {
			os = new ByteBufOutputStream(b);
			serialize(obj, os);
			os.flush();
			return b;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to bufferize serialized object", ex);
		} finally {
			if(os!=null) try { os.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Serializes the passed object as JSON into a newly created buffer
	 * @param obj The object to serialize
	 * @return The buffer the object was written to
	 */
	public static ByteBuf serializeToBuf(final Object obj) {
		return serializeToBuf(obj, null);
	}
	
	
	/**
	 * Serializes the passed object and pipes back the results in an InputStream to read it back.
	 * Spawns a thread to run the pipe out so the calling thread only needs to read the returned input stream.
	 * If the serialization fails, the worker thread will close the inoput stream to signal the failure.
	 * @param obj The object to serialize
	 * @return an InputStream to read back the JSON serialized object 
	 */
	public static InputStream serializeLoopBack(final Object obj) {
		if(obj==null) throw new IllegalArgumentException("The passed object was null");
		try {
			final PipedInputStream pin = new PipedInputStream(2048);
			final PipedOutputStream pout = new PipedOutputStream(pin);
			final Thread t = new Thread("serializeLoopBackThread") {
				@Override
				public void run() {
					try {
						serialize(obj, pout);
					} catch (Exception ex) {
						try { pin.close(); } catch (Exception x) {/* No Op */}
					}
				}
			};
			t.setDaemon(true);
			t.start();			
			return pin;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to pipe serialized object", ex);
		}
	}
	

	/**
	 * Serializes the given object and wraps it in a callback function
	 * i.e. &lt;callback&gt;(&lt;json&gt)
	 * Note: This will not append a trailing semicolon
	 * @param callback The name of the Javascript callback to prepend
	 * @param object The object to serialize
	 * @return A JSONP formatted string
	 * @throws IllegalArgumentException if the callback method name was missing 
	 * or object was null
	 * @throws JSONException if the object could not be serialized
	 */
	public static final String serializeToJSONPString(final String callback,
			final Object object) {
		if (callback == null || callback.isEmpty())
			throw new IllegalArgumentException("Missing callback name");
		if (object == null)
			throw new IllegalArgumentException("Object was null");
		try {
			return jsonMapper.writeValueAsString(new JSONPObject(callback, object));
		} catch (JsonProcessingException e) {
			throw new JSONException(e);
		}
	}

	/**
	 * Serializes the given object and wraps it in a callback function
	 * i.e. &lt;callback&gt;(&lt;json&gt)
	 * Note: This will not append a trailing semicolon
	 * @param callback The name of the Javascript callback to prepend
	 * @param object The object to serialize
	 * @return A JSONP formatted byte array
	 * @throws IllegalArgumentException if the callback method name was missing 
	 * or object was null
	 * @throws JSONException if the object could not be serialized
	 * @throws IOException Thrown when there was an issue reading the object
	 */
	public static final byte[] serializeToJSONPBytes(final String callback,
			final Object object) {
		if (callback == null || callback.isEmpty())
			throw new IllegalArgumentException("Missing callback name");
		if (object == null)
			throw new IllegalArgumentException("Object was null");
		try {
			return jsonMapper.writeValueAsBytes(new JSONPObject(callback, object));
		} catch (JsonProcessingException e) {
			throw new JSONException(e);
		}
	}

	/**
	 * Returns a reference to the static ObjectMapper
	 * @return The ObjectMapper
	 */
	public final static ObjectMapper getMapper() {
		return jsonMapper;
	}

	/**
	 * Returns a reference to the JsonFactory for streaming creation
	 * @return The JsonFactory object
	 */
	public final static JsonFactory getFactory() {
		return jsonMapper.getFactory();
	}
	
	/**
	 * Returns a shareable node factory
	 * @return a json node factory
	 */
	public static JsonNodeFactory getNodeFactory() {
		return JsonNodeFactory.instance;
	}
	
	
	/**
	 * <p>Title: ObjectNameSerializer</p>
	 * <p>Description: Built in JSON serializer for {@link ObjectName}s</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.nash.util.JSON.ObjectNameSerializer</code></p>
	 */
	public static class ObjectNameSerializer extends JsonSerializer<ObjectName> {
		@Override
		public void serialize(final ObjectName value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonProcessingException {
			if(value==null) {
				jgen.writeNull();
			} else {
				jgen.writeString(value.toString());
			}			
		}		
	}
	
	/**
	 * <p>Title: ObjectNameDeserializer</p>
	 * <p>Description: Built in JSON deserializer for {@link ObjectName}s</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.nash.util.JSON.ObjectNameDeserializer</code></p>
	 */
	public static class ObjectNameDeserializer extends JsonDeserializer<ObjectName> {

		@Override
		public ObjectName deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final String s = jp.getText();
			return (s==null || s.trim().isEmpty()) ? null : JMXHelper.objectName(s.trim());
		}
		
	}
	
	/**
	 * <p>Title: TabularDataSerializer</p>
	 * <p>Description: Built in JSON serializer for {@link TabularData}s</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.nash.util.JSON.TabularDataSerializer</code></p>
	 */
	public static class TabularDataSerializer extends JsonSerializer<TabularData> {
		@Override
		public void serialize(final TabularData value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonProcessingException {
			if(value==null) {
				jgen.writeNull();
			} else {
				jgen.writeBinary(jserialize((Serializable) value));
				
				//jgen.writeString(value.toString());
			}			
		}		
	}
	
	/**
	 * <p>Title: TabularDataDeserializer</p>
	 * <p>Description: Built in JSON deserializer for {@link TabularData}s</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.nash.util.JSON.TabularDataDeserializer</code></p>
	 */
	public static class TabularDataDeserializer extends JsonDeserializer<TabularData> {

		@Override
		public TabularData deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			return null;
		}

	}
	

	private JSON() {
	}
	
	
	private static byte[] jserialize(final Serializable ser) {
		if(ser==null) return new byte[0];
		ByteArrayOutputStream baos =  null;
		ObjectOutputStream oos = null;
		try {
			baos = new ByteArrayOutputStream(1024);
			oos = new ObjectOutputStream(baos);
			oos.writeObject(ser);
			oos.flush();
			baos.flush();
			return baos.toByteArray();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			if(oos!=null) try { oos.close(); } catch (Exception x) {/* No Op */}
			if(baos!=null) try { baos.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	private static Object jdeserialize(final byte[] bytes) {
		if(bytes==null || bytes.length==0) return null;
		ByteArrayInputStream bais =  null;
		ObjectInputStream ois = null;
		try {
			bais = new ByteArrayInputStream(bytes);
			ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			if(ois!=null) try { ois.close(); } catch (Exception x) {/* No Op */}
			if(bais!=null) try { bais.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	

}
