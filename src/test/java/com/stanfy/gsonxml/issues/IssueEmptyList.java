package com.stanfy.gsonxml.issues;

import com.stanfy.gsonxml.GsonXml;
import com.stanfy.gsonxml.GsonXmlBuilder;
import com.stanfy.gsonxml.XmlParserCreator;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Issue EmptyList.
 */
public class IssueEmptyList {

  private GsonXml gsonXml;

  static class A {
    List<I> B;
    List<I> C;
  }

  static class I {
    BigDecimal v;
  }

  @Before
  public void init() {
    XmlParserCreator parserCreator = new XmlParserCreator() {
      public XmlPullParser createParser() {
        try {
          return XmlPullParserFactory.newInstance().newPullParser();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };

    gsonXml = new GsonXmlBuilder()
        .setXmlParserCreator(parserCreator)
        .create();
  }

  @Test
  public void reproduce() {
    A a = gsonXml.fromXml("<A><B></B><C></C></A>", A.class);
    assertEquals("{\"B\":[],\"C\":[]}", gsonXml.getGson().toJson(a));
  }

}
