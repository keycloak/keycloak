package org.keycloak.broker.provider.util;

import org.junit.Test;
import org.keycloak.common.util.Base64Url;

import static org.junit.Assert.assertEquals;

public class IdentityBrokerStateTest {
    @Test
    public void encodingDecodingTest() {
        IdentityBrokerState originalBrokerState = IdentityBrokerState.decoded(
                Base64Url.encode("SomeState".getBytes()),
                "someClient",
                Base64Url.encode("SomeTabId".getBytes())
        );

        IdentityBrokerState decodedBrokerState = IdentityBrokerState.encoded(originalBrokerState.getEncoded());
        assertEquals(decodedBrokerState.getClientId(), originalBrokerState.getClientId());
        assertEquals(decodedBrokerState.getTabId(), originalBrokerState.getTabId());
        assertEquals(decodedBrokerState.getDecodedState(), originalBrokerState.getDecodedState());
        assertEquals(decodedBrokerState.getEncoded(), originalBrokerState.getEncoded());
    }
}