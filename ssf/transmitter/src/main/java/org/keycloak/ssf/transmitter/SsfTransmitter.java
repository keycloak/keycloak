package org.keycloak.ssf.transmitter;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;

public final class SsfTransmitter {

    private SsfTransmitter() {
    }

    /**
     * Resolves the {@link SsfTransmitterProvider} for the given session.
     * Single entry point for the lookup so we have one place to add
     * caching, logging, or test instrumentation around it later.
     *
     * @param session the active Keycloak session; must not be {@code null}.
     * @return the SSF transmitter provider bound to {@code session}.
     */
    public static SsfTransmitterProvider of(KeycloakSession session) {
        return session.getProvider(SsfTransmitterProvider.class);
    }

    /**
     * Returns {@code true} when the client carries the
     * {@code ssf.enabled=true} attribute that marks it as an SSF Receiver.
     * Returns {@code false} for {@code null} clients, regular OIDC apps,
     * service accounts, or any client whose attribute is unset / explicitly
     * {@code false}.
     */
    public static boolean isReceiverClient(ClientModel client) {
        if (client == null) {
            return false;
        }
        return Boolean.parseBoolean(client.getAttribute(ClientStreamStore.SSF_ENABLED_KEY));
    }

    /**
     * Looks up an SSF Receiver client by its OAuth {@code client_id} in
     * the session's current realm and returns it. Throws
     * {@link SsfException} when the client doesn't exist or isn't
     * configured as an SSF Receiver — the typical wrong-client failure
     * for programmatic emit callers, where returning {@code null} would
     * surface later as a confusing {@code STREAM_NOT_FOUND}.
     *
     * @param session         the active Keycloak session
     * @param clientClientId  OAuth {@code client_id} (not the internal UUID)
     * @return the resolved receiver client, never {@code null}
     */
    public static ClientModel getReceiverClient(KeycloakSession session, String clientClientId) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = realm.getClientByClientId(clientClientId);
        if (client == null) {
            throw new SsfException("No client with clientId '" + clientClientId + "' in realm '"
                    + realm.getName() + "'");
        }
        if (!isReceiverClient(client)) {
            throw new SsfException("Client '" + clientClientId
                    + "' is not an SSF Receiver — set the ssf.enabled=true client attribute first");
        }
        return client;
    }

}
