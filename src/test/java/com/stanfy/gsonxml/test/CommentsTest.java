package com.stanfy.gsonxml.test;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.stanfy.gsonxml.GsonXml;
import com.stanfy.gsonxml.GsonXmlBuilder;


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

  @Test
  public void realTest2() throws Exception {
    final Response response = createGsonXml().fromXml(new InputStreamReader(CommentsTest.class.getResourceAsStream("comments-response-2.xml"), "UTF-8"), Response.class);
    assertEquals(0, response.status.code);
    assertEquals("article", response.data.category);
    assertEquals(1194, response.data.total);
  }

  private GsonXml createGsonXml() {
    return new GsonXmlBuilder()
        .wrap(
            new GsonBuilder().setDateFormat("dd.MM.yyyy HH:mm:ssZ")
         )
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setSameNameLists(true)
        .create();
  }

}
