package org.keycloak.scim.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceFilterTest {

    @ParameterizedTest
    @ValueSource(strings = { "eq", "ne", "co", "sw", "ew", "gt", "ge", "lt", "le" })
    public void testJsonStringEscaping(String operator) throws Exception {
        assertFilter(operator, "a\nb\rc\td\be\f", "a\\nb\\rc\\td\\be\\f");
        assertFilter(operator, "\"\\", "\\\"\\\\");
        assertFilter(operator, "", "");
        assertFilter(operator, "plain / text \u00e9\u4e2d\ud83d\ude00", "plain / text \u00e9\u4e2d\ud83d\ude00");

        ObjectMapper mapper = new ObjectMapper();
        for (char control = 0; control < 0x20; control++) {
            String value = "a" + control + "b";
            String filter = comparison(operator, value).build();
            String literal = filter.substring(filter.indexOf('"'));
            assertEquals(value, mapper.readValue(literal, String.class),
                    "Round trip for control character " + (int) control);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "gt", "ge", "lt", "le" })
    public void testNumericValuesRemainUnquoted(String operator) {
        assertEquals("userName " + operator + " 42", comparison(operator, 42).build());
    }

    private void assertFilter(String operator, String value, String escaped) {
        assertEquals("userName " + operator + " \"" + escaped + "\"", comparison(operator, value).build());
    }

    private ResourceFilter comparison(String operator, Object value) {
        ResourceFilter filter = ResourceFilter.filter();
        return switch (operator) {
            case "eq" -> filter.eq("userName", (String) value);
            case "ne" -> filter.ne("userName", (String) value);
            case "co" -> filter.co("userName", (String) value);
            case "sw" -> filter.sw("userName", (String) value);
            case "ew" -> filter.ew("userName", (String) value);
            case "gt" -> filter.gt("userName", value);
            case "ge" -> filter.ge("userName", value);
            case "lt" -> filter.lt("userName", value);
            case "le" -> filter.le("userName", value);
            default -> throw new IllegalArgumentException(operator);
        };
    }
}
