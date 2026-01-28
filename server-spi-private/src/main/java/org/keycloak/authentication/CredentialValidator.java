package org.keycloak.authentication;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public interface CredentialValidator<T extends CredentialProvider> {
    T getCredentialProvider(KeycloakSession session);
    default List<CredentialModel> getCredentials(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.credentialManager().getStoredCredentialsByTypeStream(getCredentialProvider(session).getType())
                .collect(Collectors.toList());
    }
    default String getType(KeycloakSession session) {
        return getCredentialProvider(session).getType();
    }
}
