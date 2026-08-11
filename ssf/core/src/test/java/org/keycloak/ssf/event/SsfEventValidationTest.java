package org.keycloak.ssf.event;

import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link SsfEvent#validate()} hook and the
 * required-field overrides on the bundled CAEP events.
 *
 * <p>Covers:
 * <ul>
 *     <li>The default no-op behaviour (events without spec-required
 *         fields, e.g. {@code CaepSessionRevoked}, never throw).</li>
 *     <li>{@link CaepCredentialChange} rejects missing
 *         {@code credential_type} and {@code change_type}.</li>
 *     <li>The exception carries the stable
 *         {@link SsfEventValidationException#MESSAGE_KEY}, the event
 *         alias, and the wire-name of the offending field so callers
 *         (REST emit response, admin UI) can compose a localised
 *         message from the structured pieces.</li>
 * </ul>
 */
class SsfEventValidationTest {

    @Test
    void defaultValidateIsNoOp_caepSessionRevokedNeverThrows() {
        CaepSessionRevoked event = new CaepSessionRevoked();
        // SessionRevoked has no required fields; the subject identifies
        // the session, so validate() must not reject the empty body.
        assertDoesNotThrow(event::validate);
    }

    @Test
    void caepCredentialChange_missingCredentialType_throws() {
        CaepCredentialChange event = new CaepCredentialChange();
        // Only change_type set; credential_type is REQUIRED per CAEP §3.3.1.
        event.setChangeType(CaepCredentialChange.ChangeType.UPDATE);

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals(SsfEventValidationException.MESSAGE_KEY, ex.getMessageKey(),
                "exception should carry the stable i18n key");
        assertEquals("CaepCredentialChange", ex.getEventAlias(),
                "alias should match SsfEvent.getAlias() so callers can localise per-event");
        assertEquals("credential_type", ex.getField(),
                "field should be the wire (@JsonProperty) name, not the Java field name, "
                        + "so the operator sees the same identifier they used in the JSON body");
    }

    @Test
    void caepCredentialChange_missingChangeType_throws() {
        CaepCredentialChange event = new CaepCredentialChange();
        event.setCredentialType("password");
        // changeType deliberately not set

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("change_type", ex.getField());
    }

    @Test
    void caepCredentialChange_blankCredentialType_throws() {
        // Defensive: an empty string would slip past a plain null-check
        // but a CAEP receiver expects a meaningful identifier.
        CaepCredentialChange event = new CaepCredentialChange();
        event.setCredentialType("   ");
        event.setChangeType(CaepCredentialChange.ChangeType.UPDATE);

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("credential_type", ex.getField());
    }

    @Test
    void caepCredentialChange_bothFieldsPresent_doesNotThrow() {
        CaepCredentialChange event = new CaepCredentialChange();
        event.setCredentialType("password");
        event.setChangeType(CaepCredentialChange.ChangeType.UPDATE);

        assertDoesNotThrow(event::validate);
    }

    // ---- exception structure --------------------------------------------

    @Test
    void exceptionMessageMatchesStableComposition() {
        // getMessage() must remain a mechanical "<key>: <alias>.<field>"
        // string — log lines and non-localised callers depend on it
        // staying stable. The wire status carries the same key, so
        // grepping for "invalid_event_data" turns up both the exception
        // log and the REST response.
        SsfEventValidationException ex = new SsfEventValidationException("CaepCredentialChange", "change_type");
        assertEquals("invalid_event_data: CaepCredentialChange.change_type", ex.getMessage());
    }
}
