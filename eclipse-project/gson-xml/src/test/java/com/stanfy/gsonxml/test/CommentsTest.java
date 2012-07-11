package com.stanfy.gsonxml.test;

import java.io.InputStreamReader;
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

  public static class Comment {
    String user;
    String text;
  }

  public static class Response {
    Status status;
    @SerializedName("comment")
    List<Comment> comments;
  }

  @Test
  public void realTest() throws Exception {
    createGsonXml().fromXml(new InputStreamReader(CommentsTest.class.getResourceAsStream("comments-response.xml"), "UTF-8"), Response.class);
  }

  private GsonXml createGsonXml() {
    return new GsonXmlBuilder()
        .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
        .setDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")
        .setSameNameLists(true)
        .create();
  }

}
