package org.keycloak.ssf.event;

import java.util.Map;
import java.util.function.Supplier;

import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsfEventsTest {

    @Test
    void builtInEvents_toStringOnFreshInstanceDoesNotThrow() {
        Map<String, Supplier<? extends SsfEvent>> factories =
                new DefaultSsfEventProviderFactory().getContributedEventFactories();
        assertFalse(factories.isEmpty(), "factory should contribute the built-in events");

        for (Map.Entry<String, Supplier<? extends SsfEvent>> entry : factories.entrySet()) {
            SsfEvent event = entry.getValue().get();
            String rendered = assertDoesNotThrow(event::toString,
                    () -> "toString() must not throw for fresh " + entry.getKey());
            assertNotNull(rendered, () -> "toString() must not return null for " + entry.getKey());
            assertFalse(rendered.isBlank(), () -> "toString() must not be blank for " + entry.getKey());
            assertFalse(rendered.contains("null"),
                    () -> "unset fields must be omitted, not rendered as null, for "
                            + entry.getKey() + ": " + rendered);
        }
    }

    @Test
    void unknownEventType_isRepresentedAsGenericSsfEvent() throws Exception {
        // A SET carrying an event type URI that is not in the registry:
        // SsfEventMapJsonDeserializer falls back to GenericSsfEvent, so the
        // full payload lands in the @JsonAnySetter extension attributes and
        // the real type URI is set after materialisation.
        String unknownType = "https://example.com/secevent/custom/unknown-event";
        String setJson = """
                {
                  "jti": "jti-123",
                  "iss": "https://issuer.example.test",
                  "events": {
                    "%s": {
                      "foo": "bar",
                      "count": 3,
                      "nested": { "a": 1 }
                    }
                  }
                }
                """.formatted(unknownType);

        SsfSecurityEventToken token = JsonSerialization.readValue(setJson, SsfSecurityEventToken.class);

        Object event = token.getEvents().get(unknownType);
        GenericSsfEvent generic = assertInstanceOf(GenericSsfEvent.class, event,
                "unknown event types must degrade to GenericSsfEvent, not fail parsing");
        assertEquals(unknownType, generic.getEventType(),
                "the deserializer must preserve the original event type URI");
        assertEquals("bar", generic.getAttributes().get("foo"),
                "payload fields must be preserved in the extension attributes");
        assertEquals(3, generic.getAttributes().get("count"));
        assertEquals(Map.of("a", 1), generic.getAttributes().get("nested"),
                "nested payload objects must be preserved as maps");

        generic.setAttributeValue("unset", null);

        String rendered = assertDoesNotThrow(generic::toString);
        assertTrue(rendered.contains(unknownType),
                "toString should render the preserved event type URI: " + rendered);
        assertTrue(rendered.contains("foo=bar"),
                "toString should render the preserved attributes: " + rendered);
        assertFalse(rendered.contains("unset="),
                "toString should omit null-valued attributes: " + rendered);
    }

    @Test
    void genericEvent_toStringOnFreshInstanceDoesNotThrow() {
        // GenericSsfEvent is the unknown-type fallback and is not part of the
        // factory contributions, so it is covered explicitly.
        GenericSsfEvent event = new GenericSsfEvent();
        String rendered = assertDoesNotThrow(event::toString);
        assertNotNull(rendered);
        assertFalse(rendered.isBlank());
    }

    @Test
    void caepEvent_toStringRendersOnlySetFields() {
        CaepCredentialChange event = new CaepCredentialChange();
        event.setCredentialType("password");
        event.setChangeType(CaepCredentialChange.ChangeType.UPDATE);

        String rendered = event.toString();
        assertTrue(rendered.contains("credentialType='password'"),
                "set String fields must be rendered quoted: " + rendered);
        assertTrue(rendered.contains("changeType="),
                "set fields must be rendered: " + rendered);
        assertFalse(rendered.contains("null"),
                "unset optional fields (friendlyName, x509*, …) must be omitted: " + rendered);
    }
}
