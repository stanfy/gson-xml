package com.stanfy.gsonxml.test;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.google.gson.GsonXml;
import com.google.gson.GsonXmlBuilder;
import com.google.gson.annotations.SerializedName;


public class CommentsTest extends AbstractXmlTest {

  public static class Status {
    int code;
    String msg;
    String usrMsg;
  }

  public static class CommentsData {
    int total;
    String category;
    @SerializedName("comment")
    List<Comment> comments;
  }

  public static class Comment {
    String user;
    String text;
    @SerializedName("@id")
    int id;
    @SerializedName("@level")
    int level;
    @SerializedName("@date")
    Date date;
  }

  public static class Response {
    Status status;
    CommentsData data;
  }

  @Test
  public void realTest() throws Exception {
    final Response response = createGsonXml().fromXml(new InputStreamReader(CommentsTest.class.getResourceAsStream("comments-response.xml"), "UTF-8"), Response.class);
    assertEquals(0, response.status.code);
    assertEquals("article", response.data.category);
    assertEquals(2, response.data.comments.get(1).level);
  }

  private GsonXml createGsonXml() {
    return new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setDateFormat("dd.MM.yyyy HH:mm:ssZ")
        .setSameNameLists(true)
        .create();
  }

}
/*
----------------------
xml start <:comment>=null, com.stanfy.gsonxml.XmlReader$AttributesData@1bbb60c3
INSIDE_OBJECT NAME INSIDE_OBJECT NAME INSIDE_OBJECT
NAME, BEGIN_OBJECT, NAME, STRING, NAME, STRING, NAME, STRING, null
comment, @id, 18239321, @level, 1, @date, 11.07.2012 17:35:16+0300, null
----------------------
===== adapted =====
INSIDE_OBJECT NAME INSIDE_EMEDDED_ARRAY INSIDE_OBJECT
BEGIN_ARRAY, BEGIN_OBJECT, NAME, STRING, NAME, STRING, NAME, STRING, null
@id, 18239321, @level, 1, @date, 11.07.2012 17:35:16+0300, null
----------------------


07-11 20:41:31.947: I/System.out(2092): ----------------------
07-11 20:41:31.947: I/System.out(2092): xml start <:comment>=null, com.stanfy.gsonxml.XmlReader$AttributesData@b4834148
07-11 20:41:31.947: I/System.out(2092): INSIDE_OBJECT NAME INSIDE_OBJECT NAME INSIDE_OBJECT
07-11 20:41:31.947: I/System.out(2092): NAME, BEGIN_OBJECT, NAME, STRING, NAME, STRING, NAME, STRING, null
07-11 20:41:31.947: I/System.out(2092): comment, @id, 18239321, @level, 1, @date, 11.07.2012 17:35:16+0300, null
07-11 20:41:31.947: I/System.out(2092): ----------------------
07-11 20:41:31.947: I/System.out(2092): ===== adapted =====
07-11 20:41:31.947: I/System.out(2092): INSIDE_OBJECT NAME INSIDE_ARRAY INSIDE_OBJECT
07-11 20:41:31.947: I/System.out(2092): BEGIN_ARRAY, BEGIN_OBJECT, STRING, NAME, STRING, NAME, STRING, null
07-11 20:41:31.947: I/System.out(2092): 18239321, @level, 1, @date, 11.07.2012 17:35:16+0300, null
07-11 20:41:31.947: I/System.out(2092): ----------------------


 *
 *
 */
 */