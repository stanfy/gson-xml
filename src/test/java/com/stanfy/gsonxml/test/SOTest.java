package com.stanfy.gsonxml.test;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.google.gson.annotations.SerializedName;
import com.stanfy.gsonxml.GsonXmlBuilder;


public class SOTest extends AbstractXmlTest {

  public static final String XML =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    + "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:creativeCommons=\"http://backend.userland.com/creativeCommonsRssModule\">\n"
    + "  <title type=\"text\">newest questions tagged android - Stack Overflow</title>\n"
    + "  <entry>\n"
    + "    <id>http://stackoverflow.com/q/9439999</id>\n"
    + "    <title type=\"text\">Where is my data file?</title>\n"
    + "    <category scheme=\"http://stackoverflow.com/feeds/tag?tagnames=android&amp;sort=newest/tags\" term=\"android\"/>\n"
    + "    <category scheme=\"http://stackoverflow.com/feeds/tag?tagnames=android&amp;sort=newest/tags\" term=\"file\"/>\n"
    + "    <author>\n"
    + "      <name>cliff2310</name>\n"
    + "      <uri>http://stackoverflow.com/users/1128925</uri>\n"
    + "    </author>\n"
    + "    <link rel=\"alternate\" href=\"http://stackoverflow.com/questions/9439999/where-is-my-data-file\" />\n"
    + "    <published>2012-02-25T00:30:54Z</published>\n"
    + "    <updated>2012-02-25T00:30:54Z</updated>\n"
    + "    <summary type=\"html\">\n"
    + "      <p>I have an Application that requires a data file...</p>\n"
    + "    </summary>\n"
    + "  </entry>\n"
    + "  <entry>\n"
    + "    <id>http://stackoverflow.com/q/9449999</id>\n"
    + "    <title type=\"html\"><![CDATA[<b>Next</b> question]]></title>\n"
    + "  </entry>\n"
    + "</feed>";

  public static class Title {
    @SerializedName("@type")
    String type;
    @SerializedName("$")
    String text;
  }

  public static class Category {
    @SerializedName("@term")
    String term;
  }

  public static class Author {
    String name;
    String uri;
  }

  public static class Summary {
    @SerializedName("@type")
    String type;
    @SerializedName("p")
    String text;
  }

  public static class Entry {
    String id;
    Date published;
    Date updated;
    Title title;
    Category category;
    Author author;
    Summary summary;
  }

  public static class Feed {
    Title title;
    @SerializedName("entry")
    List<Entry> entries;
  }

  @Test
  public void soTest() {
    final Feed feed = new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setSameNameLists(true)
        .create()
        .fromXml(XML, Feed.class);
    assertEquals("newest questions tagged android - Stack Overflow", feed.title.text);
    assertEquals(2, feed.entries.size());
  }

}
