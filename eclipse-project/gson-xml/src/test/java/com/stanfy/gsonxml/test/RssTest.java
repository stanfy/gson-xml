package com.stanfy.gsonxml.test;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.google.gson.GsonXml;
import com.google.gson.GsonXmlBuilder;


public class RssTest extends AbstractXmlTest {

  public static final String XML =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    + "<rss version=\"2.0\">\n"
    + "  <channel>\n"
    + "    <item>\n"
    + "      <id>1</id>\n"
    + "      <pubDate><![CDATA[Tue, 10 Jul 2012 10:43:36 +0300]]></pubDate>\n"
    + "      <title><![CDATA[Some text]]></title>\n"
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
    Date pubDate;
    String title;
  }



  @Test
  public void rssTest() throws Exception {
    final Rss feed = createGsonXml().fromXml(XML, Rss.class);
    assertEquals(1, feed.channel.get(0).id);
    assertEquals(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").parse("Tue, 10 Jul 2012 10:43:36 +0300"), feed.channel.get(0).pubDate);
  }

  @Test
  public void realTest() throws Exception {
    final Rss feed = createGsonXml().fromXml(new InputStreamReader(RssTest.class.getResourceAsStream("rss-response.xml"), "UTF-8"), Rss.class);
    assertEquals(20, feed.channel.size());
  }

  private GsonXml createGsonXml() {
    return new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")
        .create();
  }

}
