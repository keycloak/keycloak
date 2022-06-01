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
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorage;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory;
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
@RequireProvider(value = MapStorageProvider.class, only = {ConcurrentHashMapStorageProviderFactory.PROVIDER_ID})
public class ConcurrentHashMapStorageTest extends KeycloakModelTest {

    private static final Logger LOG = Logger.getLogger(ConcurrentHashMapStorageTest.class.getName());

    private String realmId;

    private String mapStorageProviderId;

    @Before
    public void initMapStorageProviderId() {
        MapStorageProviderFactory ms = (MapStorageProviderFactory) getFactory().getProviderFactory(MapStorageProvider.class, ConcurrentHashMapStorageProviderFactory.PROVIDER_ID);
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

        String[] ids = withRealm(realmId, (session, realm) -> {
            ConcurrentHashMapStorage<K, MapClientEntity, ClientModel> storageMain = (ConcurrentHashMapStorage<K, MapClientEntity, ClientModel>) (MapStorage) session.getProvider(MapStorageProvider.class, ConcurrentHashMapStorageProviderFactory.PROVIDER_ID).getStorage(ClientModel.class);
            ConcurrentHashMapStorage<K1, MapClientEntity, ClientModel> storage1 = (ConcurrentHashMapStorage<K1, MapClientEntity, ClientModel>) (MapStorage) session.getComponentProvider(MapStorageProvider.class, component1Id).getStorage(ClientModel.class);
            ConcurrentHashMapStorage<K2, MapClientEntity, ClientModel> storage2 = (ConcurrentHashMapStorage<K2, MapClientEntity, ClientModel>) (MapStorage) session.getComponentProvider(MapStorageProvider.class, component2Id).getStorage(ClientModel.class);

            // Assert that the map storage can be used both as a standalone store and a component
            assertThat(storageMain, notNullValue());
            assertThat(storage1, notNullValue());
            assertThat(storage2, notNullValue());

            final StringKeyConverter<K> kcMain = storageMain.getKeyConverter();
            final StringKeyConverter<K1> kc1 = storage1.getKeyConverter();
            final StringKeyConverter<K2> kc2 = storage2.getKeyConverter();

            String idMain = kcMain.keyToString(kcMain.yieldNewUniqueKey());
            String id1    = kc1.keyToString(kc1.yieldNewUniqueKey());
            String id2    = kc2.keyToString(kc2.yieldNewUniqueKey());

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

            MapClientEntity clientMain = new MapClientEntityImpl();
            clientMain.setId(idMain);
            clientMain.setRealmId(realmId);
            MapClientEntity client1 = new MapClientEntityImpl();
            client1.setId(id1);
            client1.setRealmId(realmId);
            MapClientEntity client2 = new MapClientEntityImpl();
            client2.setId(id2);
            client2.setRealmId(realmId);

            clientMain = storageMain.create(clientMain);
            client1 = storage1.create(client1);
            client2 = storage2.create(client2);

            return new String[] {clientMain.getId(), client1.getId(), client2.getId()};
        });

        String idMain = ids[0];
        String id1 = ids[1];
        String id2 = ids[2];

        LOG.debugf("Object IDs: %s, %s, %s", idMain, id1, id2);

        assertClientsPersisted(component1Id, component2Id, idMain, id1, id2);

        // Invalidate one component and check that the storage still contains what it should
        getFactory().invalidate(null, ObjectType.COMPONENT, component1Id);
        assertClientsPersisted(component1Id, component2Id, idMain, id1, id2);

        // Invalidate whole realm and check that the storage still contains what it should
        getFactory().invalidate(null, ObjectType.REALM, realmId);
        assertClientsPersisted(component1Id, component2Id, idMain, id1, id2);

        // Refresh factory (akin server restart) and check that the storage still contains what it should
        reinitializeKeycloakSessionFactory();
        assertClientsPersisted(component1Id, component2Id, idMain, id1, id2);
    }

    private <K,K1> void assertClientDoesNotExist(ConcurrentHashMapStorage<K, MapClientEntity, ClientModel> storage, String id, final StringKeyConverter<K1> kc, final StringKeyConverter<K> kcStorage) {
        // Assert that the other stores do not contain the to-be-created clients (if they use compatible key format)
        try {
            assertThat(storage.read(id), nullValue());
        } catch (Exception ex) {
            // If the format is incompatible then the object does not exist in the store
        }
    }

    private <K, K1, K2> void assertClientsPersisted(String component1Id, String component2Id, String idMain, String id1, String id2) {
        // Check that in the next transaction, the objects are still there
        withRealm(realmId, (session, realm) -> {
            @SuppressWarnings("unchecked")
            ConcurrentHashMapStorage<K, MapClientEntity, ClientModel> storageMain = (ConcurrentHashMapStorage<K, MapClientEntity, ClientModel>) (MapStorage) session.getProvider(MapStorageProvider.class, ConcurrentHashMapStorageProviderFactory.PROVIDER_ID).getStorage(ClientModel.class);
            @SuppressWarnings("unchecked")
            ConcurrentHashMapStorage<K1, MapClientEntity, ClientModel> storage1 = (ConcurrentHashMapStorage<K1, MapClientEntity, ClientModel>) (MapStorage) session.getComponentProvider(MapStorageProvider.class, component1Id).getStorage(ClientModel.class);
            @SuppressWarnings("unchecked")
            ConcurrentHashMapStorage<K2, MapClientEntity, ClientModel> storage2 = (ConcurrentHashMapStorage<K2, MapClientEntity, ClientModel>) (MapStorage) session.getComponentProvider(MapStorageProvider.class, component2Id).getStorage(ClientModel.class);

            final StringKeyConverter<K> kcMain = storageMain.getKeyConverter();
            final StringKeyConverter<K1> kc1 = storage1.getKeyConverter();
            final StringKeyConverter<K2> kc2 = storage2.getKeyConverter();

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
