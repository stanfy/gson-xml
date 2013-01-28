package com.stanfy.gsonxml.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.google.gson.annotations.SerializedName;
import com.stanfy.gsonxml.test.ListsTest.Place;

/**
 * Test for {@link com.stanfy.gsonxml.XmlReader}.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 *
 */
public class MixedTest extends AbstractXmlTest {

  /** Test xml. */
  public static final String TEST_XML =
      "<response code=\"0\" message=\"Success\">"
    + "  <ignore ignore=\"ignore\"><aha/></ignore>"
    + "  <person name=\"Guy\" age=\"23\" shortDescription=\"\">"
    + "    <text></text>"
    + "    <about>"
    + "      <![CDATA[A lot of interesting]]>"
    + "    </about>"
    + "    What do you think about him?"
    + "  </person>"
    + "  <ignore ignore=\"ignore\"><aha/></ignore>"
    + "  <places>"
    + "    <place id=\"1\" lat=\"0.25\" long=\"0.26\">"
    + "      <name>&lt;Place&gt;</name>"
    + "      <ignore>lalala</ignore>"
    + "      <info type=\"text\">place info</info>"
    + "    </place>"
    + "    <place id=\"2\" lat=\"0.27\" long=\"0.28\">"
    + "      <name>Place 2</name>"
    + "    </place>"
    + "  </places>"
    + "  <ignore ignore=\"ignore\"><aha/></ignore>"
    + "</response>";

  /** Place object. */
  public static class Person {
    @SerializedName("@name")
    String name;
    @SerializedName("@age")
    int age;
    @SerializedName("@shortDescription")
    String shortDescription;

    String about;

    @SerializedName("$")
    String question;

    String text;
  }

  public static class Response {
    @SerializedName("@code")
    int code;
    @SerializedName("@message")
    String message;

    Person person;
    List<EPlace> places;
  }

  public static class Info {
    @SerializedName("@type")
    String type;
    @SerializedName("$")
    String text;
  }

  public static class EPlace extends Place {
    Info info;
  }

  @Test
  public void mixedTest() {
    final Response response = gsonXml.fromXml(TEST_XML, Response.class);

    assertEquals(0, response.code);
    assertEquals("Success", response.message);

    assertNotNull(response.person);
    assertEquals("Guy", response.person.name);
    assertEquals(23, response.person.age);
    assertEquals("What do you think about him?", response.person.question);
    assertEquals("A lot of interesting", response.person.about);

    assertNotNull(response.places);
    assertEquals("text", response.places.get(0).info.type);
  }

}
