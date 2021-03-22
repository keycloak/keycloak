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
import javax.ws.rs.core.Response;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.federation.HardcodedRoleStorageProviderFactory;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class RoleStorageTest extends AbstractTestRealmKeycloakTest {

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
        provider.setName("role-storage-hardcoded");
        provider.setProviderId(HardcodedRoleStorageProviderFactory.PROVIDER_ID);
        provider.setProviderType(RoleStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle(HardcodedRoleStorageProviderFactory.ROLE_NAME, "hardcoded-role");
        provider.getConfig().putSingle(HardcodedRoleStorageProviderFactory.DELAYED_SEARCH, Boolean.toString(false));

        providerId = addComponent(provider);
    }

    @Test
    public void testGetRole() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            RoleModel hardcoded = realm.getRole("hardcoded-role");
            assertNotNull(hardcoded);
        });
    }

    @Test
    public void testGetRoleById() {
        String providerId = this.providerId;
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            StorageId storageId = new StorageId(providerId, "hardcoded-role");
            RoleModel hardcoded = realm.getRoleById(storageId.getId());
            assertNotNull(hardcoded);
        });
    }

    @Test
    public void testSearchTimeout() throws Exception{
        runTestWithTimeout(4000, () -> {
            String hardcodedRole = HardcodedRoleStorageProviderFactory.PROVIDER_ID;
            String delayedSearch = HardcodedRoleStorageProviderFactory.DELAYED_SEARCH;
            String providerId = this.providerId;
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName(AuthRealm.TEST);

                assertThat(session.roleStorageManager()
                            .searchForRolesStream(realm, "role", null, null)
                            .map(RoleModel::getName)
                            .collect(Collectors.toList()),
                        allOf(
                            hasItem(hardcodedRole),
                            hasItem("sample-realm-role"))
                        );

                //update the provider to simulate delay during the search
                ComponentModel memoryProvider = realm.getComponent(providerId);
                memoryProvider.getConfig().putSingle(delayedSearch, Boolean.toString(true));
                realm.updateComponent(memoryProvider);
            });

            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName(AuthRealm.TEST);
                // search for roles and check hardcoded-role is not present
                assertThat(session.roleStorageManager()
                            .searchForRolesStream(realm, "role", null, null)
                            .map(RoleModel::getName)
                            .collect(Collectors.toList()),
                        allOf(
                            not(hasItem(hardcodedRole)),
                            hasItem("sample-realm-role")
                        ));
            });
        });
    }

    /* 
        TODO review caching of roles, it behaves a little bit different than clients so following tests fails.
        Tracked as KEYCLOAK-14938.
    */
//    @Test
//    public void testDailyEviction() {
//        testNotCached();
//
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
//
//    }
//
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
//
//    private void testNotCached() {
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleModel hardcoded = realm.getRole("hardcoded-role");
//            Assert.assertNotNull(hardcoded);
//            Assert.assertThat(hardcoded, not(instanceOf(RoleAdapter.class)));
//        });
//    }
//
//    private void testIsCached() {
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleModel hardcoded = realm.getRole("hardcoded-role");
//            Assert.assertNotNull(hardcoded);
//            Assert.assertThat(hardcoded, instanceOf(RoleAdapter.class));
//        });
//    }
//
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
//
//    private void setDefaultCachePolicy() {
//        testingClient.server().run(session -> {
//            RealmModel realm = session.realms().getRealmByName("test");
//            RoleStorageProviderModel model = realm.getRoleStorageProviders().get(0);
//            model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.DEFAULT);
//            realm.updateComponent(model);
//        });
//    }
}
