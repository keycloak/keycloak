package org.keycloak.ssf.transmitter.stream.storage.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.vault.VaultStringSecret;
import org.keycloak.vault.VaultTranscriber;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the vault placeholder round-trip behavior added to
 * {@link ClientStreamStore}: a receiver that GETs and re-PUTs its
 * stream configuration must not silently undo an admin's
 * externalization of {@code authorization_header} into
 * {@code ${vault.x}}.
 */
class ClientStreamStoreVaultRoundTripTest {

    private static final String STREAM_ID = "stream-1";
    private static final String VAULT_PLACEHOLDER = "${vault.my_token}";
    private static final String RESOLVED_SECRET = "Bearer abc123";

    @Test
    void read_resolvesVaultPlaceholder() {
        Map<String, String> attrs = seededAttrs(VAULT_PLACEHOLDER);
        ClientStreamStore store = newStore(vaultThatResolves(VAULT_PLACEHOLDER, RESOLVED_SECRET));

        StreamConfig loaded = store.extractStreamConfig(fakeClient(attrs));

        assertEquals(RESOLVED_SECRET, loaded.getDelivery().getAuthorizationHeader());
    }

    @Test
    void read_passesThroughLiteralWhenNotAVaultExpression() {
        Map<String, String> attrs = seededAttrs("Bearer literal-token");
        ClientStreamStore store = newStore(vaultReturnsEmpty());

        StreamConfig loaded = store.extractStreamConfig(fakeClient(attrs));

        assertEquals("Bearer literal-token", loaded.getDelivery().getAuthorizationHeader());
    }

    @Test
    void roundTrip_preservesPlaceholder_whenReceiverEchoesResolvedValueUnchanged() {
        Map<String, String> attrs = seededAttrs(VAULT_PLACEHOLDER);
        ClientModel client = fakeClient(attrs);
        ClientStreamStore store = newStore(vaultThatResolves(VAULT_PLACEHOLDER, RESOLVED_SECRET));

        // Receiver loads the config (sees the resolved secret), then PATCHes
        // it back unchanged — the typical no-op round-trip from a receiver
        // that has no knowledge of vault placeholders.
        StreamConfig loaded = store.extractStreamConfig(client);
        store.storeStreamConfig(client, loaded);

        assertEquals(VAULT_PLACEHOLDER,
                attrs.get(ClientStreamStore.SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY),
                "vault placeholder must survive a no-op round-trip from the receiver");
    }

    @Test
    void roundTrip_overwritesPlaceholder_whenReceiverChangesValue() {
        Map<String, String> attrs = seededAttrs(VAULT_PLACEHOLDER);
        ClientModel client = fakeClient(attrs);
        ClientStreamStore store = newStore(vaultThatResolves(VAULT_PLACEHOLDER, RESOLVED_SECRET));

        StreamConfig loaded = store.extractStreamConfig(client);
        loaded.getDelivery().setAuthorizationHeader("Bearer rotated");
        store.storeStreamConfig(client, loaded);

        assertEquals("Bearer rotated",
                attrs.get(ClientStreamStore.SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY),
                "an actual change to the secret must replace the placeholder");
    }

    private static Map<String, String> seededAttrs(String authorizationHeader) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put(ClientStreamStore.SSF_STREAM_ID_KEY, STREAM_ID);
        attrs.put(ClientStreamStore.SSF_STATUS_KEY, "enabled");
        attrs.put(ClientStreamStore.SSF_STREAM_DELIVERY_METHOD_KEY,
                "urn:ietf:rfc:8935:delivery:method:push");
        attrs.put(ClientStreamStore.SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY,
                "https://receiver.example/events");
        attrs.put(ClientStreamStore.SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY,
                authorizationHeader);
        return attrs;
    }

    private static ClientModel fakeClient(Map<String, String> attrs) {
        ClientModel client = mock(ClientModel.class);
        when(client.getId()).thenReturn("kc-internal-id");
        when(client.getClientId()).thenReturn("receiver-client");
        when(client.getAttribute(any(String.class)))
                .thenAnswer(inv -> attrs.get(inv.<String>getArgument(0)));
        doAnswer(inv -> {
            attrs.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(client).setAttribute(any(String.class), any(String.class));
        doAnswer(inv -> {
            attrs.remove(inv.<String>getArgument(0));
            return null;
        }).when(client).removeAttribute(any(String.class));
        return client;
    }

    private static ClientStreamStore newStore(VaultTranscriber vault) {
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.vault()).thenReturn(vault);
        return new ClientStreamStore(session);
    }

    private static VaultTranscriber vaultThatResolves(String placeholder, String resolved) {
        // Pre-build the VaultStringSecret mocks before stubbing — Mockito
        // disallows mock construction inside another stub's answer lambda.
        VaultStringSecret resolvedSecret = secretOf(resolved);
        VaultStringSecret emptySecret = emptySecret();

        VaultTranscriber vault = mock(VaultTranscriber.class);
        when(vault.getStringSecret(any(String.class))).thenReturn(emptySecret);
        when(vault.getStringSecret(eq(placeholder))).thenReturn(resolvedSecret);
        return vault;
    }

    private static VaultTranscriber vaultReturnsEmpty() {
        VaultStringSecret emptySecret = emptySecret();
        VaultTranscriber vault = mock(VaultTranscriber.class);
        when(vault.getStringSecret(any(String.class))).thenReturn(emptySecret);
        return vault;
    }

    private static VaultStringSecret secretOf(String value) {
        VaultStringSecret secret = mock(VaultStringSecret.class);
        when(secret.get()).thenReturn(Optional.of(value));
        return secret;
    }

    private static VaultStringSecret emptySecret() {
        VaultStringSecret secret = mock(VaultStringSecret.class);
        when(secret.get()).thenReturn(Optional.empty());
        return secret;
    }
}
