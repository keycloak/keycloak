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

import org.keycloak.common.Profile.Feature;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.group.GroupStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.federation.HardcodedGroupStorageProviderFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@AuthServerContainerExclude(AuthServer.REMOTE)
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

    @BeforeClass
    public static void checkNotMapStorage() {
        ProfileAssume.assumeFeatureDisabled(Feature.MAP_STORAGE);
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
                            .searchForGroupByName(realm, "group", null, null).stream()
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
                            .searchForGroupByName(realm, "group", null, null).stream()
                            .map(GroupModel::getName)
                            .collect(Collectors.toList()),
                        allOf(
                            not(hasItem(hardcodedGroup)),
                            hasItem("sample-realm-group")
                        ));
            });
        });
    }

    /* 
        TODO review caching of groups, it behaves a little bit different than clients so following tests fails.
        Tracked as KEYCLOAK-15135.
    */
//    @Test
//    public void testDailyEviction() {
//        testNotCached();

//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleStorageProviderModel model = realm.getRoleStorageProviders().get(0);
//            Calendar eviction = Calendar.getInstance();
//            eviction.add(Calendar.HOUR, 1);
//            model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.EVICT_DAILY);
//            model.setEvictionHour(eviction.get(HOUR_OF_DAY));
//            model.setEvictionMinute(eviction.get(MINUTE));
//            realm.updateComponent(model);
//        });
//        testIsCached();
//        setTimeOffset(2 * 60 * 60); // 2 hours in future
//        testNotCached();
//        testIsCached();
//
//        setDefaultCachePolicy();
//        testIsCached();

//    }

//    @Test
//    public void testWeeklyEviction() {
//        testNotCached();
//
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleStorageProviderModel model = realm.getRoleStorageProviders().get(0);
//            Calendar eviction = Calendar.getInstance();
//            eviction.add(Calendar.HOUR, 4 * 24);
//            model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.EVICT_WEEKLY);
//            model.setEvictionDay(eviction.get(DAY_OF_WEEK));
//            model.setEvictionHour(eviction.get(HOUR_OF_DAY));
//            model.setEvictionMinute(eviction.get(MINUTE));
//            realm.updateComponent(model);
//        });
//        testIsCached();
//        setTimeOffset(2 * 24 * 60 * 60); // 2 days in future
//        testIsCached();
//        setTimeOffset(5 * 24 * 60 * 60); // 5 days in future
//        testNotCached();
//        testIsCached();
//
//        setDefaultCachePolicy();
//        testIsCached();
//
//    }
//
//    @Test
//    public void testMaxLifespan() {
//        testNotCached();
//
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleStorageProviderModel model = realm.getRoleStorageProviders().get(0);
//            model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.MAX_LIFESPAN);
//            model.setMaxLifespan(1 * 60 * 60 * 1000);
//            realm.updateComponent(model);
//        });
//        testIsCached();
//
//        setTimeOffset(1/2 * 60 * 60); // 1/2 hour in future
//
//        testIsCached();
//
//        setTimeOffset(2 * 60 * 60); // 2 hours in future
//
//        testNotCached();
//        testIsCached();
//
//        setDefaultCachePolicy();
//        testIsCached();
//
//    }

//    private void testNotCached() {
//        String providerId = this.providerId;
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            StorageId storageId = new StorageId(providerId, "hardcoded-group");
//            GroupModel hardcoded = session.groups().getGroupById(realm, storageId.getId());
//            Assert.assertNotNull(hardcoded);
//            Assert.assertThat(hardcoded, not(instanceOf(GroupAdapter.class)));
//        });
//    }

//    private void testIsCached() {
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleModel hardcoded = realm.getRole("hardcoded-role");
//            Assert.assertNotNull(hardcoded);
//            Assert.assertThat(hardcoded, instanceOf(RoleAdapter.class));
//        });
//    }

//    @Test
//    public void testNoCache() {
//        testNotCached();
//
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleStorageProviderModel model = realm.getRoleStorageProviders().get(0);
//            model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
//            realm.updateComponent(model);
//        });
//
//        testNotCached();
//
//        // test twice because updating component should evict
//        testNotCached();
//
//        // set it back
//        setDefaultCachePolicy();
//        testIsCached();
//    }

//    private void setDefaultCachePolicy() {
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleStorageProviderModel model = realm.getRoleStorageProviders().get(0);
//            model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.DEFAULT);
//            realm.updateComponent(model);
//        });
//    }
}
