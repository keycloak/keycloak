package org.keycloak.tests.providers.federation;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;

// Supplies external users while leaving their password storage and validation to Keycloak.
public class NoCredentialUserStorageProviderFactory implements UserStorageProviderFactory<NoCredentialUserStorageProviderFactory.Storage> {

    public static final String ID = "test-no-credential-user-storage";
    public static final String USERNAME_PREFIX = "no-credential-";

    @Override
    public Storage create(KeycloakSession session, ComponentModel model) {
        return new Storage(session, model);
    }

    @Override
    public String getId() {
        return ID;
    }

    public static class Storage implements UserStorageProvider, UserLookupProvider {

        private final KeycloakSession session;
        private final ComponentModel model;

        public Storage(KeycloakSession session, ComponentModel model) {
            this.session = session;
            this.model = model;
        }

        @Override
        public UserModel getUserById(RealmModel realm, String id) {
            return getUserByUsername(realm, StorageId.externalId(id));
        }

        @Override
        public UserModel getUserByUsername(RealmModel realm, String username) {
            if (username == null || !username.startsWith(USERNAME_PREFIX)) {
                return null;
            }
            return new AbstractUserAdapterFederatedStorage.Streams(session, realm, model) {
                @Override
                public String getUsername() {
                    return username;
                }

                @Override
                public void setUsername(String username) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public UserModel getUserByEmail(RealmModel realm, String email) {
            return null;
        }

        @Override
        public void close() {
        }
    }
}
