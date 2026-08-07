package org.keycloak.ssf.event.risc;

import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-format tests for the RISC account-state events.
 *
 * <p>{@code event_timestamp} and {@code initiating_entity} are CAEP-defined
 * claims that these events carry as extensions — RISC 1.0 lists no attributes
 * for account-disabled / account-enabled. They are declared on the concrete
 * event classes rather than inherited, so these tests pin the serialized names
 * and the omit-when-unset behaviour that the shared base class no longer
 * provides.
 */
class RiscAccountEventsTest {

    @Test
    void accountDisabled_serializesReasonTimestampAndInitiatingEntity() throws Exception {
        RiscAccountDisabled event = new RiscAccountDisabled();
        event.setReason(RiscAccountDisabled.REASON_BRUTE_FORCE);
        event.setEventTimestamp(1712345678L);
        event.setInitiatingEntity(InitiatingEntity.POLICY);

        JsonNode json = JsonSerialization.mapper.valueToTree(event);

        assertEquals(RiscAccountDisabled.REASON_BRUTE_FORCE, json.path("reason").asText());
        assertEquals(1712345678L, json.path("event_timestamp").asLong());
        assertEquals("policy", json.path("initiating_entity").asText(),
                "initiating_entity must serialize via the enum's @JsonValue code");
    }

    @Test
    void accountEnabled_serializesTimestampAndInitiatingEntity() throws Exception {
        RiscAccountEnabled event = new RiscAccountEnabled();
        event.setEventTimestamp(1712345678L);
        event.setInitiatingEntity(InitiatingEntity.ADMIN);

        JsonNode json = JsonSerialization.mapper.valueToTree(event);

        assertEquals(1712345678L, json.path("event_timestamp").asLong());
        assertEquals("admin", json.path("initiating_entity").asText());
    }

    @Test
    void unsetClaims_areOmittedFromWireJson() throws Exception {
        JsonNode disabled = JsonSerialization.mapper.valueToTree(new RiscAccountDisabled());

        // @JsonInclude(NON_NULL) on SsfEvent must still apply to the claims now
        // declared on the concrete class — a primitive long would have emitted 0.
        assertTrue(disabled.path("event_timestamp").isMissingNode(),
                "unset event_timestamp must be omitted, not serialized as 0: " + disabled);
        assertTrue(disabled.path("initiating_entity").isMissingNode(),
                "unset initiating_entity must be omitted: " + disabled);
        assertTrue(disabled.path("reason").isMissingNode(),
                "unset reason must be omitted: " + disabled);
    }

    @Test
    void toStringRendersDeclaredClaims() {
        RiscAccountDisabled event = new RiscAccountDisabled();
        event.setReason(RiscAccountDisabled.REASON_ADMIN);
        event.setInitiatingEntity(InitiatingEntity.ADMIN);

        String rendered = event.toString();

        // The events override appendFields rather than toString, so the claims
        // must appear without the base class knowing about them.
        assertTrue(rendered.contains("reason='" + RiscAccountDisabled.REASON_ADMIN + "'"),
                "declared String claims must render quoted: " + rendered);
        assertTrue(rendered.contains("initiatingEntity=ADMIN"),
                "declared claims must render: " + rendered);
        assertFalse(rendered.contains("=null"),
                "unset claims must be omitted, not rendered as null: " + rendered);
    }
}
