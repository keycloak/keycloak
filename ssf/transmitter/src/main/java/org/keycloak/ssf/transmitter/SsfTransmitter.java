package org.keycloak.ssf.transmitter;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.transmitter.support.SsfUtil;

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
        // Distinguish "disabled" from "not a receiver" so the emit caller
        // gets an actionable message instead of being told to set an
        // attribute that may already be present (see keycloak/keycloak#50050).
        if (!client.isEnabled()) {
            throw new SsfException("Client '" + clientClientId
                    + "' is disabled — enable the client to resume SSF event delivery");
        }
        if (!SsfUtil.isReceiverClient(client)) {
            throw new SsfException("Client '" + clientClientId
                    + "' is not an SSF Receiver — set the ssf.enabled=true client attribute first");
        }
        return client;
    }

}
