package com.stanfy.gsonxml.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.gson.GsonXmlBuilder;


public class RssTest extends AbstractXmlTest {

  public static final String XML =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    + "<rss version=\"2.0\">\n"
    + "  <channel>\n"
    + "    <item>\n"
    + "      <id>1</id>\n"
    + "    </item>\n"
    + "    <item>\n"
    + "      <id>2</id>\n"
    + "    </item>\n"
    + "  </channel>\n"
    + "</rss>";

  public static class Rss {
    List<Item> channel;
  }

  public static class Item {
    long id;
  }

  @Test
  public void rssTest() {
    final Rss feed = new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .create()
        .fromXml(XML, Rss.class);
    assertEquals(1, feed.channel.get(0).id);
  }

}
