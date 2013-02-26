package com.stanfy.gsonxml;

import java.io.IOException;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Reads XML as JSON.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public class XmlReader extends JsonReader {

  /** Internal token type. */
  private static final int START_TAG = 1, END_TAG = 2, VALUE = 3, IGNORE = -1;

  /** Scope. */
  private static enum Scope {
    /** We are inside an object. Next token should be {@link JsonToken#NAME} or {@link JsonToken#END_OBJECT}. */
    INSIDE_OBJECT,
    /** We are inside an array. Next token should be {@link JsonToken#BEGIN_OBJECT} or {@link JsonToken#END_ARRAY}. */
    INSIDE_ARRAY,
    /** We are inside automatically added array. Next token should be {@link JsonToken#BEGIN_OBJECT} or {@link JsonToken#END_ARRAY}. */
    INSIDE_EMBEDDED_ARRAY,
    /** We are inside primitive embedded array. Child scope can be #PRIMITIVE_VALUE only. */
    INSIDE_PRIMITIVE_EMBEDDED_ARRAY,
    /** We are inside primitive array. Child scope can be #PRIMITIVE_VALUE only. */
    INSIDE_PRIMITIVE_ARRAY,
    /** We are inside primitive value. Next token should be {@link JsonToken#STRING} or {@link JsonToken#END_ARRAY}. */
    PRIMITIVE_VALUE,
    /** New start tag met, we returned {@link JsonToken#NAME}. Object, array, or value can go next. */
    NAME
  }

  /** XML parser. */
  private final XmlPullParser xmlParser;

  /** Option. */
  private final Options options;

  /** Tokens queue. */
  private TokenRef tokensQueue, tokensQueueStart;
  /** Values queue. */
  private ValueRef valuesQueue, valuesQueueStart;

  private JsonToken expectedToken;

  /** State. */
  private boolean endReached, firstStart = true, lastTextWiteSpace = false;

  private JsonToken token;

  /** Counter for "$". */
  private int textNameCounter = 0;

  /** Stack. */
  private Scope[] stack = new Scope[32];
  /** Stack size. */
  private int stackSize = 0;

  private boolean skipping;

  /** Last XML token info. */
  private final XmlTokenInfo xmlToken = new XmlTokenInfo();

  /** Attributes. */
  private final AttributesData attributes = new AttributesData(10);

  public XmlReader(final Reader in, final XmlParserCreator creator, final Options options) {
    super(in);
    this.xmlParser = creator.createParser();
    this.options = options;
    this.xmlToken.type = IGNORE;
    try {
      this.xmlParser.setInput(in);
      this.xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, options.namespaces);
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

  private int cleanupScopeStack(final int count, final int oldStackSize) {
    int curStackSize = stackSize;
    if (oldStackSize < curStackSize) {
      for (int i = oldStackSize; i < curStackSize; i++) {
        stack[i - count] = stack[i];
      }
      stackSize -= count;
    } else {
      stackSize -= count - oldStackSize + curStackSize;
    }
    if (stackSize < 0) { stackSize = 0; }
    return oldStackSize - count;
  }

  private void adaptCurrentToken() throws XmlPullParserException, IOException {
    if (token == expectedToken) { return; }
    if (expectedToken != JsonToken.BEGIN_ARRAY) { return; }

    switch (token) {

    case BEGIN_OBJECT:

      token = JsonToken.BEGIN_ARRAY;

      final Scope lastScope = stack[stackSize - 1];

      if (peekNextToken() == JsonToken.NAME) {
        if (options.sameNameList) {
          cleanupScopeStack(3, stackSize);

          // use it as a field
          pushToQueue(JsonToken.BEGIN_OBJECT);

          push(Scope.INSIDE_EMBEDDED_ARRAY);
          push(Scope.INSIDE_OBJECT);
          if (lastScope == Scope.NAME) {
            push(Scope.NAME);
          }
        } else {
          // ignore name
          nextToken();
          nextValue();

          int pushPos = stackSize;
          if (options.primitiveArrays && peekNextToken() == null) {
            // pull what next: it can be either primitive or object
            fillQueues(true);
          }
          pushPos = cleanupScopeStack(3, pushPos);

          if (options.primitiveArrays && peekNextToken() == JsonToken.STRING) {
            // primitive
            pushAt(pushPos, Scope.INSIDE_PRIMITIVE_ARRAY);
          } else {
            // object (if array it will be adapted again)
            pushAt(pushPos, Scope.INSIDE_ARRAY);
            if (stackSize <= pushPos + 1 || stack[pushPos + 1] != Scope.INSIDE_OBJECT) {
              pushAt(pushPos + 1, Scope.INSIDE_OBJECT);
            }
            if (peekNextToken() != JsonToken.BEGIN_OBJECT) {
              pushToQueue(JsonToken.BEGIN_OBJECT);
            }
          }

        }
      }
      break;

    case STRING:
      if (options.sameNameList) {
        // we have array of primitives
        token = JsonToken.BEGIN_ARRAY;
        cleanupScopeStack(2, stackSize);

        pushToQueue(JsonToken.STRING);
        push(Scope.INSIDE_PRIMITIVE_EMBEDDED_ARRAY);

      } else {
        // we have empty list
        token = JsonToken.BEGIN_ARRAY;
        pushToQueue(JsonToken.END_ARRAY);
      }
      break;

    default:
    }

  }

  @Override
  public JsonToken peek() throws IOException {
    if (expectedToken == null && firstStart) { return JsonToken.BEGIN_OBJECT; }

    if (token != null) {
      try {
        adaptCurrentToken();
      } catch (final XmlPullParserException e) {
        throw new JsonSyntaxException("XML parsing exception", e);
      }
      expectedToken = null;
      return token;
    }

    try {

      fillQueues(false);
      expectedToken = null;

      return token = nextToken();

    } catch (final XmlPullParserException e) {
      throw new JsonSyntaxException("XML parsing exception", e);
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
    final int type = xmlParser.nextToken();

    final XmlTokenInfo info = this.xmlToken;
    info.clear();

    switch (type) {

    case XmlPullParser.START_TAG:
      info.type = START_TAG;
      info.name = xmlParser.getName();
      info.ns = xmlParser.getNamespace();
      final int aCount = xmlParser.getAttributeCount();
      if (aCount > 0) {
        attributes.fill(xmlParser);
        info.attributesData = attributes;
      }
      break;

    case XmlPullParser.END_TAG:
      info.type = END_TAG;
      info.name = xmlParser.getName();
      info.ns = xmlParser.getNamespace();
      break;

    case XmlPullParser.CDSECT:
    case XmlPullParser.TEXT:
      final String text = xmlParser.getText().trim();
      if (text.length() == 0) {
        lastTextWiteSpace = true;
        info.type = IGNORE;
        return info;
      }
      lastTextWiteSpace = false;
      info.type = VALUE;
      info.value = text;
      break;


    case XmlPullParser.END_DOCUMENT:
      endReached = true;
      // fall through

    default:
      info.type = IGNORE;
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
    final int count = attrData.count;
    for (int i = 0; i < count; i++) {
      addToQueue(JsonToken.NAME);
      addToQueue("@" + attrData.getName(i));
      addToQueue(JsonToken.STRING);
      addToQueue(attrData.values[i]);
    }
  }

  private void fillQueues(boolean force) throws IOException, XmlPullParserException {

    boolean mustRepeat = force;

    while ((tokensQueue == null && !endReached) || mustRepeat) {
      final XmlTokenInfo xml = nextXmlInfo();
      if (endReached) {
        if (!options.skipRoot) { addToQueue(JsonToken.END_OBJECT); }
        break;
      }
      if (xml.type == IGNORE) { continue; }

      mustRepeat = false;

//      System.out.println(xml);

      switch (xml.type) {
      case START_TAG:
        if (firstStart) {
          firstStart = false;
          processRoot(xml);
        } else {
          processStart(xml);
        }
        break;
      case VALUE:
        mustRepeat = processText(xml);
        break;
      case END_TAG:
        processEnd(xml);
        break;
      default:
      }

//      dump(false);

      if (skipping) { break; }
    }
  }

  private void processRoot(final XmlTokenInfo xml) throws IOException, XmlPullParserException {
    if (!options.skipRoot) {

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
        push(options.rootArrayPrimitive ? Scope.INSIDE_PRIMITIVE_ARRAY : Scope.INSIDE_ARRAY);
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

    case INSIDE_PRIMITIVE_ARRAY:
    case INSIDE_PRIMITIVE_EMBEDDED_ARRAY:
      processTagName = false;
      push(Scope.PRIMITIVE_VALUE);
      break;

    case INSIDE_EMBEDDED_ARRAY:
    case INSIDE_ARRAY:
      processTagName = false;
      // fall through

    case NAME:
      addToQueue(JsonToken.BEGIN_OBJECT);
      push(Scope.INSIDE_OBJECT);
      break;

    default:
    }

    if (processTagName) {                 // ignore tag name inside the array
      push(Scope.NAME);
      addToQueue(JsonToken.NAME);
      addToQueue(xml.getName(xmlParser));
      lastTextWiteSpace = true;           // if tag is closed immediately we'll add empty value to the queue
    }

    if (xml.attributesData != null) {
      lastScope = stack[stackSize - 1];
      if (lastScope == Scope.PRIMITIVE_VALUE) { throw new IllegalStateException("Attributes data in primitive scope"); }
      if (lastScope == Scope.NAME) {
        addToQueue(JsonToken.BEGIN_OBJECT);
        push(Scope.INSIDE_OBJECT);
      }
      // attributes, as fields
      addToQueue(xml.attributesData);
    }
  }

  private boolean processText(final XmlTokenInfo xml) {
    switch (stack[stackSize - 1]) {

    case PRIMITIVE_VALUE:
      addTextToQueue(xml.value, false);
      return false;

    case NAME:
      addTextToQueue(xml.value, true);
      return true;

    case INSIDE_OBJECT:
      String name = "$";
      if (textNameCounter > 0) { name += textNameCounter; }
      textNameCounter++;
      addToQueue(JsonToken.NAME);
      addToQueue(name);
      addTextToQueue(xml.value, false);
      return false;

    default:
      throw new JsonSyntaxException("Cannot process text '" + xml.value + "' inside scope " + stack[stackSize - 1]);
    }
  }

  private void addTextToQueue(final String value, final boolean canBeAppended) {
    if (canBeAppended && tokensQueue != null && tokensQueue.token == JsonToken.STRING) {
      if (value.length() > 0) {
        valuesQueue.value += " " + value;
      }
    } else {
      addToQueue(JsonToken.STRING);
      addToQueue(value);
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

    case PRIMITIVE_VALUE:
      stackSize--;
      break;

    case INSIDE_PRIMITIVE_EMBEDDED_ARRAY:
    case INSIDE_EMBEDDED_ARRAY:
      addToQueue(JsonToken.END_ARRAY);
      addToQueue(JsonToken.END_OBJECT);
      stackSize--;
      fixScopeStack();
      break;

    case INSIDE_PRIMITIVE_ARRAY:
    case INSIDE_ARRAY:
      addToQueue(JsonToken.END_ARRAY);
      fixScopeStack();
      break;

    case NAME:
      if (lastTextWiteSpace) {
        addTextToQueue("", true);
      }
      fixScopeStack();
      break;

    default:
      // nothing
    }
  }

  private void ensureStack() {
    if (stackSize == stack.length) {
      final Scope[] newStack = new Scope[stackSize * 2];
      System.arraycopy(stack, 0, newStack, 0, stackSize);
      stack = newStack;
    }
  }

  private void push(final Scope scope) {
    ensureStack();
    stack[stackSize++] = scope;
  }
  private void pushAt(final int position, final Scope scope) {
    int pos = position;
    if (pos < 0) { pos = 0; }
    ensureStack();
    for (int i = stackSize - 1; i >= pos; i--) {
      stack[i + 1] = stack[i];
    }
    stack[pos] = scope;
    stackSize++;
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

    public void clear() {
      type = IGNORE;
      name = null;
      value = null;
      ns = null;
      attributesData = null;
    }

    @Override
    public String toString() {
      return "xml "
          + (type == START_TAG ? "start" : type == END_TAG ? "end" : "value")
          + " <" + ns + ":" + name + ">=" + value + (attributesData != null ? ", " + attributesData : "");
    }

    public String getName(final XmlPullParser parser) throws IOException, XmlPullParserException {
      return nameWithNs(name, ns, parser);
    }
  }

  private static final class AttributesData {
    String[] names, values, ns;

    int count = 0;

    public AttributesData(final int capacity) {
      createArrays(capacity);
    }

    private void createArrays(final int capacity) {
      this.names = new String[capacity];
      this.values = new String[capacity];
      this.ns = new String[capacity];
    }

    public void fill(final XmlPullParser parser) {
      final int aCount = parser.getAttributeCount();
      if (aCount > names.length) {
        createArrays(aCount);
      }

      count = aCount;
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

  /** Xml reader options. */
  public static class Options {
    /** Options. */
    boolean primitiveArrays, skipRoot, sameNameList, namespaces, rootArrayPrimitive;
  }

}
