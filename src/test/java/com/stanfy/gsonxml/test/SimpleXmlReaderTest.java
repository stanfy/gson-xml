package com.stanfy.gsonxml.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.fest.reflect.core.Reflection;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.gson.annotations.SerializedName;
import com.stanfy.gsonxml.GsonXml;
import com.stanfy.gsonxml.GsonXmlBuilder;
import com.stanfy.gsonxml.XmlParserCreator;
import com.stanfy.gsonxml.XmlReader;

import java.io.StringReader;

/**
 * Tests for {@link XmlReader}.
 */
public class SimpleXmlReaderTest {

  /** Default parser creator. */
  public static final XmlParserCreator PARSER_CREATOR = new XmlParserCreator() {
    @Override
    public XmlPullParser createParser() {
      try {
        return Reflection.staticMethod("newPullParser")
            .withReturnType(XmlPullParser.class)
            .in(Class.forName("android.util.Xml"))
            .invoke();
      } catch (final Exception ignored) {
        // it's not Android
      }

      try {
        final XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        return parser;
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  };

  static GsonXml createGson() {
    return createGson(false);
  }
  static GsonXml createGson(final boolean namespaces) {
    return new GsonXmlBuilder().setXmlParserCreator(PARSER_CREATOR).setTreatNamespaces(namespaces).create();
  }

  /* ================== Simple =================== */

  /** Very simple model. */
  public static class SimpleModel {
    private String name;
    private String description;

    public String getName() { return name; }
    public String getDescription() { return description; }
  }

  @Test
  public void simpleTest() {
    final String xml = "<model><name>my name</name><description>my description</description></model>";
    final SimpleModel model = createGson().fromXml(xml, SimpleModel.class);
    assertEquals("my name", model.getName());
    assertEquals("my description", model.getDescription());
  }

  /* ================== Attributes test =================== */

  /** Very simple model. */
  public static class SimpleModelForAttr {
    private String name;
    @SerializedName("@a1")
    private String a1;
    @SerializedName("@a2")
    private int a2;

    public String getName() { return name; }
    public String getA1() { return a1; }
    public int getA2() { return a2; }
  }

  @Test
  public void attributesAndSkipTest() {
    // description must be skipped
    final String xml = "<model a1=\"a\" a2=\"2\"><name>my name</name><description>my description</description></model>";
    final SimpleModelForAttr model = createGson().fromXml(xml, SimpleModelForAttr.class);
    assertEquals("my name", model.getName());
    assertEquals("a", model.getA1());
    assertEquals(2, model.getA2());
  }

  /* ================== Embedded text =================== */

  /** Very simple model. */
  public static class SimpleModelForText extends SimpleModel {
    @SerializedName("$")
    private String description;

    @Override
    public String getDescription() { return description; }
  }

  @Test
  public void embeddedTextTest() {
    final String xml = "<model><name>my name</name>my description</model>";
    final SimpleModelForText model = createGson().fromXml(xml, SimpleModelForText.class);
    assertEquals("my name", model.getName());
    assertEquals("my description", model.getDescription());
  }

  /* ================== Multiple text =================== */

  /** Very simple model. */
  public static class SimpleModelForMultipleText extends SimpleModelForText {
    @SerializedName("$2")
    private String description2;
    @SerializedName("$3")
    private String description3;

    public String getDescription2() { return description2; }
    public String getDescription3() { return description3; }
  }

  @Test
  public void multipleTextTest() {
    final String xml = "<model>lala<name>my name</name>my description<ignore/>t1<ignore/>t2</model>";
    final SimpleModelForMultipleText model = createGson().fromXml(xml, SimpleModelForMultipleText.class);
    assertEquals("my name", model.getName());
    assertEquals("lala", model.getDescription());
    assertEquals("t1", model.getDescription2());
    assertEquals("t2", model.getDescription3());
  }

  /* ================== Name spaces =================== */

  /** Very simple model. */
  public static class SimpleModelForNS extends SimpleModel {
    @SerializedName("<myNs>description")
    private String description;
    @SerializedName("@<myNs>a")
    private boolean a;
    @Override
    public String getDescription() { return description; }
    public boolean isA() { return a; }
  }

  @Test
  public void namespaceTest() {
    final String xml = "<model xmlns:myNs=\"http://fake.com/myNs\" myNs:a=\"true\"><name>my name</name><myNs:description>my description</myNs:description></model>";
    final SimpleModelForNS model = createGson(true).fromXml(xml, SimpleModelForNS.class);
    assertEquals("my name", model.getName());
    assertEquals("my description", model.getDescription());
    assertTrue(model.isA());
  }

  /* ================== Text =================== */

  @Test
  public void entityRefTest() throws Exception {
    String xml = "<model a1=\"a&amp;a&lt;&gt;\"><name>my &lt;&gt; name &amp;</name><description>my &amp; description</description></model>";
    SimpleModelForAttr result = createGson().fromXml(xml, SimpleModelForAttr.class);
    assertEquals("a&a<>", result.getA1());
    assertEquals("my <> name &", result.getName());
  }

  /** Result for dashedTagNamesShouldBeParsed. */
  private static class DashedTagResult {
    @SerializedName("dashed-field")
    String field;
  }

  @Test
  public void dashedTagNamesShouldBeParsed() throws Exception {
    String xml = "<r><dashed-field>a</dashed-field></r>";
    DashedTagResult r = createGson().fromXml(xml, DashedTagResult.class);
    assertEquals("a", r.field);
  }

  /** Result for dashedTagNamesShouldBeParsed. */
  private static class DashedAttrResult {
    @SerializedName("@dashed-field")
    String field;
  }

  @Test
  public void dashedAttrNamesShouldBeParsed() throws Exception {
    String xml = "<r dashed-field=\"b\"/>";
    DashedAttrResult r = createGson().fromXml(xml, DashedAttrResult.class);
    assertEquals("b", r.field);
  }

}
