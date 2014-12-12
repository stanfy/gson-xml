GsonXml
===============

GsonXml is a small library that allows using [Google Gson library] (https://code.google.com/p/google-gson/) for XML deserialization.
The main idea is to convert a stream of XML pull parser events to a stream of JSON tokens.
It's implemented by passing a custom `JsonReader` (that wraps `XmlPullParsers`) to `Gson`.

Though currently this library is not pretending to be the most efficient one for XML deserialization, it can be very useful.

[![Build Status](https://secure.travis-ci.org/stanfy/gson-xml.png?branch=master)](http://travis-ci.org/stanfy/gson-xml)

Compatible Gson versions: 2.1, 2.2.

Usage
-------------

    /** Very simple model. */
    public static class SimpleModel {
      private String name;
      private String description;
    
      public String getName() { return name; }
      public String getDescription() { return description; }
    }
    
    
    public void simpleTest() {
      
      XmlParserCreator parserCreator = new XmlParserCreator() {
        @Override
        public XmlPullParser createParser() {
          try {
            return XmlPullParserFactory.newInstance().newPullParser();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      };
    
      GsonXml gsonXml = new GsonXmlBuilder()
         .setXmlParserCreator(parserCreator)
         .create();

      String xml = "<model><name>my name</name><description>my description</description></model>";
      SimpleModel model = gsonXml.fromXml(xml, SimpleModel.class);
      
      assertEquals("my name", model.getName());
      assertEquals("my description", model.getDescription());
    }

Use `@SerializedName` annotation to handle tag attributes and text nodes.

To illustrate, this XML
```xml
<person dob="01.01.1973" gender="male">
  <name>John</name>
  Likes traveling.
</person>
```
can be mapped to a POJO
```java
public class Person {

  @SerializedName("@dob")
  private String dob;

  @SerializedName("@gender")
  private String gender;

  private String name;

  @SerializedName("$")
  private String description;

  // ...
}
```

You may also take a look at
[SimpleXmlReaderTest](https://github.com/stanfy/gson-xml/blob/master/src/test/java/com/stanfy/gsonxml/test/SimpleXmlReaderTest.java)
to see other samples.

Deserializing lists
-------------------

Since there is no direct analogy of JSON arrays in XML, some ambiguity appears when you are trying to deserialize lists.
`GsonXmlBuilder` has method `setSameNameLists(boolean)` in order to resolve this issue.

Considering the following Java class
```java
class Container {
  List<Person> persons;
}
```

Call `setSameNameLists(false)` in order to deserialize such an object from the following XML:
```xml
<container>
  <persons>
    <any-tag-name id="1">
      <name>John</name>
    </any-tag-name>
    <any-tag-name id="2">
      <name>Mark</name>
    </any-tag-name>
  </persons>
</container>
```
Note that tag names of `persons` tag children nodes are ignored.

And call `setSameNameLists(true)` in order to deserialize such an object from another piece of XML:
```xml
<container>
  <person id="1">
    <name>John</name>
  </person>
  <person id="2">
    <name>Mark</name>
  </person>
</container>
```
Don't forget to put `SerializedName('person')` annotation on `persons` field.

Note that at the moment it's impossible to deserialize more than one list in the same container with option
`setSameNameLists(true)`.

Also be aware that currently it's impossible to deserialize XML structure where both types of lists exist.


Download
--------

In a Maven project include the dependency:
```
<dependency>
  <groupId>com.stanfy</groupId>
  <artifactId>gson-xml-java</artifactId>
  <version>(insert latest version)</version>
</dependency>
```

Gradle example:
```
compile 'com.stanfy:gson-xml-java:0.1.+'
```

Android Notes
-------------

In order to use this library in Android project, copy only `gson-xml` and `gson` jars to the project libraries folder.
`kxml2` and `xmlpull` jars are not required since `XmlPullParser` is a part of Android SDK.
To exclude them in your Gradle project use the following lines:
```
compile('com.stanfy:gson-xml-java:0.1.+') {
  exclude group: 'xmlpull', module: 'xmlpull'
}
```

Also be aware that Android SDK up to 'Ice Cream Sandwich' returns instance of `ExpatPullParser` when you call
[Xml.newPullParser()](http://developer.android.com/reference/android/util/Xml.html#newPullParser()).
And this parser does not support namespaces.

Read also this [blog post](http://android-developers.blogspot.com/2011_12_01_archive.html) about issues with
Android XML parsers.

License
-------

    Copyright 2013 Stanfy Corp.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    
