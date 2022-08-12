package org.keycloak.broker.provider.util;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.mockito.Mockito;

public class IdentityBrokerStateTest {


    @Test
    public void testDecodedWithClientIdNotUuid() {

        // Given
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String clientId = "something not a uuid";
        String clientClientId = "http://i.am.an.url";
        String tabId = "vpISZLVDAc0";

        // When
        IdentityBrokerState encodedState = IdentityBrokerState.decoded(state, clientId, clientClientId, tabId);

        // Then
        Assert.assertNotNull(encodedState);
        Assert.assertEquals(clientClientId, encodedState.getClientId());
        Assert.assertEquals(tabId, encodedState.getTabId());
        Assert.assertEquals("gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.http://i.am.an.url", encodedState.getEncoded());
    }

    @Test
    public void testDecodedWithClientIdAnActualUuid() {

        // Given
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String clientId = "ed49448c-14cf-471e-a83a-063d0dc3bc8c";
        String clientClientId = "http://i.am.an.url";
        String tabId = "vpISZLVDAc0";

        // When
        IdentityBrokerState encodedState = IdentityBrokerState.decoded(state, clientId, clientClientId, tabId);

        // Then
        Assert.assertNotNull(encodedState);
        Assert.assertEquals(clientClientId, encodedState.getClientId());
        Assert.assertEquals(tabId, encodedState.getTabId());
        Assert.assertEquals("gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.7UlEjBTPRx6oOgY9DcO8jA", encodedState.getEncoded());
    }

    @Test
    @Ignore
    public void testEncodedWithClientIdUUid() {
        // Given
        String encoded = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.7UlEjBTPRx6oOgY9DcO8jA";
        ClientModel clientModel = Mockito.mock(ClientModel.class);
        String myClientId = "my-client-id";
        Mockito.when(clientModel.getClientId()).thenReturn(myClientId);
        RealmModel realmModel = Mockito.mock(RealmModel.class);
        Mockito.when(realmModel.getClientById(Mockito.eq("ed49448c-14cf-471e-a83a-063d0dc3bc8c"))).thenReturn(clientModel);

        // When
        IdentityBrokerState decodedState = IdentityBrokerState.encoded(encoded, realmModel);

        // Then
        Assert.assertNotNull(decodedState);
        Assert.assertEquals(myClientId, decodedState.getClientId());
    }

    @Test
    @Ignore
    public void testEncodedWithClientIdNotUUid() {
        // Given
        String encoded = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.http://i.am.an.url";
        RealmModel realmModel = Mockito.mock(RealmModel.class);

        // When
        IdentityBrokerState decodedState = IdentityBrokerState.encoded(encoded, realmModel);

        // Then
        Assert.assertNotNull(decodedState);
        Assert.assertEquals("http://i.am.an.url", decodedState.getClientId());
//        Mockito.verify(realmModel).getClientById(tim)
    }

}
