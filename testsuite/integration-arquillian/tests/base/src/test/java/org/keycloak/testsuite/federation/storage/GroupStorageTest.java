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

package org.keycloak.testsuite.federation.storage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.GroupAdapter;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.group.GroupStorageProvider;
import org.keycloak.storage.group.GroupStorageProviderModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.federation.HardcodedGroupStorageProviderFactory;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GroupStorageTest extends AbstractTestRealmKeycloakTest {

    private String providerId;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    protected String addComponent(ComponentRepresentation component) {
        try (Response resp = adminClient.realm("test").components().add(component)) {
            String id = ApiUtil.getCreatedId(resp);
            getCleanup().addComponentId(id);
            return id;
        }
    }

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("group-storage-hardcoded");
        provider.setProviderId(HardcodedGroupStorageProviderFactory.PROVIDER_ID);
        provider.setProviderType(GroupStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle(HardcodedGroupStorageProviderFactory.GROUP_NAME, "hardcoded-group");
        provider.getConfig().putSingle(HardcodedGroupStorageProviderFactory.DELAYED_SEARCH, Boolean.toString(false));

        providerId = addComponent(provider);
    }

    @Test
    public void testGetGroupById() {
        String providerId = this.providerId;
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            StorageId storageId = new StorageId(providerId, "hardcoded-group");
            GroupModel hardcoded = session.groups().getGroupById(realm, storageId.getId());
            assertNotNull(hardcoded);
        });
    }

    @Test
    public void testSearchTimeout() throws Exception{
        runTestWithTimeout(4000, () -> {
            String hardcodedGroup = HardcodedGroupStorageProviderFactory.PROVIDER_ID;
            String delayedSearch = HardcodedGroupStorageProviderFactory.DELAYED_SEARCH;
            String providerId = this.providerId;
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName(AuthRealm.TEST);

                assertThat(session.groups()
                            .searchForGroupByNameStream(realm, "group", false, null, null)
                            .map(GroupModel::getName)
                            .collect(Collectors.toList()),
                        allOf(
                            hasItem(hardcodedGroup),
                            hasItem("sample-realm-group"))
                        );

                //update the provider to simulate delay during the search
                ComponentModel memoryProvider = realm.getComponent(providerId);
                memoryProvider.getConfig().putSingle(delayedSearch, Boolean.toString(true));
                realm.updateComponent(memoryProvider);
            });

            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName(AuthRealm.TEST);
                // search for groups and check hardcoded-group is not present
                assertThat(session.groups()
                            .searchForGroupByNameStream(realm, "group", false, null, null)
                            .map(GroupModel::getName)
                            .collect(Collectors.toList()),
                        allOf(
                            not(hasItem(hardcodedGroup)),
                            hasItem("sample-realm-group")
                        ));
            });
        });
    }

    @Test
    public void testNoCache() {
        testIsCached();

        try {
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                GroupStorageProviderModel model = new GroupStorageProviderModel(realm.getComponent(providerId));
                model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
                realm.updateComponent(model);
            });

            testNotCached();
            testNotCached();
        } finally {
            setDefaultCachePolicy();
        }

        testIsCached();
    }

    private void testNotCached() {
        String providerId = this.providerId;
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            StorageId storageId = new StorageId(providerId, "hardcoded-group");
            GroupModel hardcoded = session.groups().getGroupById(realm, storageId.getId());
            assertNotNull(hardcoded);
            assertFalse(hardcoded instanceof GroupAdapter);
        });
    }

    private void testIsCached() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            GroupModel hardcoded = session.groups().getGroupById(realm, new StorageId(providerId, "hardcoded-group").getId());
            assertNotNull(hardcoded);
            assertTrue(hardcoded instanceof GroupAdapter);
        });
    }

    private void setDefaultCachePolicy() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            GroupStorageProviderModel model = new GroupStorageProviderModel(realm.getComponent(providerId));
            model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.DEFAULT);
            realm.updateComponent(model);
        });
    }

    // TODO review caching of groups, it behaves a little bit different than clients so daily/weekly eviction tests still need attention.
    // Tracked as KEYCLOAK-15135.
}
