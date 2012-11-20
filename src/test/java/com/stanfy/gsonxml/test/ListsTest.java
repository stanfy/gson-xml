package com.stanfy.gsonxml.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.gson.GsonXmlBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;


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
    + "    <name>&lt;Place&gt;</name>"
    + "  </place>"
    + "  <place id=\"2\" lat=\"0.27\" long=\"0.28\">"
    + "    <name>Place 2</name>"
    + "  </place>"
    + "</places>";

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
    List<Place> places;
  }

  @Test
  public void listsTest() {
    final List<Place> places = gsonXml.fromXml(TEST_XML, new TypeToken<List<Place>>() {}.getType());
    assertPlaces(places);
  }

  private void assertPlaces(final List<Place> places) {
    assertEquals(2, places.size());
    assertEquals(1, places.get(0).id);
    assertEquals(2, places.get(1).id);

    assertEquals(0.26, places.get(0).lon, 0.00001);
  }

  @Test
  public void skipTest() {
    final PlacesContainer placesC = new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setSkipRoot(false)
        .create()
        .fromXml(TEST_XML, PlacesContainer.class);
    assertPlaces(placesC.places);
  }

}
