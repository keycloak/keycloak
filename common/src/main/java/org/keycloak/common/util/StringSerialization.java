package org.keycloak.common.util;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities to serialize objects to string. Type safety is not guaranteed here and is responsibility of the caller.
 * @author hmlnarik
 */
public class StringSerialization {

    // Since there is still need to support JDK 7, we have to work without functional interfaces
    private static enum DeSerializerFunction {
        OBJECT {
            @Override public String serialize(Object o)   { return o.toString(); }
            @Override public Object deserialize(String s) { return s; }
        },
        URI {
            @Override public String serialize(Object o)   { return o.toString(); }
            @Override public Object deserialize(String s) { return java.net.URI.create(s); }
        },
        ;

        /** Serialize value which is guaranteed to be non-null */
        public abstract String serialize(Object o);
        public abstract Object deserialize(String s);
    }

    private static final Map<Class<?>, DeSerializerFunction> WELL_KNOWN_DESERIALIZERS = new LinkedHashMap<>();
    private static final String SEPARATOR = ";";
    private static final Pattern ESCAPE_PATTERN = Pattern.compile(SEPARATOR);
    private static final Pattern UNESCAPE_PATTERN = Pattern.compile(SEPARATOR + SEPARATOR);
    private static final Pattern VALUE_PATTERN = Pattern.compile("([NV])" +
      "(" +
        "(?:[^" + SEPARATOR + "]|" + SEPARATOR + SEPARATOR + ")*?" +
      ")($|" + SEPARATOR + "(?!" + SEPARATOR + "))",
      Pattern.DOTALL
    );

    static {
        WELL_KNOWN_DESERIALIZERS.put(URI.class, DeSerializerFunction.URI);
        WELL_KNOWN_DESERIALIZERS.put(String.class, DeSerializerFunction.OBJECT);
    }

    /**
     * Serialize given objects as strings separated by {@link #SEPARATOR} according to the {@link #WELL_KNOWN_SERIALIZERS}.
     * @param toSerialize
     * @return
     */
    public static String serialize(Object... toSerialize) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < toSerialize.length; i ++) {
            Object o = toSerialize[i];
            String stringO = getStringFrom(o);
            String escapedStringO = ESCAPE_PATTERN.matcher(stringO).replaceAll(SEPARATOR + SEPARATOR);
            sb.append(escapedStringO);

            if (i < toSerialize.length - 1) {
                sb.append(SEPARATOR);
            }
        }

        return sb.toString();
    }

    public static Deserializer deserialize(String what) {
        return new Deserializer(what);
    }

    private static String getStringFrom(Object o) {
        if (o == null) {
            return "N";
        }

        Class<?> c = o.getClass();
        DeSerializerFunction f = WELL_KNOWN_DESERIALIZERS.get(c);
        return "V" + (f == null ? o : f.serialize(o));
    }

    private static <T> T getObjectFrom(String escapedString, Class<T> clazz) {
        DeSerializerFunction f = WELL_KNOWN_DESERIALIZERS.get(clazz);
        Object res = f == null ? escapedString : f.deserialize(escapedString);
        return clazz.cast(res);
    }

    public static class Deserializer {

        private final Matcher valueMatcher;

        public Deserializer(String what) {
            this.valueMatcher = VALUE_PATTERN.matcher(what);
        }

        public <T> T next(Class<T> clazz) {
            if (! this.valueMatcher.find()) {
                return null;
            }
            String valueOrNull = this.valueMatcher.group(1);
            if (valueOrNull == null || Objects.equals(valueOrNull, "N")) {
                return null;
            }
            String escapedStringO = this.valueMatcher.group(2);
            String unescapedStringO = UNESCAPE_PATTERN.matcher(escapedStringO).replaceAll(SEPARATOR);
            return getObjectFrom(unescapedStringO, clazz);
        }
    }
}
