package com.stanfy.gsonxml.test;

import static com.stanfy.gsonxml.test.SimpleXmlReaderTest.createGson;

import org.junit.BeforeClass;

import com.stanfy.gsonxml.GsonXml;

/**
 * Base class for tests.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public abstract class AbstractXmlTest {

  /** Parser. */
  static GsonXml gsonXml;

  @BeforeClass
  public static void createParser() {
    gsonXml = createGson();
  }

}
