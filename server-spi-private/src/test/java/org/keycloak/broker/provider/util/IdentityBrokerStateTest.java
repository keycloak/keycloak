package org.keycloak.broker.provider.util;

import org.junit.Test;
import org.keycloak.authorization.identity.Identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IdentityBrokerStateTest {
    private static final int RELAY_STATE_MAX_LENGTH = 80;

    @Test
    public void decoded_withinBounds_preservesClientId() {
        // Arrange
        // The length of the concatenated strings is below 80 characters, the RelayState limit as per the SAML specification.
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String tabId = "vpISZLVDAc0";
        String clientId = "foo";

        // Act
        IdentityBrokerState ibs = IdentityBrokerState.decoded(state, clientId, tabId);

        // Assert
        assertEquals(clientId, ibs.getClientId());
        assertTrue(ibs.getEncoded().length() < RELAY_STATE_MAX_LENGTH);
    }

    @Test
    public void decoded_outOfBounds_compressesClientId() {
        // Arrange
        // The length of the concatenated strings is above 80 characters, the RelayState limit as per the SAML specification.
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String tabId = "vpISZLVDAc0";
        String clientId = "https://login.blablabla.com/auth/realms/broker";

        // Act
        IdentityBrokerState ibs = IdentityBrokerState.decoded(state, clientId, tabId);

        // Assert
        assertTrue(ibs.getClientId().length() < clientId.length());
        assertTrue(ibs.getEncoded().length() < RELAY_STATE_MAX_LENGTH);
    }

    @Test
    public void encoded_compressedClientId_decompressesClientId() {
        // Arrange
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String tabId = "vpISZLVDAc0";
        String clientId = "https://login.blablabla.com/auth/realms/broker";
        String encodedState = IdentityBrokerState.decoded(state, clientId, tabId).getEncoded();

        // Act
        IdentityBrokerState ibs = IdentityBrokerState.encoded(encodedState);

        // Assert
        assertEquals(clientId, ibs.getClientId());
    }

    @Test
    public void encoded_uncompressedClientId_preservesClientId() {
        // Arrange
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String tabId = "vpISZLVDAc0";
        String clientId = "foo";
        String encodedState = IdentityBrokerState.decoded(state, clientId, tabId).getEncoded();

        // Act
        IdentityBrokerState ibs = IdentityBrokerState.encoded(encodedState);

        // Assert
        assertEquals(clientId, ibs.getClientId());
    }
}

