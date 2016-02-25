package com.stanfy.gsonxml.test;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.stanfy.gsonxml.GsonXmlBuilder;

import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DemoTest extends AbstractXmlTest {

    public static class A {
        public A() {
            maps = new LinkedHashMap<String, B>();
        }

        @SerializedName("@id")
        long id;
        @SerializedName("name")
        List<String> names;
        @SerializedName("b")
        List<B> bs;

        @SerializedName("id")
        int[] is;
        @SerializedName(value = "map")
        Map<String, B> maps;
        //List<MyMap> maps;

        @SerializedName("type")
        C c;
    }

    public static class MyMap {
        @SerializedName("key")
        String key;
        @SerializedName("value")
        B value;
    }

    public static class B {
        @SerializedName("@id")
        long id;
        @SerializedName("$")
        String text;

        @SerializedName("@type")
        C c;
    }

    public enum C {
        a, b, c
    }

    final static String TEST_XML =
            "       <a id=\"123\">" +
                    "   <name>test name1</name>" +
                    "   <name>test name2</name>" +
                    "   <b id=\"1234567890\">b name1</b>" +
                    "   <b id=\"0123\">b name2</b>" +
                    "   <map>" +
                    "       <key>hello</key>" +
                    "       <value id=\"1\">world</value>" +
                    "   </map>" +
                    "   <map>" +
                    "       <value id=\"1\" type=\"a\" >name</value>" +
                    "       <key>hello2</key>" +
                    "   </map>" +
                    "   <id>1</id>" +
                    "   <id>2</id>" +
                    "   <id>3</id>" +
                    "   <type>b</type>" +
                    "</a>";
    final static String TEST_XML_MAP =
            "       <a>" +
                    "   <map>" +
                    "       <key>hello</key>" +
                    "       <value id=\"1\">world</value>" +
                    "   </map>" +
                    "   <map>" +
                    "       <value id=\"1\" type=\"a\" >name</value>" +
                    "       <key>hello2</key>" +
                    "   </map>" +
                    "</a>";

    @Test
    public void test() {
        GsonBuilder builder = new GsonBuilder();
        builder.enableComplexMapKeySerialization();
        builder.registerTypeAdapter(C.class, new TypeAdapter<C>() {
            @Override
            public C read(JsonReader in) throws IOException {
                String value = in.nextString();
                C[] cs = C.values();
                for (C c : cs) {
                    if (c.toString().equalsIgnoreCase(value)) {
                        return c;
                    }
                }
                return null;
            }

            @Override
            public void write(JsonWriter out, C value) throws IOException {
                out.value(value == null ? "" : value.toString());
            }
        });
        final A a = new GsonXmlBuilder()
                .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
                .wrap(builder)
                .setPrimitiveArrays(true)
                .setSameNameLists(true)
                .create()
                .fromXml(TEST_XML, A.class);
        assertEquals("test name2", a.names.get(1));
        assertEquals(1234567890, a.bs.get(0).id);
        assertEquals("b name2", a.bs.get(1).text);
        assertEquals(2, a.bs.size());
        assertEquals(C.b, a.c);
        assertEquals("world", a.maps.get("hello").text);
        assertEquals("name", a.maps.get("hello2").text);
        assertEquals(C.a, a.maps.get("hello2").c);
        assertEquals(2, a.maps.size());
        assertEquals(3, a.is.length);
        assertEquals(LinkedHashMap.class, a.maps.getClass());
//        assertEquals(2, a.maps.get(1).value.id);
    }

    @Test
    public void testRootMap() {
        GsonBuilder builder = new GsonBuilder();
        builder.enableComplexMapKeySerialization();
        builder.registerTypeAdapter(C.class, new TypeAdapter<C>() {
            @Override
            public C read(JsonReader in) throws IOException {
                String value = in.nextString();
                C[] cs = C.values();
                for (C c : cs) {
                    if (c.toString().equalsIgnoreCase(value)) {
                        return c;
                    }
                }
                return null;
            }

            @Override
            public void write(JsonWriter out, C value) throws IOException {
                out.value(value == null ? "" : value.toString());
            }
        });
        Map<String, B> maps = new GsonXmlBuilder()
                .setXmlParserCreator(SimpleXmlReaderTest.PARSER_CREATOR)
                .wrap(builder)
                .setSkipRoot(true)
                .setPrimitiveArrays(true)
                .setSameNameLists(true)
                .create()
                .fromXml(TEST_XML_MAP, new TypeToken<Map<String, B>>() {
                }.getType());
        assertEquals("world", maps.get("hello").text);
        assertEquals("name", maps.get("hello2").text);
        assertEquals(C.a, maps.get("hello2").c);
        assertEquals(2, maps.size());
    }
}
