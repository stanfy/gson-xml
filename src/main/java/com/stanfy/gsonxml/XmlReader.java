package com.stanfy.gsonxml;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;

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
    INSIDE_OBJECT(false),
    /** We are inside an array. Next token should be {@link JsonToken#BEGIN_OBJECT} or {@link JsonToken#END_ARRAY}. */
    INSIDE_ARRAY(true),
    /** We are inside automatically added array. Next token should be {@link JsonToken#BEGIN_OBJECT} or {@link JsonToken#END_ARRAY}. */
    INSIDE_EMBEDDED_ARRAY(true),
    /** We are inside primitive embedded array. Child scope can be #PRIMITIVE_VALUE only. */
    INSIDE_PRIMITIVE_EMBEDDED_ARRAY(true),
    /** We are inside primitive array. Child scope can be #PRIMITIVE_VALUE only. */
    INSIDE_PRIMITIVE_ARRAY(true),
    /** We are inside primitive value. Next token should be {@link JsonToken#STRING} or {@link JsonToken#END_ARRAY}. */
    PRIMITIVE_VALUE(false),
    /** New start tag met, we returned {@link JsonToken#NAME}. Object, array, or value can go next. */
    NAME(false);

    /** Inside array flag. */
    final boolean insideArray;

    private Scope(final boolean insideArray) {
      this.insideArray = insideArray;
    }
  }

  /** XML parser. */
  private final XmlPullParser xmlParser;

  /** Option. */
  final Options options;

  /** Tokens pool. */
  private final RefsPool<TokenRef> tokensPool = new RefsPool<TokenRef>(new Creator<TokenRef>() {
    public TokenRef create() { return new TokenRef(); }
  });
  /** Values pool. */
  private final RefsPool<ValueRef> valuesPool = new RefsPool<ValueRef>(new Creator<ValueRef>() {
    public ValueRef create() { return new ValueRef(); }
  });

  /** Tokens queue. */
  private TokenRef tokensQueue, tokensQueueStart;
  /** Values queue. */
  private ValueRef valuesQueue, valuesQueueStart;

  private JsonToken expectedToken;

  /** State. */
  private boolean endReached, firstStart = true, lastTextWhiteSpace = false;

  /** Stack of scopes. */
  private final Stack<Scope> scopeStack = new Stack<Scope>();
  /** Stack of last closed tags. */
  private final Stack<ClosedTag> closeStack = new Stack<ClosedTag>();

  /** Current token. */
  private JsonToken token;

  /** Counter for "$". */
  private int textNameCounter = 0;

  /** Skipping state flag. */
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
  private CharSequence dump() {
    return new StringBuilder()
      .append("Scopes: ").append(scopeStack).append('\n')
      .append("Closed tags: ").append(closeStack).append('\n')
      .append("Token: ").append(token).append('\n')
      .append("Tokens queue: ").append(tokensQueueStart).append('\n')
      .append("Values queue: ").append(valuesQueueStart).append('\n');
  }

  @Override
  public String toString() { return "--- XmlReader ---\n" + dump(); }

  private JsonToken peekNextToken() { return tokensQueueStart != null ? tokensQueueStart.token : null; }

  private JsonToken nextToken() {
    final TokenRef ref = tokensQueueStart;
    if (ref == null) {
      return JsonToken.END_DOCUMENT;
    }

    tokensQueueStart = ref.next;
    if (ref == tokensQueue) { tokensQueue = null; }
    tokensPool.release(ref);
    return ref.token;
  }

  private ValueRef nextValue() {
    final ValueRef ref = valuesQueueStart;
    if (ref == null) { throw new IllegalStateException("No value can be given"); }
    if (ref == valuesQueue) { valuesQueue = null; }
    valuesPool.release(ref);
    valuesQueueStart = ref.next;
    return ref;
  }

  private void expect(final JsonToken token) throws IOException {
    final JsonToken actual = peek();
    this.token = null;
    if (actual != token) { throw new IllegalStateException(token + " expected, but met " + actual + "\n" + dump()); }
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

  private void adaptCurrentToken() throws XmlPullParserException, IOException {
    if (token == expectedToken) { return; }
    if (expectedToken != JsonToken.BEGIN_ARRAY) { return; }

    switch (token) {

    case BEGIN_OBJECT:

      token = JsonToken.BEGIN_ARRAY;

      final Scope lastScope = scopeStack.peek();

      if (peekNextToken() == JsonToken.NAME) {
        if (options.sameNameList) {
          // we are replacing current scope with INSIDE_EMBEDDED_ARRAY
          scopeStack.cleanup(1);

          // use it as a field
          pushToQueue(JsonToken.BEGIN_OBJECT);

          scopeStack.push(Scope.INSIDE_EMBEDDED_ARRAY);
          scopeStack.push(Scope.INSIDE_OBJECT);
          if (lastScope == Scope.NAME) {
            scopeStack.push(Scope.NAME);
          }
        } else {
          // ignore name
          nextToken();
          nextValue();

          int pushPos = scopeStack.size();
          if (options.primitiveArrays && peekNextToken() == null) {
            // pull what next: it can be either primitive or object
            fillQueues(true);
          }
          pushPos = scopeStack.cleanup(3, pushPos);

          if (options.primitiveArrays && peekNextToken() == JsonToken.STRING) {
            // primitive
            scopeStack.pushAt(pushPos, Scope.INSIDE_PRIMITIVE_ARRAY);
          } else {
            // object (if array it will be adapted again)
            scopeStack.pushAt(pushPos, Scope.INSIDE_ARRAY);
            if (scopeStack.size() <= pushPos + 1 || scopeStack.get(pushPos + 1) != Scope.INSIDE_OBJECT) {
              scopeStack.pushAt(pushPos + 1, Scope.INSIDE_OBJECT);
            }
            if (peekNextToken() != JsonToken.BEGIN_OBJECT) {
              pushToQueue(JsonToken.BEGIN_OBJECT);
            }
          }

        }
      }
      break;

    case STRING:
      token = JsonToken.BEGIN_ARRAY;
      if (options.sameNameList) {

        if (options.primitiveArrays) {
          // we have array of primitives
          pushToQueue(JsonToken.STRING);
          scopeStack.push(Scope.INSIDE_PRIMITIVE_EMBEDDED_ARRAY);
        } else {
          // pass value as a text node inside of an object
          String value = nextValue().value;
          pushToQueue(JsonToken.END_OBJECT);
          pushToQueue(JsonToken.STRING);
          pushToQueue(JsonToken.NAME);
          pushToQueue(JsonToken.BEGIN_OBJECT);
          pushToQueue(value);
          pushToQueue("$");
          scopeStack.push(Scope.INSIDE_EMBEDDED_ARRAY);
        }

      } else {
        // we have an empty list
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
    final int type = xmlParser.next();

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

    case XmlPullParser.TEXT:
      final String text = xmlParser.getText().trim();
      if (text.length() == 0) {
        lastTextWhiteSpace = true;
        info.type = IGNORE;
        return info;
      }
      lastTextWhiteSpace = false;
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
    final TokenRef tokenRef = tokensPool.get();
    tokenRef.token = token;
    tokenRef.next = null;

    if (tokensQueue == null) {
      tokensQueue = tokenRef;
      tokensQueueStart = tokenRef;
    } else {
      tokensQueue.next = tokenRef;
      tokensQueue = tokenRef;
    }
  }
  private void pushToQueue(final JsonToken token) {
    final TokenRef tokenRef = tokensPool.get();
    tokenRef.token = token;
    tokenRef.next = null;

    if (tokensQueueStart == null) {
      tokensQueueStart = tokenRef;
      tokensQueue = tokenRef;
    } else {
      tokenRef.next = tokensQueueStart;
      tokensQueueStart = tokenRef;
    }
  }
  private void addToQueue(final String value) {
    final ValueRef valueRef = valuesPool.get();
    valueRef.value = value.trim();
    valueRef.next = null;

    if (valuesQueue == null) {
      valuesQueue = valueRef;
      valuesQueueStart = valueRef;
    } else {
      valuesQueue.next = valueRef;
      valuesQueue = valueRef;
    }
  }
  private void pushToQueue(final String value) {
    final ValueRef valueRef = valuesPool.get();
    valueRef.value = value;
    valueRef.next = null;

    if (valuesQueueStart == null) {
      valuesQueue = valueRef;
      valuesQueueStart = valueRef;
    } else {
      valueRef.next = valuesQueueStart;
      valuesQueueStart = valueRef;
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

      if (!mustRepeat && skipping) { break; }
    }
  }

  private void processRoot(final XmlTokenInfo xml) throws IOException, XmlPullParserException {
    if (!options.skipRoot) {

      addToQueue(expectedToken);
      scopeStack.push(Scope.INSIDE_OBJECT);
      processStart(xml);

    } else if (xml.attributesData != null) {

      addToQueue(JsonToken.BEGIN_OBJECT);
      scopeStack.push(Scope.INSIDE_OBJECT);
      addToQueue(xml.attributesData);

    } else {

      switch (expectedToken) {
      case BEGIN_OBJECT:
        addToQueue(JsonToken.BEGIN_OBJECT);
        scopeStack.push(Scope.INSIDE_OBJECT);
        break;
      case BEGIN_ARRAY:
        addToQueue(JsonToken.BEGIN_ARRAY);
        scopeStack.push(options.rootArrayPrimitive ? Scope.INSIDE_PRIMITIVE_ARRAY : Scope.INSIDE_ARRAY);
        break;
      default:
        throw new IllegalStateException("First expectedToken=" + expectedToken + " (not begin_object/begin_array)");
      }

    }
  }

  private void processStart(final XmlTokenInfo xml) throws IOException, XmlPullParserException {

    boolean processTagName = true;

    Scope lastScope = scopeStack.peek();

    if (options.sameNameList && lastScope.insideArray && closeStack.size() > 0) {
      ClosedTag lastClosedInfo = closeStack.peek();
      if (lastClosedInfo.depth == xmlParser.getDepth()) {
        String currentName = options.namespaces ? xml.getName(xmlParser) : xml.name;
        if (!currentName.equals(lastClosedInfo.name)) {
          // close the previous array
          addToQueue(JsonToken.END_ARRAY);
          fixScopeStack();
          lastScope = scopeStack.peek();
        }
      }
    }

    switch (lastScope) {

    case INSIDE_PRIMITIVE_ARRAY:
    case INSIDE_PRIMITIVE_EMBEDDED_ARRAY:
      processTagName = false;
      scopeStack.push(Scope.PRIMITIVE_VALUE);
      break;

    case INSIDE_EMBEDDED_ARRAY:
    case INSIDE_ARRAY:
      processTagName = false;
      // fall through

    case NAME:
      addToQueue(JsonToken.BEGIN_OBJECT);
      scopeStack.push(Scope.INSIDE_OBJECT);
      break;

    default:
    }

    if (processTagName) {                 // ignore tag name inside the array
      scopeStack.push(Scope.NAME);
      addToQueue(JsonToken.NAME);
      addToQueue(xml.getName(xmlParser));
      lastTextWhiteSpace = true;           // if tag is closed immediately we'll add empty value to the queue
    }

    if (xml.attributesData != null) {
      lastScope = scopeStack.peek();
      if (lastScope == Scope.PRIMITIVE_VALUE) { throw new IllegalStateException("Attributes data in primitive scope"); }
      if (lastScope == Scope.NAME) {
        addToQueue(JsonToken.BEGIN_OBJECT);
        scopeStack.push(Scope.INSIDE_OBJECT);
      }
      // attributes, as fields
      addToQueue(xml.attributesData);
    }
  }

  private boolean processText(final XmlTokenInfo xml) {
    switch (scopeStack.peek()) {

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
      throw new JsonSyntaxException("Cannot process text '" + xml.value + "' inside scope " + scopeStack.peek());
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
    scopeStack.fix(Scope.NAME);
  }

  private void processEnd(final XmlTokenInfo xml) throws IOException, XmlPullParserException {
    switch (scopeStack.peek()) {

    case INSIDE_OBJECT:
      addToQueue(JsonToken.END_OBJECT);
      textNameCounter = 0;
      fixScopeStack();
      break;

    case PRIMITIVE_VALUE:
      scopeStack.drop();
      break;

    case INSIDE_PRIMITIVE_EMBEDDED_ARRAY:
    case INSIDE_EMBEDDED_ARRAY:
      addToQueue(JsonToken.END_ARRAY);
      addToQueue(JsonToken.END_OBJECT);
      fixScopeStack(); // auto-close embedded array
      fixScopeStack(); // close current object scope
      break;

    case INSIDE_PRIMITIVE_ARRAY:
    case INSIDE_ARRAY:
      addToQueue(JsonToken.END_ARRAY);
      fixScopeStack();
      break;

    case NAME:
      if (lastTextWhiteSpace) {
        addTextToQueue("", true);
      }
      fixScopeStack();
      break;

    default:
      // nothing
    }

    if (options.sameNameList) {
      int stackSize = xmlParser.getDepth();
      final String name = options.namespaces ? xml.getName(xmlParser) : xml.name;
      final Stack<ClosedTag> closeStack = this.closeStack;
      boolean nameChange = false;
      while (closeStack.size() > 0 && closeStack.peek().depth > stackSize) {
        closeStack.drop();
      }
      if (closeStack.size() == 0 || closeStack.peek().depth < stackSize) {
        closeStack.push(new ClosedTag(stackSize, name));
      } else {
        closeStack.peek().name = name;
      }
    }
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

  private final class AttributesData {
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
        if (options.namespaces) {
          ns[i] = parser.getAttributePrefix(i);
        }
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

  /** Closed tag data. */
  private static class ClosedTag {
    int depth;
    String name;

    public ClosedTag(final int depth, final String name) {
      this.depth = depth;
      this.name = name;
    }

    @Override
    public String toString() {
      return "'" + name + "'/" + depth;
    }
  }

  /** Pool for  */
  private static final class RefsPool<T> {

    /** Max count. */
    private static final int SIZE = 32;

    /** Factory instance. */
    private final Creator<T> creator;

    /** Pool. */
    private final Object[] store = new Object[SIZE];

    /** Store length. */
    private int len = 0;

    public RefsPool(final Creator<T> factory) {
      this.creator = factory;
    }

    /** Get value from pool or create it. */
    public T get() {
      if (len == 0) { return creator.create(); }
      return (T)store[--len];
    }

    /** Return value to the pool. */
    public void release(final T obj) {
      if (len < SIZE) {
        store[len++] = obj;
      }
    }

  }

  /** Factory. */
  private interface Creator<T> { T create(); }

}
