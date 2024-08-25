/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.organization.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.models.cache.infinispan.organization.InfinispanOrganizationProvider.cacheKeyOrgMemberCount;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.RealmCacheSession;
import org.keycloak.models.cache.infinispan.CachedCount;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.runonserver.RunOnServer;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationCacheTest extends AbstractOrganizationTest {

    @Before
    public void onBefore() {
        createOrganization("orga");
        createOrganization("orgb");
    }

    @After
    public void onAfter() {
        List<UserRepresentation> users = testRealm().users().search("member");

        if (!users.isEmpty()) {
            UserRepresentation member = users.get(0);
            testRealm().users().get(member.getId()).remove();
        }
    }

    @Test
    public void testGetByDomain() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = orgProvider.getByDomainName("orga.org");
            assertNotNull(acme);
            acme.setDomains(Set.of(new OrganizationDomainModel("acme.org")));
        });

        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = orgProvider.getByDomainName("orga.org");
            assertNull(acme);
            acme = orgProvider.getByDomainName("acme.org");
            assertNotNull(acme);
        });

        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = orgProvider.getByDomainName("acme.org");
            assertNotNull(acme);
            orgProvider.remove(acme);
        });

        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = orgProvider.getByDomainName("acme.org");
            assertNull(acme);
        });
    }

    @Test
    public void testGetByMember() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().addUser(realm, "member");
            member.setEnabled(true);
            orgProvider.addMember(orga, member);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            orgProvider.addMember(orgb, member);
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            Stream<OrganizationModel> memberOf = orgProvider.getByMember(member);
            assertEquals(2, memberOf.count());
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            orgProvider.remove(orga);
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            Stream<OrganizationModel> memberOf = orgProvider.getByMember(member);
            assertEquals(1, memberOf.count());
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            orgProvider.removeMember(orgb, member);
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            Stream<OrganizationModel> memberOf = orgProvider.getByMember(member);
            assertEquals(0, memberOf.count());
        });
    }

    @Test
    public void testGetByMemberId() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().addUser(realm, "member");
            member.setEnabled(true);
            orgProvider.addMember(orga, member);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            orgProvider.addMember(orgb, member);
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            UserModel memberOf = orgProvider.getMemberById(org, member.getId());
            assertNotNull(memberOf);
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            orgProvider.removeMember(org, member);
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            assertNull(orgProvider.getMemberById(orga, member.getId()));
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            assertNotNull(orgProvider.getMemberById(orgb, member.getId()));
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            assertEquals(1, orgProvider.getByMember(member).count());
            orgProvider.remove(orgb);
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            assertEquals(0, orgProvider.getByMember(member).count());
        });
    }

    @Test
    public void testMembersCount() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().addUser(realm, "member");
            member.setEnabled(true);
            orgProvider.addMember(orgb, member);

            String cachedKey = cacheKeyOrgMemberCount(realm, orgb);
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            CachedCount cached = realmCache.getCache().get(cachedKey, CachedCount.class);

            // initially members count is not cached
            assertNull(cached);

            // members count is cached after first call of getMembersCount()
            long membersCount = orgProvider.getMembersCount(orgb);
            assertEquals(1, membersCount);
            cached = realmCache.getCache().get(cachedKey, CachedCount.class);
            assertNotNull(cached);
            assertEquals(1, cached.getCount());

            UserModel user = session.users().addUser(realm, "another-member");
            user.setEnabled(true);
            orgProvider.addMember(orgb, user);
        });

        // addMember invalidates cached members count
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            RealmModel realm = session.getContext().getRealm();

            String cachedKey = cacheKeyOrgMemberCount(realm, orgb);
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            CachedCount cached = realmCache.getCache().get(cachedKey, CachedCount.class);

            assertNull(cached);
            assertEquals(2, orgProvider.getMembersCount(orgb));

            cached = realmCache.getCache().get(cachedKey, CachedCount.class);
            assertNotNull(cached);
            assertEquals(2, cached.getCount());

            orgProvider.removeMember(orgb, session.users().getUserByUsername(realm, "another-member"));
        });

        // removeMember invalidates cached members count
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            RealmModel realm = session.getContext().getRealm();

            String cachedKey = cacheKeyOrgMemberCount(realm, orgb);
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            CachedCount cached = realmCache.getCache().get(cachedKey, CachedCount.class);

            assertNull(cached);
            assertEquals(1, orgProvider.getMembersCount(orgb));

            cached = realmCache.getCache().get(cachedKey, CachedCount.class);
            assertNotNull(cached);
            assertEquals(1, cached.getCount());

            session.users().removeUser(realm, session.users().getUserByUsername(realm, "member"));
        });

        // remove user from the realm invalidates cached members count
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            RealmModel realm = session.getContext().getRealm();

            String cachedKey = cacheKeyOrgMemberCount(realm, orgb);
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            CachedCount cached = realmCache.getCache().get(cachedKey, CachedCount.class);

            assertNull(cached);
            assertEquals(0, orgProvider.getMembersCount(orgb));

            cached = realmCache.getCache().get(cachedKey, CachedCount.class);
            assertNotNull(cached);
            assertEquals(0, cached.getCount());
        });
    }
}
