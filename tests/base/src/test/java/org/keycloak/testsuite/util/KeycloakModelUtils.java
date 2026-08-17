package org.keycloak.testsuite.util;

import java.util.Arrays;
import java.util.List;

import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.models.utils.KeycloakModelUtils.getDefaultClientAuthenticatorType;

public final class KeycloakModelUtils {

    private KeycloakModelUtils() {
    }

    public static ClientRepresentation createClient(RealmRepresentation realm, String name) {
        ClientRepresentation app = new ClientRepresentation();
        app.setName(name);
        app.setClientId(name);
        List<ClientRepresentation> clients = realm.getClients();
        if (clients != null) {
            clients.add(app);
        } else {
            realm.setClients(Arrays.asList(app));
        }
        app.setClientAuthenticatorType(getDefaultClientAuthenticatorType());
        generateSecret(app);
        app.setFullScopeAllowed(true);
        return app;
    }

    public static CredentialRepresentation generateSecret(ClientRepresentation client) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        client.setSecret(secret.getChallengeResponse());
        return ModelToRepresentation.toRepresentation(secret);
    }
}
