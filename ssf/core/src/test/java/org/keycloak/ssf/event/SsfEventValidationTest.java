package org.keycloak.ssf.event;

import java.util.Map;

import org.keycloak.ssf.event.caep.CaepAssuranceLevelChange;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepDeviceComplianceChange;
import org.keycloak.ssf.event.caep.CaepRiskLevelChanged;
import org.keycloak.ssf.event.caep.CaepSessionEstablished;
import org.keycloak.ssf.event.caep.CaepSessionPresented;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.event.caep.CaepTokenClaimsChanged;
import org.keycloak.ssf.event.risc.RiscAccountDisabled;
import org.keycloak.ssf.event.risc.RiscCredentialCompromise;

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
 *     <li>{@link CaepDeviceComplianceChange} rejects missing
 *         {@code current_status}, accepts missing
 *         {@code previous_status}.</li>
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

    @Test
    void caepDeviceComplianceChange_missingCurrentStatus_throws() {
        CaepDeviceComplianceChange event = new CaepDeviceComplianceChange();
        // previous_status set, current_status (REQUIRED) deliberately not.
        event.setPreviousStatus(CaepDeviceComplianceChange.ComplianceChange.COMPLIANT);

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("current_status", ex.getField());
        assertEquals("CaepDeviceComplianceChange", ex.getEventAlias());
    }

    @Test
    void caepDeviceComplianceChange_currentStatusOnly_doesThrow() {
        // previous_status is REQUIRED
        CaepDeviceComplianceChange event = new CaepDeviceComplianceChange();
        event.setCurrentStatus(CaepDeviceComplianceChange.ComplianceChange.NOT_COMPLIANT);

        assertThrows(SsfEventValidationException.class, event::validate);
    }

    // ---- additional CAEP / RISC event coverage --------------------------

    @Test
    void caepAssuranceLevelChange_missingNamespace_throws() {
        // CAEP §3.3.4: namespace + current_level are REQUIRED.
        // previous_level is optional — receiver MUST treat omission as
        // "previous unknown".
        CaepAssuranceLevelChange event = new CaepAssuranceLevelChange();
        event.setCurrentLevel("nist-aal2");

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("namespace", ex.getField());
    }

    @Test
    void caepAssuranceLevelChange_missingCurrentLevel_throws() {
        CaepAssuranceLevelChange event = new CaepAssuranceLevelChange();
        event.setNamespace("https://www.nist.gov/identity/aal");

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("current_level", ex.getField());
    }

    @Test
    void caepAssuranceLevelChange_namespaceAndCurrentLevel_doesNotThrow() {
        // previous_level + change_direction deliberately omitted —
        // both are spec-optional.
        CaepAssuranceLevelChange event = new CaepAssuranceLevelChange();
        event.setNamespace("https://www.nist.gov/identity/aal");
        event.setCurrentLevel("nist-aal2");

        assertDoesNotThrow(event::validate);
    }

    @Test
    void caepRiskLevelChanged_missingPrincipal_throws() {
        // CAEP §3.8: principal + current_level are REQUIRED.
        CaepRiskLevelChanged event = new CaepRiskLevelChanged();
        event.setCurrentLevel("HIGH");

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("principal", ex.getField());
    }

    @Test
    void caepRiskLevelChanged_missingCurrentLevel_throws() {
        CaepRiskLevelChanged event = new CaepRiskLevelChanged();
        event.setPrincipal("USER");

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("current_level", ex.getField());
    }

    @Test
    void caepTokenClaimsChanged_missingClaims_throws() {
        // CAEP §3.3.2: claims is REQUIRED. An empty map carries no
        // signal so we treat it the same as null — a receiver couldn't
        // act on "some claims changed" with no claim names.
        CaepTokenClaimsChanged event = new CaepTokenClaimsChanged();

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("claims", ex.getField());
    }

    @Test
    void caepTokenClaimsChanged_emptyClaims_throws() {
        CaepTokenClaimsChanged event = new CaepTokenClaimsChanged();
        event.setClaims(Map.of());

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("claims", ex.getField());
    }

    @Test
    void caepTokenClaimsChanged_populatedClaims_doesNotThrow() {
        CaepTokenClaimsChanged event = new CaepTokenClaimsChanged();
        event.setClaims(Map.of("trust_level", "high"));

        assertDoesNotThrow(event::validate);
    }

    @Test
    void caepSessionEstablished_emptyBody_doesNotThrow() {
        // CAEP §3.4: every body field is OPTIONAL — the subject
        // identifies the session being established. Default no-op
        // validate() is the right behaviour.
        assertDoesNotThrow(new CaepSessionEstablished()::validate);
    }

    @Test
    void caepSessionPresented_emptyBody_doesNotThrow() {
        // CAEP §3.5: same shape as session-established — every field
        // optional, validation is a no-op.
        assertDoesNotThrow(new CaepSessionPresented()::validate);
    }

    @Test
    void riscCredentialCompromise_missingCredentialType_throws() {
        // RISC §2.7: credential_type is REQUIRED.
        RiscCredentialCompromise event = new RiscCredentialCompromise();

        SsfEventValidationException ex = assertThrows(SsfEventValidationException.class, event::validate);
        assertEquals("credential_type", ex.getField());
        assertEquals("RiscCredentialCompromise", ex.getEventAlias());
    }

    @Test
    void riscCredentialCompromise_credentialTypeSet_doesNotThrow() {
        RiscCredentialCompromise event = new RiscCredentialCompromise();
        event.setCredentialType("password");

        assertDoesNotThrow(event::validate);
    }

    @Test
    void riscAccountDisabled_emptyBody_doesNotThrow() {
        // RISC §2.3: reason is OPTIONAL — account-disabled carries
        // its signal in the event type itself plus the subject.
        assertDoesNotThrow(new RiscAccountDisabled()::validate);
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
