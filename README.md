GsonXml
===============

GsonXml is a small library that allows using [Google Gson library] (https://code.google.com/p/google-gson/) for XML deserialization.
The main idea is to convert a stream of XML pull parser events to a stream of JSON tokens.
It's implemented by passing a custom `JsonReader` (that wraps `XmlPullParsers`) to Gson.

Though currently this library is not pretending to be the most efficient library for XML deserialization, it can be very useful.

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

Download
--------

[Zip](https://github.com/downloads/stanfy/gson-xml/gson-xml-0.1-release.zip) that contains all the required binaries and main source 
can be download at Github Downloads section. 

Android Note
------------

In order to use this library in Android project, copy only gson-xml and gson jars to the project libraries folder.
kxml2 and xmlpull jars are not required.
    