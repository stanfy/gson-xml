package com.stanfy.gsonxml.test;

import static org.fest.assertions.api.Assertions.*;

import com.google.gson.annotations.SerializedName;
import com.stanfy.gsonxml.GsonXml;
import com.stanfy.gsonxml.GsonXmlBuilder;

import org.junit.Test;

import java.util.List;

/**
 * Test for nested lists.
 */
public class NestedListTest {

  /** Nested lists. */
  public static final String TEST_XML_NESTED_SAME_NAME_LIST =
      "<one date=\"1\">"
    + "  <two id=\"3\">"
    + "    <three id=\"2\" title=\"test\">"
    + "    </three>"
    + "  </two>"
    + "</one>";

  static class One {

    @SerializedName("@date")
    long date;

    @SerializedName("two")
    List<Two> twos;
  }

  class Two {

    @SerializedName("@id")
    int id;

    @SerializedName("three")
    List<Three> threes;
  }

  class Three {

    @SerializedName("@id")
    int id;

    @SerializedName("@title")
    String title;
  }


  @Test
  public void shouldHandleNestedSameNameLists() {
    GsonXml gsonXml = new GsonXmlBuilder().setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR).setSameNameLists(true).create();
    One res = gsonXml.fromXml(TEST_XML_NESTED_SAME_NAME_LIST, One.class);
    assertThat(res).isNotNull();
    assertThat(res.date).isEqualTo(1);

    assertThat(res.twos).isNotEmpty();
    assertThat(res.twos.size()).isEqualTo(1);
    assertThat(res.twos.get(0).id).isEqualTo(3);

    assertThat(res.twos.get(0).threes).isNotEmpty();
    assertThat(res.twos.get(0).threes.size()).isEqualTo(1);
    assertThat(res.twos.get(0).threes.get(0).title).isEqualTo("test");
    assertThat(res.twos.get(0).threes.get(0).id).isEqualTo(2);
  }


  /** Nested lists of primitives. */
  public static final String TEST_XML_NESTED_PRIMITIVE_SAME_NAME_LIST =
      "  <one date=\"2\">"
    + "    <two id=\"1\">"
    + "      <three>item0</three>"
    + "      <three>item1</three>"
    + "      <three>item2</three>"
    + "    </two>"
    + "    <two>"
    + "      <three>item3</three>"
    + "      <three>item4</three>"
    + "    </two>"
    + "  </one>";

  class TwoPrimitive {
    @SerializedName("@id")
    long id;
    @SerializedName("three")
    List<String> threes;
  }

  class OnePrimitive {
    @SerializedName("@date")
    int date;

    @SerializedName("two")
    List<TwoPrimitive> twos;
  }

  @Test
  public void shouldHandlePrimitiveNestedSameNameLists() {
    GsonXml gsonXml = new GsonXmlBuilder().setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR).setPrimitiveArrays(true).setSameNameLists(true).create();
    OnePrimitive res = gsonXml.fromXml(TEST_XML_NESTED_PRIMITIVE_SAME_NAME_LIST, OnePrimitive.class);
    assertThat(res).isNotNull();
    assertThat(res.date).isEqualTo(2);

    assertThat(res.twos).isNotEmpty();
    assertThat(res.twos.size()).isEqualTo(2);
    assertThat(res.twos.get(0).id).isEqualTo(1);

    assertThat(res.twos.get(0).threes).containsExactly("item0", "item1", "item2");
    assertThat(res.twos.get(1).threes).containsExactly("item3", "item4");
  }



}
