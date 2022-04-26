package org.keycloak.storage;

import org.keycloak.models.RealmModel;

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
}
