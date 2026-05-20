package org.keycloak.ssf.transmitter.subject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link SsfNotifyAttributes} attribute key derivation
 * and value semantics.
 */
class SsfNotifyAttributesTest {

    @Test
    void attributeKey_prefixesClientId() {
        assertEquals("ssf.notify.abc-123", SsfNotifyAttributes.attributeKey("abc-123"));
    }

    @Test
    void attributeKey_handlesUuid() {
        String clientId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        assertEquals("ssf.notify." + clientId, SsfNotifyAttributes.attributeKey(clientId));
    }

    @Test
    void attributePrefix_isConsistent() {
        assertEquals("ssf.notify.", SsfNotifyAttributes.ATTRIBUTE_PREFIX);
    }
}
