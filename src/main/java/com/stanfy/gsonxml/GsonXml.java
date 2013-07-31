package com.stanfy.gsonxml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Primitives;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import com.stanfy.gsonxml.XmlReader.Options;

/**
 * Wrapper for {@link Gson}.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public class GsonXml {

  /** Core object. */
  private final Gson core;

  /** XML parser creator. */
  private final XmlParserCreator xmlParserCreator;

  /** Option. */
  private final Options options;

  GsonXml(final Gson gson, final XmlParserCreator xmlParserCreator, final Options options) {
    if (xmlParserCreator == null) { throw new NullPointerException("XmlParserCreator is null"); }
    this.core = gson;
    this.xmlParserCreator = xmlParserCreator;
    this.options = options;
  }

  public Gson getGson() { return core; }

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
    final XmlReader jsonReader = new XmlReader(json, xmlParserCreator, options); // change reader
    final Object object = fromXml(jsonReader, classOfT);
    assertFullConsumption(object, jsonReader);
    return Primitives.wrap(classOfT).cast(object);
  }

  @SuppressWarnings("unchecked")
  public <T> T fromXml(final Reader json, final Type typeOfT) throws JsonIOException, JsonSyntaxException {
    final XmlReader jsonReader = new XmlReader(json, xmlParserCreator, options); // change reader
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
