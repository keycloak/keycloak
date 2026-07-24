/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmProvider extends Provider {

    /**
     * Creates new realm with the given name. The internal ID will be generated automatically.
     * @param name String name of the realm
     * @return Model of the created realm.
     */
    RealmModel createRealm(String name);

    /**
     * Created new realm with given ID and name.
     * @param id Internal ID of the realm or {@code null} if one is to be created by the underlying store. If the store
     *           expects the ID to have a certain format (for example {@code UUID}) and the supplied ID doesn't follow
     *           the expected format, the store may replace the {@code id} with a new one at its own discretion.
     * @param name String name of the realm
     * @return Model of the created realm.
     */
    RealmModel createRealm(String id, String name);

    /**
     * Exact search for a realm by its internal ID.
     * @param id Internal ID of the realm.
     * @return Model of the realm
     */
    RealmModel getRealm(String id);

    /**
     * Exact search for a realm by its name.
     * @param name String name of the realm
     * @return Model of the realm
     */
    RealmModel getRealmByName(String name);

    /**
     * Returns realms as a stream.
     * @return Stream of {@link RealmModel}. Never returns {@code null}.
     */
    Stream<RealmModel> getRealmsStream();

    /**
     * Returns realms as a stream filtered by search.
     * @param search String to search for in realm names
     * @return Stream of {@link RealmModel}. Never returns {@code null}.
     */
    default Stream<RealmModel> getRealmsStream(String search) {
        return getRealmsStream().filter(realm -> search.isEmpty() || realm.getName().toLowerCase().contains(search.trim().toLowerCase()));
    }

    /**
     * Returns stream of realms which has component with the given provider type.
     * @param type {@code Class<?>} Type of the provider.
     * @return Stream of {@link RealmModel}. Never returns {@code null}.
     */
    Stream<RealmModel> getRealmsWithProviderTypeStream(Class<?> type);

    /**
     * Removes realm with the given id.
     * @param id of realm.
     * @return {@code true} if the realm was successfully removed.
     */
    boolean removeRealm(String id);

    default ClientInitialAccessModel createClientInitialAccessModel(RealmModel realm, int expiration, int count) {
        return realm.createClientInitialAccessModel(expiration, count);
    }
    default ClientInitialAccessModel getClientInitialAccessModel(RealmModel realm, String id) {
        return realm.getClientInitialAccessModel(id);
    }
    default void removeClientInitialAccessModel(RealmModel realm, String id) {
        realm.removeClientInitialAccessModel(id);
    }

    /**
     * Returns client's initial access as a stream.
     * @param realm {@link RealmModel} The realm where to list client's initial access.
     * @return Stream of {@link ClientInitialAccessModel}. Never returns {@code null}.
     */
    default Stream<ClientInitialAccessModel> listClientInitialAccessStream(RealmModel realm) {
        return realm.getClientInitialAccesses();
    }

    /**
     * Removes all expired client initial accesses from all realms.
     */
    void removeExpiredClientInitialAccess();

    default void decreaseRemainingCount(RealmModel realm, ClientInitialAccessModel clientInitialAccess) { // Separate provider method to ensure we decrease remainingCount atomically instead of doing classic update
        realm.decreaseRemainingCount(clientInitialAccess);
    }

    void saveLocalizationText(RealmModel realm, String locale, String key, String text);

    void saveLocalizationTexts(RealmModel realm, String locale, Map<String, String> localizationTexts);

    boolean updateLocalizationText(RealmModel realm, String locale, String key, String text);

    boolean deleteLocalizationTextsByLocale(RealmModel realm, String locale);

    boolean deleteLocalizationText(RealmModel realm, String locale, String key);

    String getLocalizationTextsById(RealmModel realm, String locale, String key);
}
