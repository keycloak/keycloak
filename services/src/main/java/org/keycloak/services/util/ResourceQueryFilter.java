package org.keycloak.services.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides support for filtering result sets by a String query.
 *
 * The query format is a space-separated list of field:value pairs, where value can have four formats:
 * - a single word (e.g. `foo:bar`)
 * - a quoted string with support for spaces and special characters (e.g. `foo:"bar baz"`)
 * - a list of values (e.g. `foo:[bar, baz]`); if the target field is a map, this checks if given keys are present (ignoring values)
 * - a map of key-value pairs (e.g. `foo:[bar:baz, qux:quux]`)
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ResourceQueryFilter<T> {
    public static final Pattern singleFieldPattern = Pattern.compile("([a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*):(?:([\\S&&[^\\[\"\\]:]]+)|\"([^\"]+)\"|\\[([^\\]]+)])");
    public static final Pattern fullQueryPattern = Pattern.compile(String.format("^%s(\\s+%s)*$", singleFieldPattern, singleFieldPattern));
    public static final Pattern listPattern = Pattern.compile("^[\\S&&[^,:]]+(,\\s*[\\S&&[^,:]]+)*$");
    public static final Pattern mapPattern = Pattern.compile("^[\\S&&[^,:]]+:[\\S&&[^,]]+(,\\s*[\\S&&[^,:]]+:[\\S&&[^,:]]+)*$");

    private final String query;
    private final Map<String, Object> parsedQuery = new HashMap<>();

    public ResourceQueryFilter(String q) {
        query = q.trim();
        parseQuery();
    }

    private void parseQuery() {
        Matcher matcher = fullQueryPattern.matcher(query);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid query format: " + query);
        }

        matcher = singleFieldPattern.matcher(query);
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = null;

            if (matcher.group(2) != null) { // foo:bar
                value = matcher.group(2);
            } else if (matcher.group(3) != null) { // foo:"bar baz"
                value = matcher.group(3);
            } else if (matcher.group(4) != null) { // foo:[bar, baz], or foo:[bar:baz, qux:quux]
                String valueStr = matcher.group(4);
                // No support for nested lists or maps, quotes (i.e. whitespace chars, commas, colons)
                String[] values = valueStr.split(",\\s*");
                if (mapPattern.matcher(valueStr).matches()) {
                    value = Arrays.stream(values).collect(Collectors.toMap(v -> v.substring(0, v.indexOf(":")), v -> v.substring(v.indexOf(":") + 1)));
                } else if (listPattern.matcher(valueStr).matches()) {
                    value = Arrays.asList(values);
                } else {
                    throw new IllegalArgumentException("Invalid value format: " + valueStr);
                }
            }

            parsedQuery.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public Stream<T> filterByQuery(Stream<T> stream) {
        for (Map.Entry<String, Object> expression : parsedQuery.entrySet()) {
            String[] key = expression.getKey().split("\\.");
            Object expectedValue = expression.getValue();

            stream = stream.filter(r -> {

                // iteratively go through the getters to get the final actual value
                Object actualValue = r;
                for (String fieldName : key) {
                    try {
                        actualValue = findGetter(actualValue.getClass(), fieldName).invoke(actualValue);
                    } catch (NoSuchMethodException e) {
                        return false; // field not found, can't match
                    } catch (Exception e) {
                        throw new RuntimeException("Error invoking getter for field: " + fieldName, e);
                    }
                    if (actualValue == null) return false; // field not set, can't match
                }

                // TODO fix this code, it's a mess
                if (actualValue instanceof List || actualValue instanceof Set) {
                    if (!(expectedValue instanceof List)) return false;
                    List<String> expectedList = (List<String>) expectedValue;

                    // we're doing unnecessary loops here, we should optimize this
                    return ((Collection<?>) actualValue).stream().map(Object::toString).collect(Collectors.toSet()).containsAll(expectedList);
                } else if (actualValue instanceof Map) { // check for exact match if expectedValue is a map
                    // considering the key is a String
                    Map<String, ?> actualMap;
                    try {
                        actualMap = (Map<String, ?>) actualValue;
                    } catch (ClassCastException e) {
                        return false;
                    }

                    if (expectedValue instanceof Map) {
                        Map<String, String> expectedMap = (Map<String, String>) expectedValue;
                        for (Map.Entry<String, ?> expectedEntry : expectedMap.entrySet()) {
                            Object actualMapValue = actualMap.get(expectedEntry.getKey());
                            if (actualMapValue == null || !expectedEntry.getValue().equals(actualMapValue.toString())) {
                                return false;
                            }
                        }
                        return true;
                    } else if (expectedValue instanceof List) { // check for key only if expectedValue is a list
                        // if expectedValue is a list, check if any of the values in the map match
                        List<String> expectedList = (List<String>) expectedValue;
                        for (String expectedKey : expectedList) {
                            if (!actualMap.containsKey(expectedKey)) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        return false;
                    }
                } else { // otherwise check for String match
                    return expectedValue.toString().equals(actualValue.toString());
                }
            });
        }

        return stream;
    }

    private Method findGetter(Class<?> clazz, String key) throws NoSuchMethodException {
        String adjustedKey = Character.toUpperCase(key.charAt(0)) + key.substring(1);

        // this is not optimal
        // TODO at least cache the getters
        try {
            return clazz.getMethod("get" + adjustedKey);
        } catch (NoSuchMethodException e) {
            return clazz.getMethod("is" + adjustedKey);
        }
    }

    public Map<String, Object> getParsedQuery() {
        return Collections.unmodifiableMap(parsedQuery);
    }
}
