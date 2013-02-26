package com.stanfy.gsonxml;

import com.google.gson.GsonBuilder;
import com.stanfy.gsonxml.XmlReader.Options;

/**
 * Use this builder for constructing {@link GsonXml} object. All methods are very
 * similar to {@link com.google.gson.GsonBuilder}.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public class GsonXmlBuilder {

  /** Core builder. */
  private GsonBuilder coreBuilder;

  /** Factory for XML parser. */
  private XmlParserCreator xmlParserCreator;

  /** Options. */
  private final Options options = new Options();
  {
    // Parse option: whether to skip root element
    options.skipRoot = true;
    // Parse option: whether to treat XML namespaces.
    options.namespaces = false;
    // Parse option: list a created from a set of elements with the same name without a grouping element.
    options.sameNameList = false;
  }

  /**
   * @param gsonBuilder instance of {@link GsonBuilder}
   * @return this instance for chaining
   */
  public GsonXmlBuilder wrap(final GsonBuilder gsonBuilder) {
    this.coreBuilder = gsonBuilder;
    return this;
  }

  /**
   * Set a factory for XML pull parser.
   * @param xmlParserCreator instance of {@link XmlParserCreator}
   * @return this instance for chaining
   */
  public GsonXmlBuilder setXmlParserCreator(final XmlParserCreator xmlParserCreator) {
    this.xmlParserCreator = xmlParserCreator;
    return this;
  }

  /**
   * Here's the difference.<br/>
   * <b>Skip root: on</b>
   * <pre>
   *   &lt;root>&ltname>value&lt;/name>&lt;/root>
   *   ==>
   *   {name : 'value'}
   * </pre>
   * <b>Skip root: off</b>
   * <pre>
   *   &lt;root>&ltname>value&lt;/name>&lt;/root>
   *   ==>
   *   {root : {name : 'value'}}
   * </pre>
   * @param value true to skip root element
   * @return this instance for chaining
   */
  public GsonXmlBuilder setSkipRoot(final boolean value) {
    this.options.skipRoot = value;
    return this;
  }

  /**
   * Here's the difference.<br/>
   * <b>Treat namespaces: on</b>
   * <pre>
   *   &lt;root>&lt;ns:name>value&lt;/ns:name>&lt;/root>
   *   ==>
   *   {'&lt;ns>name' : 'value'}
   * </pre>
   * <b>Treat namespaces: off</b>
   * <pre>
   *   &lt;root>&lt;ns:name>value&lt;/ns:name>&lt;/root>
   *   ==>
   *   {name : 'value'}
   * </pre>
   * @param value true to treat namespaces
   * @return this instance for chaining
   */
  public GsonXmlBuilder setTreatNamespaces(final boolean value) {
    this.options.namespaces = value;
    return this;
  }

  /**
   * Here's the difference.<br/>
   * <b>Same name lists: on</b>
   * <pre>
   *   &lt;root>
   *     &lt;name>value&lt;/name>
   *     &lt;item>value1&lt;/item>
   *     &lt;item>value2&lt;/item>
   *   &lt;/root>
   *   ==>
   *   {name : 'value', item : ['value1', 'value2']}
   * </pre>
   * <b>Treat namespaces: off</b>
   * <pre>
   *   &lt;root>
   *     &lt;name>value&lt;/name>
   *     &lt;items>
   *       &lt;ignored>value1&lt;/ignored>
   *       &lt;ignored>value2&lt;/ignored>
   *     &lt;/items>
   *   &lt;/root>
   *   ==>
   *   {name : 'value', items : ['value1', 'value2']}
   * </pre>
   * @param value true for same name list policy
   * @return this instance for chaining
   */
  public GsonXmlBuilder setSameNameLists(final boolean value) {
    this.options.sameNameList = value;
    return this;
  }

  /**
   * If set to true than arrays can contain primitive values. If false only arrays can contain objects only.
   * When set to true you cannot parse the next sample:
   * <pre>
   *   &lt;list>
   *     &lt;item>
   *       text node value
   *       &lt;field-name>field value&lt;/field-name>
   *     &lt;/item>
   *     &lt;item>value2&lt;/item>
   *   &lt;/list>
   * </pre>
   * It's caused by the fact that parser meats 'text node value' and makes a decision that this item is primitive.
   * @param primitiveArrays value for primitive arrays policy
   * @return this instance for chaining
   */
  public GsonXmlBuilder setPrimitiveArrays(final boolean primitiveArrays) {
    this.options.primitiveArrays = primitiveArrays;
    return this;
  }

  /**
   * When set to true and the root element is parsed as a collection this collection items are treated as primitives.
   * @see #setPrimitiveArrays(boolean)
   * @param rootArrayPrimitive flag for 'root array primitive' policy
   * @return this instance for chaining
   */
  public GsonXmlBuilder setRootArrayPrimitive(final boolean rootArrayPrimitive) {
    this.options.rootArrayPrimitive = rootArrayPrimitive;
    return this;
  }

  /**
   * Creates a {@link GsonXml} instance based on the current configuration. This method is free of
   * side-effects to this {@code GsonXmlBuilder} instance and hence can be called multiple times.
   *
   * @return an instance of GsonXml configured with the options currently set in this builder
   */
  public GsonXml create() {
    if (coreBuilder == null) {
      coreBuilder = new GsonBuilder();
    }
    return new GsonXml(coreBuilder.create(), xmlParserCreator, options);
  }


}
