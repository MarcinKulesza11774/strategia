package util;

import java.util.*;

/**
 * Minimalistyczny parser JSON bez zewnętrznych zależności.
 * Obsługuje: obiekty {}, tablice [], stringi "", liczby, boolean, null.
 *
 * Użycie:
 *   Object root = JsonParser.parse(jsonString);
 *   Map<String,Object> obj = JsonParser.asMap(root);
 *   List<Object>       arr = JsonParser.asList(root);
 *   String  s = JsonParser.asString(obj.get("key"));
 *   int     i = JsonParser.asInt(obj.get("key"));
 *   double  d = JsonParser.asDouble(obj.get("key"));
 *   boolean b = JsonParser.asBool(obj.get("key"));
 */
public class JsonParser {

    private final String src;
    private int          pos;

    private JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static Object parse(String json) {
        return new JsonParser(json.trim()).readValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object o) {
        if (o instanceof Map) return (Map<String, Object>) o;
        throw new IllegalArgumentException("Expected object, got: " + o);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object o) {
        if (o instanceof List) return (List<Object>) o;
        throw new IllegalArgumentException("Expected array, got: " + o);
    }

    public static String asString(Object o) {
        if (o == null) return null;
        return o.toString();
    }

    public static int asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s)  return Integer.parseInt(s.trim());
        throw new IllegalArgumentException("Expected number, got: " + o);
    }

    public static double asDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s)  return Double.parseDouble(s.trim());
        throw new IllegalArgumentException("Expected number, got: " + o);
    }

    public static boolean asBool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof String  s) return s.equalsIgnoreCase("true");
        throw new IllegalArgumentException("Expected boolean, got: " + o);
    }

    /** Bezpieczny getter z domyślną wartością. */
    public static int getInt(Map<String,Object> map, String key, int def) {
        Object v = map.get(key);
        return v == null ? def : asInt(v);
    }

    public static double getDouble(Map<String,Object> map, String key, double def) {
        Object v = map.get(key);
        return v == null ? def : asDouble(v);
    }

    public static boolean getBool(Map<String,Object> map, String key, boolean def) {
        Object v = map.get(key);
        return v == null ? def : asBool(v);
    }

    public static String getString(Map<String,Object> map, String key, String def) {
        Object v = map.get(key);
        return v == null ? def : asString(v);
    }

    // -------------------------------------------------------------------------
    // Parser internals
    // -------------------------------------------------------------------------

    private Object readValue() {
        skipWs();
        if (pos >= src.length()) throw new RuntimeException("Unexpected end of JSON");
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't', 'f' -> readBool();
            case 'n' -> readNull();
            default  -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // skip '{'
        skipWs();
        if (pos < src.length() && src.charAt(pos) == '}') { pos++; return map; }
        while (pos < src.length()) {
            skipWs();
            String key = readString();
            skipWs();
            expect(':');
            skipWs();
            Object val = readValue();
            map.put(key, val);
            skipWs();
            if (pos < src.length() && src.charAt(pos) == ',') { pos++; continue; }
            break;
        }
        skipWs();
        expect('}');
        return map;
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        pos++; // skip '['
        skipWs();
        if (pos < src.length() && src.charAt(pos) == ']') { pos++; return list; }
        while (pos < src.length()) {
            skipWs();
            list.add(readValue());
            skipWs();
            if (pos < src.length() && src.charAt(pos) == ',') { pos++; continue; }
            break;
        }
        skipWs();
        expect(']');
        return list;
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\' && pos < src.length()) {
                char esc = src.charAt(pos++);
                sb.append(switch (esc) {
                    case '"'  -> '"';
                    case '\\' -> '\\';
                    case '/'  -> '/';
                    case 'n'  -> '\n';
                    case 'r'  -> '\r';
                    case 't'  -> '\t';
                    default   -> esc;
                });
            } else {
                sb.append(c);
            }
        }
        throw new RuntimeException("Unterminated string");
    }

    private Number readNumber() {
        int start = pos;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ',' || c == '}' || c == ']' || c == ' ' || c == '\n' || c == '\r' || c == '\t') break;
            pos++;
        }
        String num = src.substring(start, pos).trim();
        if (num.contains(".") || num.contains("e") || num.contains("E"))
            return Double.parseDouble(num);
        return Long.parseLong(num);
    }

    private Boolean readBool() {
        if (src.startsWith("true",  pos)) { pos += 4; return true;  }
        if (src.startsWith("false", pos)) { pos += 5; return false; }
        throw new RuntimeException("Invalid boolean at pos " + pos);
    }

    private Object readNull() {
        if (src.startsWith("null", pos)) { pos += 4; return null; }
        throw new RuntimeException("Invalid null at pos " + pos);
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c)
            throw new RuntimeException("Expected '" + c + "' at pos " + pos +
                    ", got: " + (pos < src.length() ? src.charAt(pos) : "EOF"));
        pos++;
    }
}
