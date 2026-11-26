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
import java.time.Duration;
import java.util.Calendar;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.RoleAdapter;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.CacheableStorageProviderModel.CachePolicy;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.storage.role.RoleStorageProviderModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.federation.HardcodedRoleStorageProviderFactory;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

                assertThat(session.roles()
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
                assertThat(session.roles()
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

    @Test
    public void testNoCache() {
        testIsCached();
        try {
            setCachePolicy(CachePolicy.NO_CACHE, null, -1);
            testNotCached();
            testNotCached();
        } finally {
            setDefaultCachePolicy();
        }
        testIsCached();
    }
    @Test
    public void testDailyEviction() {
        testEviction(CachePolicy.EVICT_DAILY, Duration.ofHours(1), null, Duration.ofHours(2));
    }
    @Test
    public void testWeeklyEviction() {
        testEviction(CachePolicy.EVICT_WEEKLY, Duration.ofDays(4), Duration.ofDays(2), Duration.ofDays(5));
    }
    @Test
    public void testMaxLifespan() {
        testIsCached();
        try {
            setCachePolicy(CachePolicy.MAX_LIFESPAN, null, Duration.ofHours(1).toMillis());
            testIsCached();
            assertCachedAt(Duration.ofMinutes(30));
            assertNotCachedAt(Duration.ofHours(2));
        } finally {
            timeOffSet.set(0);
            setDefaultCachePolicy();
        }
        testIsCached();
    }
    private void testEviction(CachePolicy policy, Duration evictionOffset, Duration cachedOffset, Duration expiredOffset) {
        testIsCached();
        try {
            setCachePolicy(policy, evictionOffset, -1);
            testIsCached();
            if (cachedOffset != null) {
                assertCachedAt(cachedOffset);
            }
            assertNotCachedAt(expiredOffset);
        } finally {
            timeOffSet.set(0);
            setDefaultCachePolicy();
        }
        testIsCached();
    }
    private void assertCachedAt(Duration offset) {
        timeOffSet.set(Math.toIntExact(offset.toSeconds())); testIsCached();
    }
    private void assertNotCachedAt(Duration offset) {
        timeOffSet.set(Math.toIntExact(offset.toSeconds())); testNotCached(); testIsCached();
    }
    private void testNotCached() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            RoleModel hardcoded = realm.getRole("hardcoded-role");
            assertNotNull(hardcoded);
            org.junit.jupiter.api.Assertions.assertFalse(hardcoded instanceof RoleAdapter);
        });
    }
    private void testIsCached() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            RoleModel hardcoded = realm.getRole("hardcoded-role");
            assertNotNull(hardcoded);
            org.junit.jupiter.api.Assertions.assertTrue(hardcoded instanceof RoleAdapter);
        });
    }
    private void setDefaultCachePolicy() { setCachePolicy(CachePolicy.DEFAULT, null, -1); }
    private void setCachePolicy(CachePolicy policy, Duration evictionOffset, long maxLifespan) {
        String providerId = this.providerId;
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            RoleStorageProviderModel model = new RoleStorageProviderModel(realm.getComponent(providerId));
            model.setCachePolicy(policy);
            if (maxLifespan > 0) {
                model.setMaxLifespan(maxLifespan);
            }
            if (evictionOffset != null) {
                Calendar eviction = Calendar.getInstance();
                eviction.add(Calendar.HOUR, Math.toIntExact(evictionOffset.toHours()));
                if (policy == CachePolicy.EVICT_WEEKLY) {
                    model.setEvictionDay(eviction.get(Calendar.DAY_OF_WEEK));
                }
                model.setEvictionHour(eviction.get(Calendar.HOUR_OF_DAY));
                model.setEvictionMinute(eviction.get(Calendar.MINUTE));
            }
            realm.updateComponent(model);
        });
    }

}
