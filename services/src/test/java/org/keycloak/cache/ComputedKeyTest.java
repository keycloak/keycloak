package org.keycloak.cache;

import java.util.Map;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class ComputedKeyTest {

    @Test
    public void testComputedKeyWithStrings() {
        String k1 = ComputedKey.computeKey("realm", "type", "key1");
        Assertions.assertEquals(k1, ComputedKey.computeKey("realm", "type", "key1"));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm2", "type", "key"));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm", "type2", "key"));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm", "type", "key2"));
    }

    @Test
    public void testComputedKeyWithAttributes() {
        String k1 = ComputedKey.computeKey("realm", "type", Map.of("one", "one", "two", "two"));
        Assertions.assertEquals(k1, ComputedKey.computeKey("realm", "type", Map.of("one", "one", "two", "two")));
        Assertions.assertEquals(k1, ComputedKey.computeKey("realm", "type", Map.of("two", "two", "one", "one")));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm2", "type", Map.of("one", "one", "two", "two")));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm", "type2", Map.of("one", "one", "two", "two")));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm", "type", Map.of("one2", "one", "two", "two")));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm", "type", Map.of("one", "one2", "two", "two")));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm", "type", Map.of("one", "one", "two2", "two")));
        Assertions.assertNotEquals(k1, ComputedKey.computeKey("realm", "type", Map.of("one", "one", "two", "two2")));
    }

}
