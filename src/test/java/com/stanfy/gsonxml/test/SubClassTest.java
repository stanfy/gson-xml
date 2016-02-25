package com.stanfy.gsonxml.test;

import com.google.gson.annotations.SerializedName;
import com.stanfy.gsonxml.GsonXmlBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubClassTest {
    public static class A {
        public A(){

        }
        @SerializedName("b")
        B b;

         class B {
            @SerializedName("@id")
            long id;
            @SerializedName("$")
            String name;
        }
    }

    static final String XML =
            "        <a>" +
                    "   <b id=\"1\">test</b>" +
                    "</a>";

    @Test
    public void test() {
        final A a = new GsonXmlBuilder()
                .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
                .create()
                .fromXml(XML, A.class);
        assertEquals(1, a.b.id);
        assertEquals("test", a.b.name);
    }
}
