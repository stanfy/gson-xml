package com.stanfy.gsonxml.test;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.stanfy.gsonxml.GsonXmlBuilder;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test for {@link com.stanfy.gsonxml.XmlReader}.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 *
 */
public class ListsTest extends AbstractXmlTest {

  /** Test XML. */
  public static final String TEST_XML =
      "<places>"
    + "  <place id=\"1\" lat=\"0.25\" long=\"0.26\">"
    + "    some text"
    + "    <name>&lt;Place&gt;</name>"
    + "  </place>"
    + "  <place id=\"2\" lat=\"0.27\" long=\"0.28\">"
    + "    <name>Place 2</name>"
    + "  </place>"
    + "</places>";

  /** Field and same name list. */
  public static final String TEST_XML_WITH_HEADER =
      "<places>"
    + "  <error>0</error>"
    + "  <place id=\"1\" lat=\"0.25\" long=\"0.26\">"
    + "    <name>&lt;Place&gt;</name>"
    + "  </place>"
    + "  <place id=\"2\" lat=\"0.27\" long=\"0.28\">"
    + "    <name>Place 2</name>"
    + "  </place>"
    + "</places>";

  /** Empty list xml. */
  public static final String TEST_XML_WITH_EMPTY_WRAPPED_LIST =
      "<places>"
    + "  <error>1</error>"
    + "  <places>"
    + "  </places>"
    + "</places>";

  /** Field and same name primitive list. */
  public static final String TEST_XML_WITH_HEADER_AND_PRIMITIVES_LIST =
        "<response>"
      + "  <error>0</error>"
      + "  <list>item0</list>"
      + "  <list>item1</list>"
      + "  <list>     item2 \n  </list>"
      + "  <list>     item3  </list>"
      + "</response>";

  /** Field and same name primitive list. */
  public static final String TEST_XML_FIELD_AND_CONTAINER_PRIMITIVE_LIST =
        "<response>"
      + "  <error>0</error>"
      + "  <list>"
      + "    <item>item0</item>"
      + "    <item>item1</item>"
      + "  </list>"
      + "</response>";

  /** Field and same name primitive list. */
  public static final String TEST_XML_PRIMITIVE_LIST =
        "<response>"
      + "  <item>1</item>"
      + "  <item>2</item>"
      + "  <item>3</item>"
      + "</response>";

  /** Place object. */
  public static class Place {
    @SerializedName("@id")
    long id;
    @SerializedName("@lat")
    double lat;
    @SerializedName("@long")
    double lon;

    String name;
  }

  /** Container. */
  public static class PlacesContainer {
    String error;
    @SerializedName("place")
    List<Place> places;
  }

  /** Container. */
  public static class PlacesWrappedContainer {
    String error;
    List<Place> places;
  }

  /** Primitives list + field. */
  public static class ListWithHeader {
    String error;
    List<String> list;
  }

  @Test
  public void listsTest() {
    final List<Place> places = gsonXml.fromXml(TEST_XML, new TypeToken<List<Place>>() {}.getType());
    assertPlaces(places);
  }

  @Test
  public void primitiveListWithSameNameTagsTest() throws JsonSyntaxException, JsonIOException, FileNotFoundException {
    final ListWithHeader listWithHeader =
        new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setPrimitiveArrays(true)
        .setSameNameLists(true)
        .setSkipRoot(true)
        .create()
        .fromXml(TEST_XML_WITH_HEADER_AND_PRIMITIVES_LIST, ListWithHeader.class);
    assertPrimitives(listWithHeader);
    assertEquals(listWithHeader.list.size(), 4);
  }

  private void assertPlaces(final List<Place> places) {
    assertEquals(2, places.size());
    assertEquals(1, places.get(0).id);
    assertEquals(2, places.get(1).id);

    assertEquals(0.26, places.get(0).lon, 0.00001);
  }

  private void assertPrimitives(final ListWithHeader listWithHeader) {
    assertEquals("0", listWithHeader.error);
    assertEquals("item0", listWithHeader.list.get(0));
    assertEquals("item1", listWithHeader.list.get(1));
  }

  @Test
  public void skipTest() {
    final PlacesContainer placesC = new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setSkipRoot(true)
        .setSameNameLists(true)
        .create()
        .fromXml(TEST_XML_WITH_HEADER, PlacesContainer.class);
    assertPlaces(placesC.places);
    assertEquals(placesC.error, "0");
  }

  @Test
  public void primitiveListWithContainerTest() {
    final ListWithHeader listWithHeader =
        new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setSkipRoot(true)
        .setPrimitiveArrays(true)
        .create()
        .fromXml(TEST_XML_FIELD_AND_CONTAINER_PRIMITIVE_LIST, ListWithHeader.class);
    assertPrimitives(listWithHeader);
  }

  @Test
  public void primitiveListIntSameNameTest() {
    final List<Integer> intList =
        new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setSkipRoot(true)
        .setRootArrayPrimitive(true)
        .create()
        .fromXml(TEST_XML_PRIMITIVE_LIST, new TypeToken<List<Integer>>() { }.getType());
    assertEquals(3, intList.size());
    assertEquals(3, (int)intList.get(2));
  }

  /**
   * This case is not supported.
   * @see GsonXmlBuilder#setPrimitiveArrays(boolean)
   */
  @Test(expected = JsonSyntaxException.class)
  public void firstTextNodeNotSupportedWithPrimitives() {
    new GsonXmlBuilder()
    .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
    .setSkipRoot(true)
    .setPrimitiveArrays(true)
    .create()
    .fromXml(TEST_XML, PlacesContainer.class);
  }

  @Test
  public void wrappedEmptyLists() {
    final PlacesWrappedContainer places = new GsonXmlBuilder()
    .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
    .create()
    .fromXml(TEST_XML_WITH_EMPTY_WRAPPED_LIST, PlacesWrappedContainer.class);

    assertEquals("1", places.error);
    assertTrue(places.places.isEmpty());
  }

  // currently we cannot parse this case
  @Test(expected = JsonSyntaxException.class)
  public void sameNameEmptyLists() {
    final PlacesContainer places = new GsonXmlBuilder()
    .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
    .setSameNameLists(true)
    .create()
    .fromXml("<root><error></error><place> </place></root>", PlacesContainer.class);

    assertEquals("", places.error);
    assertTrue(places.places.isEmpty());
  }

  @Test
  public void emptyRootList() {
    List<String> empty = new GsonXmlBuilder()
      .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
      .setRootArrayPrimitive(true)
      .create()
      .fromXml("<root></root>", new TypeToken<List<String>>() { }.getType());

    assertTrue(empty.isEmpty());

    empty = new GsonXmlBuilder()
      .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
      .setSameNameLists(false)
      .create()
      .fromXml("<root></root>", new TypeToken<List<String>>() { }.getType());

    assertTrue(empty.isEmpty());
  }

  /** Nested lists of primitives. */
  public static final String TEST_XML_NESTED_LISTS =
          "<response>"
                  + "  <list0>"
                  + "    <list1>"
                  + "      <list2>item0</list2>"
                  + "      <list2>item1</list2>"
                  + "      <list2>item2</list2>"
                  + "    </list1>"
                  + "    <list1>"
                  + "      <list2>item3</list2>"
                  + "      <list2>item4</list2>"
                  + "    </list1>"
                  + "  </list0>";

  public static class One {
    @SerializedName("list0")
    List<Two> list;
  }

  public static class Two {
    @SerializedName("list1")
    List<Three> list;
  }

  public static class Three {
    @SerializedName("list2")
    List<String> list;
  }

  @Test
  public void nestedListsTest() {
    One nestedList = new GsonXmlBuilder()
      .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
      .setSameNameLists(true)
      .create()
      .fromXml(TEST_XML_NESTED_LISTS, new TypeToken<One>() { }.getType());

    assertEquals(nestedList.list.size(), 1);
    assertEquals(nestedList.list.get(0).list.size(), 2);
    assertEquals(nestedList.list.get(0).list.get(0).list.size(), 3);
    assertEquals(nestedList.list.get(0).list.get(1).list.size(), 2);
    assertEquals(nestedList.list.get(0).list.get(0).list.get(0), "item0");
    assertEquals(nestedList.list.get(0).list.get(0).list.get(1), "item1");
    assertEquals(nestedList.list.get(0).list.get(0).list.get(2), "item2");
    assertEquals(nestedList.list.get(0).list.get(1).list.get(0), "item3");
    assertEquals(nestedList.list.get(0).list.get(1).list.get(1), "item4");
  }

}
