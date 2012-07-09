package com.google.gson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;

import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import com.stanfy.gsonxml.XmlParserCreator;
import com.stanfy.gsonxml.XmlReader;

/**
 * Wrapper for {@link Gson}.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public final class GsonXml {

  /** Core object. */
  private final Gson core;

  /** XML parser creator. */
  private final XmlParserCreator xmlParserCreator;

  /** Option. */
  private final boolean skipRoot, treatNamespaces, sameNameLists;

  GsonXml(final Gson gson, final XmlParserCreator xmlParserCreator, final boolean skipRoot, final boolean namespaces,
      final boolean sameNameLists) {
    if (xmlParserCreator == null) { throw new NullPointerException("XmlParserCreator is null"); }
    this.core = gson;
    this.xmlParserCreator = xmlParserCreator;
    this.skipRoot = skipRoot;
    this.treatNamespaces = namespaces;
    this.sameNameLists = sameNameLists;
  }

  /**
   * Returns the type adapter for {@code} type.
   * @throws IllegalArgumentException if this GSON cannot serialize and
   *     deserialize {@code type}.
   */
  public <T> TypeAdapter<T> getAdapter(final TypeToken<T> type) { return core.getAdapter(type); }

  /**
   * This method is used to get an alternate type adapter for the specified type. This is used
   * to access a type adapter that is overridden by a {@link TypeAdapterFactory} that you
   * may have registered. This features is typically used when you want to register a type
   * adapter that does a little bit of work but then delegates further processing to the Gson
   * default type adapter. Here is an example:
   * <p>Let's say we want to write a type adapter that counts the number of objects being read
   *  from or written to JSON. We can achieve this by writing a type adapter factory that uses
   *  the <code>getDelegateAdapter</code> method:
   *  <pre> {@code
   *  class StatsTypeAdapterFactory implements TypeAdapterFactory {
   *    public int numReads = 0;
   *    public int numWrites = 0;
   *    public &lt;T&gt; TypeAdapter&lt;T&gt; create(Gson gson, TypeToken&lt;T&gt; type) {
   *      final TypeAdapter&lt;T&gt; delegate = gson.getDelegateAdapter(this, type);
   *      return new TypeAdapter&lt;T&gt;() {
   *        public void write(JsonWriter out, T value) throws IOException {
   *          ++numWrites;
   *          delegate.write(out, value);
   *        }
   *        public T read(JsonReader in) throws IOException {
   *          ++numReads;
   *          return delegate.read(in);
   *        }
   *      };
   *    }
   *  }
   *  } </pre>
   *  This factory can now be used like this:
   *  <pre> {@code
   *  StatsTypeAdapterFactory stats = new StatsTypeAdapterFactory();
   *  Gson gson = new GsonBuilder().registerTypeAdapterFactory(stats).create();
   *  // Call gson.toJson() and fromJson methods on objects
   *  System.out.println("Num JSON reads" + stats.numReads);
   *  System.out.println("Num JSON writes" + stats.numWrites);
   *  }</pre>
   *  Note that since you can not override type adapter factories for String and Java primitive
   *  types, our stats factory will not count the number of String or primitives that will be
   *  read or written.
   * @param skipPast The type adapter factory that needs to be skipped while searching for
   *   a matching type adapter. In most cases, you should just pass <i>this</i> (the type adapter
   *   factory from where {@link getDelegateAdapter} method is being invoked).
   * @param type Type for which the delegate adapter is being searched for.
   *
   */
  public <T> TypeAdapter<T> getDelegateAdapter(final TypeAdapterFactory skipPast, final TypeToken<T> type) {
    return core.getDelegateAdapter(skipPast, type);
  }

  /**
   * Returns the type adapter for {@code} type.
   *
   * @throws IllegalArgumentException if this GSON cannot serialize and
   *     deserialize {@code type}.
   */
  public <T> TypeAdapter<T> getAdapter(final Class<T> type) { return core.getAdapter(type); }

  public <T> T fromXml(final String json, final Class<T> classOfT) throws JsonSyntaxException {
    final Object object = fromXml(json, (Type) classOfT);
    return Primitives.wrap(classOfT).cast(object);
  }

  @SuppressWarnings("unchecked")
  public <T> T fromXml(final String json, final Type typeOfT) throws JsonSyntaxException {
    if (json == null) {
      return null;
    }
    final StringReader reader = new StringReader(json);
    final T target = (T) fromXml(reader, typeOfT);
    return target;
  }

  public <T> T fromXml(final Reader json, final Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
    final XmlReader jsonReader = new XmlReader(json, xmlParserCreator, skipRoot, treatNamespaces, sameNameLists); // change reader
    final Object object = fromXml(jsonReader, classOfT);
    assertFullConsumption(object, jsonReader);
    return Primitives.wrap(classOfT).cast(object);
  }

  @SuppressWarnings("unchecked")
  public <T> T fromXml(final Reader json, final Type typeOfT) throws JsonIOException, JsonSyntaxException {
    final XmlReader jsonReader = new XmlReader(json, xmlParserCreator, skipRoot, treatNamespaces, sameNameLists); // change reader
    final T object = (T) fromXml(jsonReader, typeOfT);
    assertFullConsumption(object, jsonReader);
    return object;
  }

  private static void assertFullConsumption(final Object obj, final JsonReader reader) {
    try {
      if (obj != null && reader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonIOException("JSON document was not fully consumed.");
      }
    } catch (final MalformedJsonException e) {
      throw new JsonSyntaxException(e);
    } catch (final IOException e) {
      throw new JsonIOException(e);
    }
  }

  /**
   * Reads the next JSON value from {@code reader} and convert it to an object
   * of type {@code typeOfT}.
   * Since Type is not parameterized by T, this method is type unsafe and should be used carefully
   *
   * @throws JsonIOException if there was a problem writing to the Reader
   * @throws JsonSyntaxException if json is not a valid representation for an object of type
   */
  public <T> T fromXml(final XmlReader reader, final Type typeOfT) throws JsonIOException, JsonSyntaxException {
    return core.fromJson(reader, typeOfT);
  }

  @Override
  public String toString() { return core.toString(); }

}
