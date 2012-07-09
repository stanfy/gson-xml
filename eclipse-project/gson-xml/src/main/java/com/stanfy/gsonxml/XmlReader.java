package com.stanfy.gsonxml;

import java.io.IOException;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Reads XML as JSON.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public class XmlReader extends JsonReader {

  /** Scope. */
  private static enum Scope {
    /** We are inside an object. Next token should be {@link JsonToken#NAME} or {@link JsonToken#END_OBJECT}. */
    INSIDE_OBJECT,
    /** We are inside an array. Next token should be {@link JsonToken#BEGIN_OBJECT} or {@link JsonToken#END_ARRAY}. */
    INSIDE_ARRAY,
    INSIDE_EMEDDED_ARRAY,
    /** New start tag met, we returned {@link JsonToken#NAME}. Object, array, or value can go next. */
    NAME
  }

  /** XML parser. */
  private final XmlPullParser xmlParser;

  /** Option. */
  private final boolean skipRoot, sameNameList;

  /** Tokens queue. */
  private TokenRef tokensQueue, tokensQueueStart;
  /** Values queue. */
  private ValueRef valuesQueue, valuesQueueStart;

  private JsonToken expectedToken;

  /** State. */
  private boolean endReached, firstStart = true;

  private JsonToken token;

  /** Counter for "$". */
  private int textNameCounter = 0;

  /** Stack. */
  private Scope[] stack = new Scope[32];
  /** Stack size. */
  private int stackSize = 0;

  private boolean skipping;

  public XmlReader(final Reader in, final XmlParserCreator creator, final boolean skipRoot, final boolean namespaces, final boolean sameNameList) {
    super(in);
    this.xmlParser = creator.createParser();
    this.skipRoot = skipRoot;
    this.sameNameList = sameNameList;
    try {
      this.xmlParser.setInput(in);
      this.xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, namespaces);
    } catch (final XmlPullParserException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  private void dump(final boolean showToken) {
    for (int i = 0; i < stackSize; i++) {
      System.out.print(stack[i] + " ");
    }
    System.out.println();

    if (showToken) {
      System.out.print(token + ", ");
    }
    System.out.println(tokensQueueStart);
    System.out.println(valuesQueueStart);
    System.out.println("----------------------");
  }

  private JsonToken peekNextToken() { return tokensQueueStart != null ? tokensQueueStart.token : null; }

  private JsonToken nextToken() {
    final TokenRef ref = tokensQueueStart;
    if (ref == null) {
      return JsonToken.END_DOCUMENT;
    }

    tokensQueueStart = ref.next;
    if (ref == tokensQueue) { tokensQueue = null; }
    return ref.token;
  }

  private ValueRef nextValue() {
    final ValueRef ref = valuesQueueStart;
    if (ref == null) { throw new IllegalStateException("No value can be given"); }
    if (ref == valuesQueue) { valuesQueue = null; }
    valuesQueueStart = ref.next;
    return ref;
  }

  private void expect(final JsonToken token) throws IOException {
    final JsonToken actual = peek();
    this.token = null;
    if (actual != token) { throw new IllegalStateException(token + " expected, but met " + actual); }
  }

  @Override
  public void beginObject() throws IOException {
    expectedToken = JsonToken.BEGIN_OBJECT;
    expect(expectedToken);
  }
  @Override
  public void endObject() throws IOException {
    expectedToken = JsonToken.END_OBJECT;
    expect(expectedToken);
  }
  @Override
  public void beginArray() throws IOException {
    expectedToken = JsonToken.BEGIN_ARRAY;
    expect(expectedToken);
  }
  @Override
  public void endArray() throws IOException {
    expectedToken = JsonToken.END_ARRAY;
    expect(expectedToken);
  }

  @Override
  public boolean hasNext() throws IOException {
    peek();
    return token != JsonToken.END_OBJECT && token != JsonToken.END_ARRAY;
  }

  @Override
  public void skipValue() throws IOException {
    skipping = true;
    try {
      int count = 0;
      do {
        final JsonToken token = peek();
        if (token == JsonToken.BEGIN_ARRAY || token == JsonToken.BEGIN_OBJECT) {
          count++;
        } else if (token == JsonToken.END_ARRAY || token == JsonToken.END_OBJECT) {
          count--;
        } else if (valuesQueue != null) {
          nextValue(); // pull ignored value
        }
        this.token = null; // advance
      } while (count != 0);
    } finally {
      skipping = false;
    }
  }

  private void adaptCurrentToken() {
    if (token == expectedToken) { return; }
    if (expectedToken == JsonToken.BEGIN_ARRAY && token == JsonToken.BEGIN_OBJECT) {
      token = JsonToken.BEGIN_ARRAY;

      stackSize -= 3;
      if (stackSize < 0) { stackSize = 0; }

      if (peekNextToken() == JsonToken.NAME) {
        if (sameNameList) {
          // use it as a field
          pushToQueue(JsonToken.BEGIN_OBJECT);

          push(Scope.INSIDE_EMEDDED_ARRAY);
          push(Scope.INSIDE_OBJECT);
          push(Scope.NAME);
        } else {
          // ignore name
          nextToken();
          nextValue();

          push(Scope.INSIDE_ARRAY);
          push(Scope.INSIDE_OBJECT);
          if (peekNextToken() != JsonToken.BEGIN_OBJECT) {
            pushToQueue(JsonToken.BEGIN_OBJECT);
          }
        }
      }

      System.out.println("===== adapted =====");
      dump(true);
    }
  }

  @Override
  public JsonToken peek() throws IOException {
    if (expectedToken == null && firstStart) { return JsonToken.BEGIN_OBJECT; }

    if (token != null) {
      adaptCurrentToken();
      expectedToken = null;
      return token;
    }

    try {

      fillQueues();
      expectedToken = null;

      return token = nextToken();

    } catch (final XmlPullParserException e) {
      throw new IOException("XML parsing exception", e);
    }
  }

  @Override
  public String nextString() throws IOException {
    expect(JsonToken.STRING);
    return nextValue().value;
  }
  @Override
  public boolean nextBoolean() throws IOException {
    expect(JsonToken.BOOLEAN);
    final String value = nextValue().value;
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return true;
    }
    throw new IOException("Cannot parse <" + value + "> to boolean");
  }
  @Override
  public double nextDouble() throws IOException {
    expect(JsonToken.STRING);
    return Double.parseDouble(nextValue().value);
  }
  @Override
  public int nextInt() throws IOException {
    expect(JsonToken.STRING);
    return Integer.parseInt(nextValue().value);
  }
  @Override
  public long nextLong() throws IOException {
    expect(JsonToken.STRING);
    return Long.parseLong(nextValue().value);
  }
  @Override
  public String nextName() throws IOException {
    expectedToken = JsonToken.NAME;
    expect(JsonToken.NAME);
    return nextValue().value;
  }


  private XmlTokenInfo nextXmlInfo() throws IOException, XmlPullParserException {
    final int type = xmlParser.next();

    if (type == XmlPullParser.TEXT && xmlParser.isWhitespace()) { return null; }

    final XmlTokenInfo info = new XmlTokenInfo();
    info.type = type;

    switch (type) {

    case XmlPullParser.START_TAG:
      info.name = xmlParser.getName();
      info.ns = xmlParser.getNamespace();
      final int aCount = xmlParser.getAttributeCount();
      if (aCount > 0) {
        final AttributesData attributes = new AttributesData(aCount);
        attributes.fill(xmlParser);
        info.attributesData = attributes;
      }
      break;

    case XmlPullParser.END_TAG:
      info.name = xmlParser.getName();
      info.ns = xmlParser.getNamespace();
      break;

    case XmlPullParser.TEXT:
      info.value = xmlParser.getText();
      break;

    case XmlPullParser.END_DOCUMENT:
      endReached = true;
      break;

    default:
      throw new IllegalStateException("Cannot process token type = " + type);
    }

    return info;
  }

  private void addToQueue(final JsonToken token) {
    final TokenRef tokenRef = new TokenRef();
    tokenRef.token = token;
    if (tokensQueue == null) {
      tokensQueue = tokenRef;
      tokensQueueStart = tokenRef;
    } else {
      tokensQueue.next = tokenRef;
      tokensQueue = tokenRef;
    }
  }
  private void pushToQueue(final JsonToken token) {
    final TokenRef tokenRef = new TokenRef();
    tokenRef.token = token;
    if (tokensQueueStart == null) {
      tokensQueueStart = tokenRef;
      tokensQueue = tokenRef;
    } else {
      tokenRef.next = tokensQueueStart;
      tokensQueueStart = tokenRef;
    }
  }
  private void addToQueue(final String value) {
    final ValueRef valueRef = new ValueRef();
    valueRef.value = value.trim();
    if (valuesQueue == null) {
      valuesQueue = valueRef;
      valuesQueueStart = valueRef;
    } else {
      valuesQueue.next = valueRef;
      valuesQueue = valueRef;
    }
  }
  private void addToQueue(final AttributesData attrData) throws IOException, XmlPullParserException {
    final int count = attrData.names.length;
    for (int i = 0; i < count; i++) {
      addToQueue(JsonToken.NAME);
      addToQueue("@" + attrData.getName(i));
      addToQueue(JsonToken.STRING);
      addToQueue(attrData.values[i]);
    }
  }

  private void fillQueues() throws IOException, XmlPullParserException {

    while (tokensQueue == null && !endReached) {
      final XmlTokenInfo xml = nextXmlInfo();
      if (endReached) {
        if (!skipRoot) { addToQueue(JsonToken.END_OBJECT); }
        break;
      }
      if (xml == null) { continue; }

      System.out.println(xml);

      switch (xml.type) {
      case XmlPullParser.START_TAG:
        if (firstStart) {
          firstStart = false;
          processRoot(xml);
        } else {
          processStart(xml);
        }
        break;
      case XmlPullParser.TEXT:
        processText(xml);
        break;
      case XmlPullParser.END_TAG:
        processEnd(xml);
        break;
      default:
      }

      dump(false);

      if (skipping) { break; }
    }
  }

  private void processRoot(final XmlTokenInfo xml) throws IOException, XmlPullParserException {
    if (!skipRoot) {

      addToQueue(expectedToken);
      push(Scope.INSIDE_OBJECT);
      processStart(xml);

    } else if (xml.attributesData != null) {

      addToQueue(JsonToken.BEGIN_OBJECT);
      push(Scope.INSIDE_OBJECT);
      addToQueue(xml.attributesData);

    } else {

      switch (expectedToken) {
      case BEGIN_OBJECT:
        addToQueue(JsonToken.BEGIN_OBJECT);
        push(Scope.INSIDE_OBJECT);
        break;
      case BEGIN_ARRAY:
        addToQueue(JsonToken.BEGIN_ARRAY);
        push(Scope.INSIDE_ARRAY);
        break;
      default:
        throw new IllegalStateException("First expectedToken=" + expectedToken + " (not begin_object/begin_array)");
      }

    }
  }

  private void processStart(final XmlTokenInfo xml) throws IOException, XmlPullParserException {

    boolean processTagName = true;

    Scope lastScope = stack[stackSize - 1];
    switch (lastScope) {

    case INSIDE_EMEDDED_ARRAY:
    case INSIDE_ARRAY:
      processTagName = false;
      // fall through

    case NAME:
      addToQueue(JsonToken.BEGIN_OBJECT);
      push(Scope.INSIDE_OBJECT);
      break;

    default:
    }

    if (processTagName) {
      // ignore tag name inside the array
      push(Scope.NAME);
      addToQueue(JsonToken.NAME);
      addToQueue(xml.getName(xmlParser));
    }

    if (xml.attributesData != null) {
      lastScope = stack[stackSize - 1];
      if (lastScope == Scope.NAME) {
        addToQueue(JsonToken.BEGIN_OBJECT);
        push(Scope.INSIDE_OBJECT);
      }
      // attributes, as fields
      addToQueue(xml.attributesData);
    }
  }

  private void processText(final XmlTokenInfo xml) {
    switch (stack[stackSize - 1]) {

    case INSIDE_OBJECT:
      String name = "$";
      if (textNameCounter > 0) { name += textNameCounter; }
      textNameCounter++;
      addToQueue(JsonToken.NAME);
      addToQueue(name);
      // fall-through

    default:
      addToQueue(JsonToken.STRING);
      addToQueue(xml.value);
    }
  }

  private void fixScopeStack() {
    stackSize--;
    if (stackSize > 0 && stack[stackSize - 1] == Scope.NAME) {
      stackSize--;
    }
  }

  private void processEnd(final XmlTokenInfo xml) {
    switch (stack[stackSize - 1]) {

    case INSIDE_OBJECT:
      addToQueue(JsonToken.END_OBJECT);
      textNameCounter = 0;
      fixScopeStack();
      break;

    case INSIDE_EMEDDED_ARRAY:
      addToQueue(JsonToken.END_ARRAY);
      addToQueue(JsonToken.END_OBJECT);
      stackSize--;
      fixScopeStack();
      break;

    case INSIDE_ARRAY:
      addToQueue(JsonToken.END_ARRAY);
      fixScopeStack();
      break;

    case NAME:
      fixScopeStack();
      break;

    default:
      // nothing
    }
  }

  private void push(final Scope scope) {
    if (stackSize == stack.length) {
      final Scope[] newStack = new Scope[stackSize * 2];
      System.arraycopy(stack, 0, newStack, 0, stackSize);
      stack = newStack;
    }
    stack[stackSize++] = scope;
  }

  private static final class TokenRef {
    JsonToken token;
    TokenRef next;
    @Override
    public String toString() {
      return token + ", " + next;
    }
  }
  private static final class ValueRef {
    String value;
    ValueRef next;
    @Override
    public String toString() {
      return value + ", " + next;
    }
  }

  static String nameWithNs(final String name, final String namespace, final XmlPullParser parser) throws XmlPullParserException {
    String result = name;
    String ns = namespace;
    if (ns != null && ns.length() > 0) {
      if (parser != null) {
        final int count = parser.getNamespaceCount(parser.getDepth());
        for (int i = 0; i < count; i++) {
          if (ns.equals(parser.getNamespaceUri(i))) {
            ns = parser.getNamespacePrefix(i);
            break;
          }
        }
      }
      result = "<" + ns + ">" + result;
    }
    return result;
  }

  private static final class XmlTokenInfo {
    int type;
    String name, value, ns;

    AttributesData attributesData;

    @Override
    public String toString() {
      return "xml "
          + (type == XmlPullParser.START_TAG ? "start" : type == XmlPullParser.END_TAG ? "end" : type)
          + " <" + ns + ":" + name + ">=" + value + (attributesData != null ? ", " + attributesData : "");
    }

    public String getName(final XmlPullParser parser) throws IOException, XmlPullParserException {
      return nameWithNs(name, ns, parser);
    }
  }

  private static final class AttributesData {
    final String[] names, values, ns;

    public AttributesData(final int count) {
      this.names = new String[count];
      this.values = new String[count];
      this.ns = new String[count];
    }

    public void fill(final XmlPullParser parser) {
      final int aCount = names.length;
      for (int i = 0; i < aCount; i++) {
        names[i] = parser.getAttributeName(i);
        ns[i] = parser.getAttributePrefix(i);
        values[i] = parser.getAttributeValue(i);
      }
    }

    public String getName(final int i) throws IOException, XmlPullParserException {
      return nameWithNs(names[i], ns[i], null);
    }

  }

}
