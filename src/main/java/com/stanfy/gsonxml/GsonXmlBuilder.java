package com.stanfy.gsonxml;

import com.google.gson.GsonBuilder;

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

  /** Parse option: whether to skip root element. */
  private boolean skipRoot = true;
  /** Parse option: whether to treat XML namespaces. */
  private boolean treatNamespaces;
  /** Parse option: list a created from a set of elements with the same name without a grouping element. */
  private boolean sameNameLists;

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
    this.skipRoot = value;
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
  public GsonXmlBuilder setTreatNamespaces(final boolean treatNamespaces) {
    this.treatNamespaces = treatNamespaces;
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
  public GsonXmlBuilder setSameNameLists(final boolean sameNameLists) {
    this.sameNameLists = sameNameLists;
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
    return new GsonXml(coreBuilder.create(), xmlParserCreator, skipRoot, treatNamespaces, sameNameLists);
  }


}
