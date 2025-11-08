package org.keycloak.broker.provider.util;

import java.io.IOException;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientData;

import org.junit.Assert;
import org.junit.Test;


public class IdentityBrokerStateTest {

    @Test
    public void testDecodedWithClientIdNotUuid() {

        // Given
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String clientId = "something not a uuid";
        String clientClientId = "http://i.am.an.url";
        String tabId = "vpISZLVDAc0";

        // When
        IdentityBrokerState encodedState = IdentityBrokerState.decoded(state, clientId, clientClientId, tabId, null);

        // Then
        Assert.assertNotNull(encodedState);
        Assert.assertEquals(clientClientId, encodedState.getClientId());
        Assert.assertEquals(tabId, encodedState.getTabId());
        Assert.assertEquals("gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.aHR0cDovL2kuYW0uYW4udXJs", encodedState.getEncoded());
    }

    @Test
    public void testDecodedWithClientIdAnActualUuid() {

        // Given
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String clientId = "ed49448c-14cf-471e-a83a-063d0dc3bc8c";
        String clientClientId = "http://i.am.an.url";
        String tabId = "vpISZLVDAc0";

        // When
        IdentityBrokerState encodedState = IdentityBrokerState.decoded(state, clientId, clientClientId, tabId, null);

        // Then
        Assert.assertNotNull(encodedState);
        Assert.assertEquals(clientClientId, encodedState.getClientId());
        Assert.assertEquals(tabId, encodedState.getTabId());
        Assert.assertEquals("gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.7UlEjBTPRx6oOgY9DcO8jA", encodedState.getEncoded());
    }

    @Test
    public void testDecodedWithClientIdAnActualUuidBASE64UriFriendly() throws IOException {

        // Given
        String state = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk";
        String clientId = "c5ac1ea7-6c28-4be1-b7cd-d63a1ba57f78";
        String clientClientId = "http://i.am.an.url";
        String tabId = "vpISZLVDAc0";
        String clientDataParam = new ClientData("https://my-redirect-uri", "code", "query", "some-state").encode();

        // When
        IdentityBrokerState encodedState = IdentityBrokerState.decoded(state, clientId, clientClientId, tabId, clientDataParam);

        // Then
        Assert.assertNotNull(encodedState);
        Assert.assertEquals(clientClientId, encodedState.getClientId());
        Assert.assertEquals(tabId, encodedState.getTabId());
        ClientData clientData = ClientData.decodeClientDataFromParameter(encodedState.getClientData());
        Assert.assertEquals("https://my-redirect-uri", clientData.getRedirectUri());
        Assert.assertEquals("code", clientData.getResponseType());
        Assert.assertEquals("query", clientData.getResponseMode());
        Assert.assertEquals("some-state", clientData.getState());
        Assert.assertEquals("gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.xawep2woS-G3zdY6G6V_eA.eyJydSI6Imh0dHBzOi8vbXktcmVkaXJlY3QtdXJpIiwicnQiOiJjb2RlIiwicm0iOiJxdWVyeSIsInN0Ijoic29tZS1zdGF0ZSJ9", encodedState.getEncoded());
    }

    @Test
    public void testEncodedWithClientIdUUid() {
        // Given
        String encoded = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.7UlEjBTPRx6oOgY9DcO8jA";
        String clientId = "ed49448c-14cf-471e-a83a-063d0dc3bc8c";
        String clientClientId = "my-client-id";
        ClientModel clientModel = new IdentityBrokerStateTestHelpers.TestClientModel(clientId, clientClientId);
        RealmModel realmModel = new IdentityBrokerStateTestHelpers.TestRealmModel(clientId, clientClientId, clientModel);

        // When
        IdentityBrokerState decodedState = IdentityBrokerState.encoded(encoded, realmModel);

        // Then
        Assert.assertNotNull(decodedState);
        Assert.assertEquals(clientClientId, decodedState.getClientId());
    }

    @Test
    public void testEncodedWithClientIdNotUUid() throws IOException {
        // Given
        String encoded = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.aHR0cDovL2kuYW0uYW4udXJs";
        String clientId = "http://i.am.an.url";
        ClientModel clientModel = new IdentityBrokerStateTestHelpers.TestClientModel(clientId, clientId);
        RealmModel realmModel = new IdentityBrokerStateTestHelpers.TestRealmModel(clientId, clientId, clientModel);

        // When
        IdentityBrokerState decodedState = IdentityBrokerState.encoded(encoded, realmModel);

        // Then
        Assert.assertNotNull(decodedState);
        Assert.assertEquals("http://i.am.an.url", decodedState.getClientId());
        Assert.assertNull(ClientData.decodeClientDataFromParameter(decodedState.getClientData()));
    }

    @Test
    public void testEncodedWithClientData() throws IOException {
        // Given
        String encoded = "gNrGamIDGKpKSI9yOrcFzYTKoFGH779_WNCacAelkhk.vpISZLVDAc0.aHR0cDovL2kuYW0uYW4udXJs.eyJydSI6Imh0dHBzOi8vbXktcmVkaXJlY3QtdXJpIiwicnQiOiJjb2RlIiwicm0iOiJxdWVyeSIsInN0Ijoic29tZS1zdGF0ZSJ9";
        String clientId = "http://i.am.an.url";
        ClientModel clientModel = new IdentityBrokerStateTestHelpers.TestClientModel(clientId, clientId);
        RealmModel realmModel = new IdentityBrokerStateTestHelpers.TestRealmModel(clientId, clientId, clientModel);

        // When
        IdentityBrokerState decodedState = IdentityBrokerState.encoded(encoded, realmModel);

        // Then
        Assert.assertNotNull(decodedState);
        Assert.assertEquals("http://i.am.an.url", decodedState.getClientId());
        ClientData clientData = ClientData.decodeClientDataFromParameter(decodedState.getClientData());
        Assert.assertNotNull(clientData);
        Assert.assertEquals("https://my-redirect-uri", clientData.getRedirectUri());
        Assert.assertEquals("code", clientData.getResponseType());
        Assert.assertEquals("query", clientData.getResponseMode());
        Assert.assertEquals("some-state", clientData.getState());
    }

}
