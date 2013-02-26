package com.stanfy.gsonxml.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.stanfy.gsonxml.XmlReader;

/**
 * Test for {@link XmlReader}.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public class NestedModelTest extends AbstractXmlTest {

  /** Person object. */
  public static class Person {
    /** Name. */
    String name;
    /** Position. */
    String position;
  }

  /** Company object. */
  public static class Company {
    /** Name. */
    String name;
    /** Position. */
    String field;
  }

  /** Info object. */
  public static class Info {
    /** Person. */
    Person person;
    /** Company. */
    Company company;
  }

  /** Test info XML. */
  public static final String INFO_XML =
      "<info>"
    + "  <person>"
    + "    <name>Jhoe</name>"
    + "    <position>Killer</position>"
    + "  </person>"
    + "  <company>"
    + "    <name>Assassins</name>"
    + "    <field>Services</field>"
    + "  </company>"
    + "</info>";

  @Test
  public void infoTest() {
    final Info info = gsonXml.fromXml(INFO_XML, Info.class);
    assertEquals("Jhoe", info.person.name);
    assertEquals("Killer", info.person.position);
    assertEquals("Assassins", info.company.name);
    assertEquals("Services", info.company.field);
  }

}
