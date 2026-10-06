package org.keycloak.ssf.transmitter.support;

import org.keycloak.models.ClientModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the two receiver predicates in {@link SsfUtil}:
 *
 * <ul>
 *     <li>{@link SsfUtil#isReceiverClient} — configuration only: does the
 *         client carry {@code ssf.enabled=true}? Ignores the client on/off
 *         state.</li>
 *     <li>{@link SsfUtil#isReceiverEnabled} — live-delivery gate: configured
 *         receiver <em>and</em> an enabled Keycloak client.</li>
 * </ul>
 *
 * <p>The key behaviour under test (keycloak/keycloak#50050) is that a
 * configured-but-disabled receiver is still a receiver
 * ({@code isReceiverClient == true}) but is off the air for delivery
 * ({@code isReceiverEnabled == false}), so disabling the client stops SSF
 * event delivery without discarding the stream configuration.
 */
class SsfUtilReceiverTest {

    private static ClientModel client(Boolean ssfEnabledAttr, boolean clientEnabled) {
        ClientModel client = mock(ClientModel.class);
        when(client.getAttribute(SsfUtil.SSF_ENABLED_KEY))
                .thenReturn(ssfEnabledAttr == null ? null : ssfEnabledAttr.toString());
        when(client.isEnabled()).thenReturn(clientEnabled);
        return client;
    }

    // ----- isReceiverClient: configuration only -----------------------------

    @Test
    void isReceiverClient_nullClient_false() {
        assertFalse(SsfUtil.isReceiverClient(null), "a null client is never a receiver");
    }

    @Test
    void isReceiverClient_attributeUnset_false() {
        assertFalse(SsfUtil.isReceiverClient(client(null, true)),
                "absent ssf.enabled attribute means the client is not configured as a receiver");
    }

    @Test
    void isReceiverClient_attributeFalse_false() {
        assertFalse(SsfUtil.isReceiverClient(client(false, true)),
                "ssf.enabled=false means the client is not a receiver");
    }

    @Test
    void isReceiverClient_attributeTrue_true() {
        assertTrue(SsfUtil.isReceiverClient(client(true, true)),
                "ssf.enabled=true marks the client as a receiver");
    }

    @Test
    void isReceiverClient_attributeTrueButClientDisabled_stillTrue() {
        assertTrue(SsfUtil.isReceiverClient(client(true, false)),
                "isReceiverClient reflects configuration only — a disabled client is still a configured receiver");
    }

    // ----- isReceiverEnabled: configuration AND client on/off ---------------

    @Test
    void isReceiverEnabled_nullClient_false() {
        assertFalse(SsfUtil.isReceiverEnabled(null), "a null client is never a deliverable receiver");
    }

    @Test
    void isReceiverEnabled_configuredAndEnabled_true() {
        assertTrue(SsfUtil.isReceiverEnabled(client(true, true)),
                "a configured receiver on an enabled client is live for delivery");
    }

    @Test
    void isReceiverEnabled_configuredButDisabled_false() {
        assertFalse(SsfUtil.isReceiverEnabled(client(true, false)),
                "disabling the client takes a configured receiver off the air (keycloak/keycloak#50050)");
    }

    @Test
    void isReceiverEnabled_notConfiguredButEnabled_false() {
        assertFalse(SsfUtil.isReceiverEnabled(client(false, true)),
                "an enabled client that is not configured as a receiver is not deliverable");
    }

    @Test
    void isReceiverEnabled_notConfiguredAndDisabled_false() {
        assertFalse(SsfUtil.isReceiverEnabled(client(false, false)),
                "neither configured nor enabled is not deliverable");
    }
}
