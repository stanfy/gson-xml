package com.stanfy.gsonxml;

import org.xmlpull.v1.XmlPullParser;

/**
 * Creates XML pull parser.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public interface XmlParserCreator {

  /** @return XML pull parser instance */
  XmlPullParser createParser();

}
