package com.stanfy.gsonxml.issues;

import com.google.gson.annotations.SerializedName;
import com.stanfy.gsonxml.GsonXml;
import com.stanfy.gsonxml.GsonXmlBuilder;
import com.stanfy.gsonxml.XmlParserCreator;
import junit.framework.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Issue #7.
 * https://github.com/stanfy/gson-xml/issues/7
 */
public class Issue7 {

  static class A {
    List<B> B;
    List<C> C;
  }

  static class B {
    @SerializedName("@value")
    BigDecimal value;
  }

  static class C {
    @SerializedName("$")
    String  text;
  }

  @Test
  public void reproduce() {
    XmlParserCreator parserCreator = new XmlParserCreator() {
      @Override
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

    A a = gsonXml.fromXml("<A><B value=\"23.5\" /><C>Test</C></A>", A.class);
    assertEquals("{\"B\":[{\"@value\":23.5}],\"C\":[{\"$\":\"Test\"}]}", gsonXml.getGson().toJson(a));
  }

}
