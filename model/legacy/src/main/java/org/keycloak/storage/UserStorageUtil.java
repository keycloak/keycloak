package org.keycloak.storage;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.stream.Stream;

/**
 * @author Alexander Schwartz
 */
public class UserStorageUtil {
    /**
     * Returns sorted {@link UserStorageProviderModel UserStorageProviderModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream of {@link UserStorageProviderModel}. Never returns {@code null}.
     */
    public static Stream<UserStorageProviderModel> getUserStorageProvidersStream(RealmModel realm) {
        return realm.getComponentsStream(realm.getId(), UserStorageProvider.class.getName())
                .map(UserStorageProviderModel::new)
                .sorted(UserStorageProviderModel.comparator);
    }

    public static UserFederatedStorageProvider userFederatedStorage(KeycloakSession session) {
        return session.getProvider(UserFederatedStorageProvider.class);
    }

    public static UserCache userCache(KeycloakSession session) {
        return session.getProvider(UserCache.class);
    }

}
