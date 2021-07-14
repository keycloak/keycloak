/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityImpl;
import org.keycloak.models.map.client.MapClientProviderFactory;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.StringKeyConvertor;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.InvalidationHandler.ObjectType;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;


/**
 *
 * @author hmlnarik
 */
@RequireProvider(value = ClientProvider.class, only = {MapClientProviderFactory.PROVIDER_ID})
@RequireProvider(RealmProvider.class)
@RequireProvider(MapStorageProvider.class)
public class MapStorageTest extends KeycloakModelTest {

    private static final Logger LOG = Logger.getLogger(MapStorageTest.class.getName());

    private String realmId;

    private String mapStorageProviderId;

    @Before
    public void initMapStorageProviderId() {
        MapStorageProviderFactory ms = (MapStorageProviderFactory) getFactory().getProviderFactory(MapStorageProvider.class);
        mapStorageProviderId = ms.getId();
        assertThat(mapStorageProviderId, Matchers.notNullValue());
    }

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("realm");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    @SuppressWarnings("unchecked")
    public <K, K1, K2> void testStorageSeparation() {
        String component1Id = createMapStorageComponent("component1", "keyType", "ulong");
        String component2Id = createMapStorageComponent("component2", "keyType", "string");

        Object[] ids = withRealm(realmId, (session, realm) -> {
            MapStorage<K, MapClientEntity<K>, ClientModel> storageMain = (MapStorage<K, MapClientEntity<K>, ClientModel>) session.getProvider(MapStorageProvider.class).getStorage(MapClientEntity.class, ClientModel.class);
            MapStorage<K1, MapClientEntity<K1>, ClientModel> storage1 = (MapStorage<K1, MapClientEntity<K1>, ClientModel>) session.getComponentProvider(MapStorageProvider.class, component1Id).getStorage(MapClientEntity.class, ClientModel.class);
            MapStorage<K2, MapClientEntity<K2>, ClientModel> storage2 = (MapStorage<K2, MapClientEntity<K2>, ClientModel>) session.getComponentProvider(MapStorageProvider.class, component2Id).getStorage(MapClientEntity.class, ClientModel.class);

            // Assert that the map storage can be used both as a standalone store and a component
            assertThat(storageMain, notNullValue());
            assertThat(storage1, notNullValue());
            assertThat(storage2, notNullValue());

            final StringKeyConvertor<K> kcMain = storageMain.getKeyConvertor();
            final StringKeyConvertor<K1> kc1 = storage1.getKeyConvertor();
            final StringKeyConvertor<K2> kc2 = storage2.getKeyConvertor();

            K  idMain = kcMain.yieldNewUniqueKey();
            K1 id1    = kc1.yieldNewUniqueKey();
            K2 id2    = kc2.yieldNewUniqueKey();

            assertThat(idMain, notNullValue());
            assertThat(id1, notNullValue());
            assertThat(id2, notNullValue());

            // Assert that the stores do not contain the to-be-created clients
            assertThat(storageMain.read(idMain), nullValue());
            assertThat(storage1.read(id1), nullValue());
            assertThat(storage2.read(id2), nullValue());

            assertClientDoesNotExist(storageMain, id1, kc1, kcMain);
            assertClientDoesNotExist(storageMain, id2, kc2, kcMain);
            assertClientDoesNotExist(storage1, idMain, kcMain, kc1);
            assertClientDoesNotExist(storage1, id2, kc2, kc1);
            assertClientDoesNotExist(storage2, idMain, kcMain, kc2);
            assertClientDoesNotExist(storage2, id1, kc1, kc2);

            MapClientEntity<K> clientMain = new MapClientEntityImpl<>(idMain, realmId);
            MapClientEntity<K1> client1 = new MapClientEntityImpl<>(id1, realmId);
            MapClientEntity<K2> client2 = new MapClientEntityImpl<>(id2, realmId);

            storageMain.create(clientMain);
            storage1.create(client1);
            storage2.create(client2);

            return new Object[] {idMain, id1, id2};
        });

        K idMain = (K) ids[0];
        K1 id1 = (K1) ids[1];
        K2 id2 = (K2) ids[2];

        LOG.debugf("Object IDs: %s, %s, %s", idMain, id1, id2);

        assertClientsPersisted(component1Id, component2Id, idMain, id1, id2);

        // Invalidate one component and check that the storage still contains what it should
        getFactory().invalidate(ObjectType.COMPONENT, component1Id);
        assertClientsPersisted(component1Id, component2Id, idMain, id1, id2);

        // Invalidate whole realm and check that the storage still contains what it should
        getFactory().invalidate(ObjectType.REALM, realmId);
        assertClientsPersisted(component1Id, component2Id, idMain, id1, id2);

        // Refresh factory (akin server restart) and check that the storage still contains what it should
        reinitializeKeycloakSessionFactory();
        assertClientsPersisted(component1Id, component2Id, idMain, id1, id2);
    }

    private <K,K1> void assertClientDoesNotExist(MapStorage<K, MapClientEntity<K>, ClientModel> storage, K1 id, final StringKeyConvertor<K1> kc, final StringKeyConvertor<K> kcStorage) {
        // Assert that the other stores do not contain the to-be-created clients (if they use compatible key format)
        try {
            final K keyInStorageFormat = kcStorage.fromString(kc.keyToString(id));
            assertThat(storage.read(keyInStorageFormat), nullValue());
        } catch (Exception ex) {
            // If the format is incompatible then the object does not exist in the store
        }
    }

    private <K, K1, K2> void assertClientsPersisted(String component1Id, String component2Id, K idMain, K1 id1, K2 id2) {
        // Check that in the next transaction, the objects are still there
        withRealm(realmId, (session, realm) -> {
            @SuppressWarnings("unchecked")
            MapStorage<K, MapClientEntity<K>, ClientModel> storageMain = (MapStorage<K, MapClientEntity<K>, ClientModel>) session.getProvider(MapStorageProvider.class).getStorage(MapClientEntity.class, ClientModel.class);
            @SuppressWarnings("unchecked")
            MapStorage<K1, MapClientEntity<K1>, ClientModel> storage1 = (MapStorage<K1, MapClientEntity<K1>, ClientModel>) session.getComponentProvider(MapStorageProvider.class, component1Id).getStorage(MapClientEntity.class, ClientModel.class);
            @SuppressWarnings("unchecked")
            MapStorage<K2, MapClientEntity<K2>, ClientModel> storage2 = (MapStorage<K2, MapClientEntity<K2>, ClientModel>) session.getComponentProvider(MapStorageProvider.class, component2Id).getStorage(MapClientEntity.class, ClientModel.class);

            final StringKeyConvertor<K> kcMain = storageMain.getKeyConvertor();
            final StringKeyConvertor<K1> kc1 = storage1.getKeyConvertor();
            final StringKeyConvertor<K2> kc2 = storage2.getKeyConvertor();

            // Assert that the stores contain the created clients
            assertThat(storageMain.read(idMain), notNullValue());
            assertThat(storage1.read(id1), notNullValue());
            assertThat(storage2.read(id2), notNullValue());

            // Assert that the other stores do not contain the to-be-created clients (if they use compatible key format)
            assertClientDoesNotExist(storageMain, id1, kc1, kcMain);
            assertClientDoesNotExist(storageMain, id2, kc2, kcMain);
            assertClientDoesNotExist(storage1, idMain, kcMain, kc1);
            assertClientDoesNotExist(storage1, id2, kc2, kc1);
            assertClientDoesNotExist(storage2, idMain, kcMain, kc2);
            assertClientDoesNotExist(storage2, id1, kc1, kc2);
            assertThat(storageMain.read(idMain), notNullValue());

            return null;
        });
    }

    private String createMapStorageComponent(String name, String... config) {
        ComponentModel c1 = KeycloakModelUtils.createComponentModel(name, realmId, mapStorageProviderId, MapStorageProvider.class.getName(), config);

        return withRealm(realmId, (s, r) -> r.addComponentModel(c1).getId());
    }
}
