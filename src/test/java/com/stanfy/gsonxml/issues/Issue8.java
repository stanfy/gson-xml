package com.stanfy.gsonxml.issues;

import com.google.gson.annotations.SerializedName;
import com.stanfy.gsonxml.GsonXml;
import com.stanfy.gsonxml.GsonXmlBuilder;
import com.stanfy.gsonxml.XmlParserCreator;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Issue #8.
 * https://github.com/stanfy/gson-xml/issues/8
 */
public class Issue8 {

  static class A {
    List<B> B;
  }

  static class B {
    @SerializedName("@value")
    BigDecimal value;
  }

  @Test
  public void reproduce() {
    XmlParserCreator parserCreator = new XmlParserCreator() {
      public XmlPullParser createParser() {
        try {
          return XmlPullParserFactory.newInstance().newPullParser();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };

    GsonXml gsonXml = new GsonXmlBuilder()
        .setSameNameLists(true)
        .setXmlParserCreator(parserCreator)
        .create();

    A a = gsonXml.fromXml("<A><B /></A>", A.class);
    assertEquals("{\"B\":[{}]}", gsonXml.getGson().toJson(a));
  }


}
