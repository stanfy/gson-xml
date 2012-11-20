GsonXml
===============

GsonXml is a small library that allows using Google Gson library for XML deserialization.
The main idea is to convert a stream of XML pull parser events to a stream of JSON tokens.
It's implemented by passing a custom `JsonReader` (that wraps `XmlPullParsers`) to Gson.

Though currently this library is not pretending to be the most efficient library for 
XML deserialization, it can be very useful.

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
      String xml = "<model><name>my name</name><description>my description</description></model>";
      GsonXml gsonXml = new GsonXmlBuilder().create();
      SimpleModel model = gsonXml.fromXml(xml, SimpleModel.class);
      
      assertEquals("my name", model.getName());
      assertEquals("my description", model.getDescription());
    }
