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

package org.keycloak.tests.organization.cache;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.IdentityProviderStorageProvider.FetchMode;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.CachedCount;
import org.keycloak.models.cache.infinispan.RealmCacheSession;
import org.keycloak.models.cache.infinispan.idp.IdentityProviderListQuery;
import org.keycloak.models.cache.infinispan.organization.CachedOrganization;
import org.keycloak.models.cache.infinispan.organization.CachedOrganizationIds;
import org.keycloak.models.cache.infinispan.organization.InfinispanOrganizationProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.cache.infinispan.idp.InfinispanIdentityProviderStorageProvider.cacheKeyForLogin;
import static org.keycloak.models.cache.infinispan.idp.InfinispanIdentityProviderStorageProvider.cacheKeyOrgId;
import static org.keycloak.models.cache.infinispan.organization.CachedOrganization.DOMAIN_NAMES_CACHE_MAX_SIZE;
import static org.keycloak.models.cache.infinispan.organization.InfinispanOrganizationProvider.cacheKeyOrgMemberCount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationCacheTest extends AbstractOrganizationTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void onBefore() {
        createOrganization("orga");
        createOrganization("orgb");
    }

    @AfterEach
    public void onAfter() {
        List<UserRepresentation> users = realm.admin().users().search("member");

        if (!users.isEmpty()) {
            UserRepresentation member = users.get(0);
            realm.admin().users().get(member.getId()).remove();
        }

        // clean up all organizations (cascades member/idp associations)
        realm.admin().organizations().list(-1, -1).forEach(org ->
                realm.admin().organizations().get(org.getId()).delete().close()
        );

        // clean up all realm-level IdPs
        realm.admin().identityProviders().findAll().forEach(idp ->
                realm.admin().identityProviders().get(idp.getAlias()).remove()
        );
    }

    @Test
    public void testGetByDomain() {
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = orgProvider.getByDomainName("orga.org");
            assertNotNull(acme);
            acme.setDomains(Set.of(new OrganizationDomainModel("acme.org")));
        });

        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = orgProvider.getByDomainName("orga.org");
            assertNull(acme);
            acme = orgProvider.getByDomainName("acme.org");
            assertNotNull(acme);
        });

        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = orgProvider.getByDomainName("acme.org");
            assertNotNull(acme);
            orgProvider.remove(acme);
        });

        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = orgProvider.getByDomainName("acme.org");
            assertNull(acme);
        });
    }

    @Test
    public void testGetByMember() {
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().addUser(realm, "member");
            member.setEnabled(true);
            orgProvider.addMember(orga, member);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            orgProvider.addMember(orgb, member);
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            Stream<OrganizationModel> memberOf = orgProvider.getByMember(member);
            assertEquals(2, memberOf.count());
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            orgProvider.remove(orga);
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            Stream<OrganizationModel> memberOf = orgProvider.getByMember(member);
            assertEquals(1, memberOf.count());
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            orgProvider.removeMember(orgb, member);
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            Stream<OrganizationModel> memberOf = orgProvider.getByMember(member);
            assertEquals(0, memberOf.count());
        });
    }

    @Test
    public void testGetByMemberId() {
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().addUser(realm, "member");
            member.setEnabled(true);
            orgProvider.addMember(orga, member);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            orgProvider.addMember(orgb, member);
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            UserModel memberOf = orgProvider.getMemberById(org, member.getId());
            assertNotNull(memberOf);
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            orgProvider.removeMember(org, member);
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            assertNull(orgProvider.getMemberById(orga, member.getId()));
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            assertNotNull(orgProvider.getMemberById(orgb, member.getId()));
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orgb = orgProvider.getByDomainName("orgb.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            assertEquals(1, orgProvider.getByMember(member).count());
            orgProvider.remove(orgb);
        });
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "member");
            assertEquals(0, orgProvider.getByMember(member).count());
        });
    }

    @Test
    public void testMembersCount() {
        runOnServer.run(session -> {
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
        runOnServer.run(session -> {
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
        runOnServer.run(session -> {
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
        runOnServer.run(session -> {
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

    @Test
    public void testCacheIDPByOrg() {
        IdentityProviderRepresentation idpRep = realm.admin().identityProviders().get("orga-identity-provider").toRepresentation();
        idpRep.setInternalId(null);
        idpRep.setOrganizationId(null);
        idpRep.setHideOnLogin(false);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);

        for (int i = 0; i < 10; i++) {
            final String alias = "org-idp-" + i;
            idpRep.setAlias(alias);
            realm.admin().identityProviders().create(idpRep).close();
            realm.cleanup().add(r -> {
                try {
                    r.identityProviders().get(alias).remove();
                } catch (NotFoundException ignored) {}
            });
        }

        String orgaId = realm.admin().organizations().list(-1, -1).get(0).getId();
        String orgbId = realm.admin().organizations().list(-1, -1).get(1).getId();

        for (int i = 0; i < 5; i++) {
            final String aliasA = "org-idp-" + i;
            final String aliasB = "org-idp-" + (i + 5);
            realm.admin().organizations().get(orgaId).identityProviders().addIdentityProvider(aliasA).close();
            realm.admin().organizations().get(orgbId).identityProviders().addIdentityProvider(aliasB).close();
        }

        runOnServer.run(session -> {
            IdentityProviderStorageProvider idpProvider = session.getProvider(IdentityProviderStorageProvider.class);
            RealmModel realm = session.getContext().getRealm();

            String cachedKeyA = cacheKeyOrgId(realm, orgaId);
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cachedKeyA, IdentityProviderListQuery.class);
            assertNull(identityProviderListQuery);
            String cachedKeyB = cacheKeyOrgId(realm, orgbId);
            identityProviderListQuery = realmCache.getCache().get(cachedKeyB, IdentityProviderListQuery.class);
            assertNull(identityProviderListQuery);


            idpProvider.getByOrganization(orgaId, null, null);
            identityProviderListQuery = realmCache.getCache().get(cachedKeyA, IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(6, identityProviderListQuery.getIDPs("-1.-1").size());

            idpProvider.getByOrganization(orgbId, 0, 2);
            idpProvider.getByOrganization(orgbId, 2, 6);
            identityProviderListQuery = realmCache.getCache().get(cachedKeyB, IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(2, identityProviderListQuery.getIDPs("0.2").size());
            assertEquals(4, identityProviderListQuery.getIDPs("2.6").size());
        });

        // update orga which should invalidate getByOrganization IDP cache
        OrganizationRepresentation rep = realm.admin().organizations().get(orgaId).toRepresentation();
        OrganizationDomainRepresentation orgDomainRep = new OrganizationDomainRepresentation();
        orgDomainRep.setName("orgaa.org");
        rep.addDomain(orgDomainRep);
        realm.admin().organizations().get(orgaId).update(rep).close();

        // update an IDP that is associated with orgb, that should invalidate getByOrganization IDP cache
        idpRep = realm.admin().identityProviders().get("org-idp-5").toRepresentation();
        idpRep.setDisplayName("something");
        realm.admin().identityProviders().get("org-idp-5").update(idpRep);

        runOnServer.run(session -> {
            IdentityProviderStorageProvider idpProvider = session.getProvider(IdentityProviderStorageProvider.class);
            RealmModel realm = session.getContext().getRealm();

            String cachedKeyA = cacheKeyOrgId(realm, orgaId);
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cachedKeyA, IdentityProviderListQuery.class);
            assertNull(identityProviderListQuery);

            String cachedKeyB = cacheKeyOrgId(realm, orgbId);
            identityProviderListQuery = realmCache.getCache().get(cachedKeyB, IdentityProviderListQuery.class);
            assertNull(identityProviderListQuery);
        });
    }

    @Test
    public void testCacheIDPForLogin() {
        // create 20 providers, and associate 10 of them with an organization.
        for (int i = 0; i < 20; i++) {
            final String alias = "idp-alias-" + i;
            IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
            idpRep.setAlias(alias);
            idpRep.setEnabled((i % 2) == 0); // half of the IDPs will be disabled and won't qualify for login.
            idpRep.setDisplayName("Broker " + i);
            idpRep.setProviderId("keycloak-oidc");
            realm.admin().identityProviders().create(idpRep).close();
            realm.cleanup().add(r -> r.identityProviders().findAll().forEach(idp -> r.identityProviders().get(idp.getAlias()).remove()));
        }

        String orgaId = realm.admin().organizations().list(-1, -1).get(0).getId();
        for (int i = 10; i < 20; i++) {
            realm.admin().organizations().get(orgaId).identityProviders().addIdentityProvider("idp-alias-" + i).close();
        }

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);

            // check all caches for login don't exist yet
            for (FetchMode fetchMode : IdentityProviderStorageProvider.FetchMode.values()) {
                String cachedKey = cacheKeyForLogin(realm, fetchMode);
                IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cachedKey, IdentityProviderListQuery.class);
                assertNull(identityProviderListQuery);
            }

            // perform some login IDP searches and ensure they are cached.
            session.identityProviders().getForLogin(FetchMode.REALM_ONLY, null);
            IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.REALM_ONLY), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(5, identityProviderListQuery.getIDPs("").size());

            session.identityProviders().getForLogin(FetchMode.ORG_ONLY, orgaId);
            identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.ORG_ONLY), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(5, identityProviderListQuery.getIDPs(orgaId).size());

            session.identityProviders().getForLogin(FetchMode.ALL, orgaId);
            identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.ALL), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(10, identityProviderListQuery.getIDPs(orgaId).size());
        });

        // 1- add/remove IDPs that are not available for login - none of these operations should invalidate the login caches.
        IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
        idpRep.setAlias("idp-alias-" + 20);
        idpRep.setEnabled(true);
        idpRep.setHideOnLogin(true); // this will make the new IDP not available for login.
        idpRep.setDisplayName("Broker " + 20);
        idpRep.setProviderId("keycloak-oidc");
        realm.admin().identityProviders().create(idpRep).close();

        // remove one IDP that was not available for login.
        realm.admin().identityProviders().get("idp-alias-1").remove();

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);

            // check all caches for login are still there.
            for (FetchMode fetchMode : IdentityProviderStorageProvider.FetchMode.values()) {
                String cachedKey = cacheKeyForLogin(realm, fetchMode);
                IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cachedKey, IdentityProviderListQuery.class);
                assertNotNull(identityProviderListQuery);
            }
        });

        // 2- update a couple of idps (one not available for login, one available), but don't change their login-availability status
        // none of these operations should invalidate the login caches.
        idpRep = realm.admin().identityProviders().get("idp-alias-20").toRepresentation();
        idpRep.getConfig().put("somekey", "somevalue");
        idpRep.setTrustEmail(true);
        realm.admin().identityProviders().get("idp-alias-20").update(idpRep); // should still be unavailable for login

        idpRep = realm.admin().identityProviders().get("idp-alias-0").toRepresentation();
        idpRep.getConfig().put("somekey", "somevalue");
        idpRep.setTrustEmail(true);
        realm.admin().identityProviders().get("idp-alias-0").update(idpRep); // should still be available for login

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);

            // check all caches for login are still there.
            for (FetchMode fetchMode : IdentityProviderStorageProvider.FetchMode.values()) {
                String cachedKey = cacheKeyForLogin(realm, fetchMode);
                IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cachedKey, IdentityProviderListQuery.class);
                assertNotNull(identityProviderListQuery);
            }
        });

        // 3- update an IDP, changing the availability for login - this should invalidate the caches.
        idpRep = realm.admin().identityProviders().get("idp-alias-20").toRepresentation();
        idpRep.setHideOnLogin(false);
        realm.admin().identityProviders().get("idp-alias-20").update(idpRep);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);

            // check all caches have been cleared.
            for (FetchMode fetchMode : IdentityProviderStorageProvider.FetchMode.values()) {
                String cachedKey = cacheKeyForLogin(realm, fetchMode);
                IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cachedKey, IdentityProviderListQuery.class);
                assertNull(identityProviderListQuery);
            }

            // re-do searches to populate the caches again and check the updated results.
            session.identityProviders().getForLogin(FetchMode.REALM_ONLY, null);
            IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.REALM_ONLY), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(6, identityProviderListQuery.getIDPs("").size());

            session.identityProviders().getForLogin(FetchMode.ORG_ONLY, orgaId);
            identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.ORG_ONLY), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(5, identityProviderListQuery.getIDPs(orgaId).size());

            session.identityProviders().getForLogin(FetchMode.ALL, orgaId);
            identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.ALL), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(11, identityProviderListQuery.getIDPs(orgaId).size());

        });

        // 4- finally, change one of the realm-level login IDPs, linking it to an org - although it still qualifies for login, it is now
        // linked to an org, which should invalidate all login caches.
        idpRep = realm.admin().identityProviders().get("idp-alias-20").toRepresentation();
        realm.admin().identityProviders().get("idp-alias-20").update(idpRep);
        realm.admin().organizations().get(orgaId).identityProviders().addIdentityProvider("idp-alias-20").close();

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);

            // check all caches have been cleared.
            for (FetchMode fetchMode : IdentityProviderStorageProvider.FetchMode.values()) {
                String cachedKey = cacheKeyForLogin(realm, fetchMode);
                IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cachedKey, IdentityProviderListQuery.class);
                assertNull(identityProviderListQuery);
            }

            // re-do searches to populate the caches again and check the updated results.
            session.identityProviders().getForLogin(FetchMode.REALM_ONLY, null);
            IdentityProviderListQuery identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.REALM_ONLY), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(5, identityProviderListQuery.getIDPs("").size());

            session.identityProviders().getForLogin(FetchMode.ORG_ONLY, orgaId);
            identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.ORG_ONLY), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(6, identityProviderListQuery.getIDPs(orgaId).size());

            session.identityProviders().getForLogin(FetchMode.ALL, orgaId);
            identityProviderListQuery = realmCache.getCache().get(cacheKeyForLogin(realm, FetchMode.ALL), IdentityProviderListQuery.class);
            assertNotNull(identityProviderListQuery);
            assertEquals(11, identityProviderListQuery.getIDPs(orgaId).size());

        });
    }

    @Test
    public void testGetByDomainCaseInsensitiveAndStaleCacheHandling() {
        final String domainLower = "case.org";
        final String domainMixed = "CaSe.Org";

        // 1. Create org with lowercase domain
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.create(null, "case-org", "case-org");
            org.setDomains(Set.of(new OrganizationDomainModel(domainLower)));
        });

        // 2. Look up by mixed-case domain (simulates login with user@CaSe.Org) to populate the cache
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(domainMixed);
            assertNotNull(org, "Mixed-case domain lookup should find the organization");
        });

        // 3. Delete the org
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(domainLower);
            assertNotNull(org);
            orgProvider.remove(org);
        });

        // 4. Recreate the org with the same domain
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.create(null, "case-org", "case-org");
            org.setDomains(Set.of(new OrganizationDomainModel(domainLower)));
        });

        // 5. Look up by mixed-case again (simulates login with user@CaSe.Org after org recreation).
        // Without the fix, the stale cache entry from step 2 (stored under the mixed-case key)
        // was not invalidated in step 3 (invalidation only targets the lowercase key), so it
        // still points to the old deleted org ID. getById returns null for that ID, and
        // findAny() on a null element throws NPE.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(domainMixed);
            assertNotNull(org, "Mixed-case domain lookup should find the recreated organization");
        });
    }

    @Test
    public void testGetByWildcardDomain() {
        final String wildcardOrgAlias = "wildcard-org";
        final String wildcard = "*.wildcard.org";
        final String childA = "a.wildcard.org";
        final String childB = "deep.a.wildcard.org";

        // 1. Create an organization owning "*.wildcard.org".
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.create(null, wildcardOrgAlias, wildcardOrgAlias);
            org.setDomains(Set.of(new OrganizationDomainModel(wildcard)));
        });

        // 2. Resolve two distinct literal subdomains to populate the domain-lookup cache entries
        //    under the wildcard org (both should resolve via the wildcard).
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(childA);
            assertNotNull(org);
            assertEquals(wildcardOrgAlias, org.getAlias());
            org = orgProvider.getByDomainName(childB);
            assertNotNull(org);
            assertEquals(wildcardOrgAlias, org.getAlias());
            // Also the bare base domain must match the wildcard.
            org = orgProvider.getByDomainName("wildcard.org");
            assertNotNull(org);
            assertEquals(wildcardOrgAlias, org.getAlias());
        });

        // 3. Replace the wildcard with an unrelated domain. The previously cached literal
        //    lookups (childA, childB, "wildcard.org") must be invalidated so they stop
        //    resolving to the wildcard org.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(wildcard);
            assertNotNull(org);
            org.setDomains(Set.of(new OrganizationDomainModel("unrelated.org")));
        });

        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            assertNull(orgProvider.getByDomainName(childA), "Cached child lookup must be invalidated after the wildcard is replaced");
            assertNull(orgProvider.getByDomainName(childB), "Cached nested-child lookup must be invalidated after the wildcard is replaced");
            assertNull(orgProvider.getByDomainName("wildcard.org"), "Cached base-domain lookup must be invalidated after the wildcard is replaced");
            // the new domain still resolves correctly
            OrganizationModel org = orgProvider.getByDomainName("unrelated.org");
            assertNotNull(org);
            assertEquals(wildcardOrgAlias, org.getAlias());
        });

        // 4. Remove the org entirely and check that the last cached lookup ("unrelated.org")
        //    is invalidated as well.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName("unrelated.org");
            assertNotNull(org);
            orgProvider.remove(org);
        });

        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            assertNull(orgProvider.getByDomainName("unrelated.org"));
        });
    }

    @Test
    public void testExactDomainOverridesCachedWildcardMatch() {
        final String wildcardOrgAlias = "wildcard-org";
        final String exactOrgAlias = "exact-org";
        final String exactDomain = "team.precedence.org";

        // Create a wildcard-owning org and resolve a literal subdomain through it so it lands in the cache.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.create(null, wildcardOrgAlias, wildcardOrgAlias);
            org.setDomains(Set.of(new OrganizationDomainModel("*.precedence.org")));
        });

        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(exactDomain);
            assertNotNull(org);
            assertEquals(wildcardOrgAlias, org.getAlias());
        });

        // Create another org taking the exact domain. This must invalidate the previously cached
        // "team.precedence.org" -> wildcard-org entry so the lookup now returns the exact-domain org.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.create(null, exactOrgAlias, exactOrgAlias);
            org.setDomains(Set.of(new OrganizationDomainModel(exactDomain)));
        });

        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(exactDomain);
            assertNotNull(org);
            assertEquals(exactOrgAlias, org.getAlias(), "Exact domain must win over the previously cached wildcard resolution");

            // sibling subdomain still resolves to the wildcard org
            OrganizationModel sibling = orgProvider.getByDomainName("sibling.precedence.org");
            assertNotNull(sibling);
            assertEquals(wildcardOrgAlias, sibling.getAlias());
        });

        // Removing the exact-domain org must invalidate its cached lookup so the wildcard takes over again.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(exactDomain);
            assertNotNull(org);
            orgProvider.remove(org);
        });

        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName(exactDomain);
            assertNotNull(org);
            assertEquals(wildcardOrgAlias, org.getAlias(), "After removing the exact-domain org, resolution must fall back to the wildcard org");
        });
    }

    @Test
    public void testBoundedDomainNamesInCache() {
        String wildcardDomain = "*.bounded.test.org";

        // 1. Create an organization whose only configured domain is a wildcard so that all
        //    sub*.bounded.test.org look-ups resolve to it without needing per-domain DB entries.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.create(null, "bounded-org", "bounded-org");
            org.setDomains(Set.of(new OrganizationDomainModel(wildcardDomain)));
        });

        // 2. Within ONE session, resolve MAX_DOMAIN_NAMES + 1 distinct sub-domains.
        //    Each resolution causes addDomainName() to be called on the shared CachedOrganization,
        //    which eventually triggers the LRU eviction and the cache-key invalidation.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            for (int i = 0; i <= DOMAIN_NAMES_CACHE_MAX_SIZE; i++) {
                OrganizationModel org = orgProvider.getByDomainName("sub" + i + ".bounded.test.org");
                assertNotNull(org);
            }
        });

        // 3. In a fresh session verify the bounded-list invariants.
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);

            // sub0 was the second domain evicted (after *.bounded.test.org).
            // Its CachedOrganizationIds entry must have been invalidated and therefore absent.
            String evictedDomainCacheKey = InfinispanOrganizationProvider.cacheKeyByDomain(realm, "sub0.bounded.test.org");
            CachedOrganizationIds evictedCachedIds = realmCache.getCache().get(evictedDomainCacheKey, CachedOrganizationIds.class);
            assertNull(evictedCachedIds,
                    "sub0.bounded.test.org was evicted from the bounded domainNames list; its cache entry must be gone");

            // sub100 was the last domain added and must still be present in the cache.
            String lastDomainCacheKey = InfinispanOrganizationProvider.cacheKeyByDomain(realm, "sub100.bounded.test.org");
            CachedOrganizationIds lastCachedIds = realmCache.getCache().get(lastDomainCacheKey, CachedOrganizationIds.class);
            assertNotNull(lastCachedIds,
                    "sub100.bounded.test.org was never evicted; its cache entry must still be present");

            // The CachedOrganization's domainNames map must be bounded to exactly MAX_DOMAIN_NAMES entries
            // (sub1 … sub100) after the two evictions.
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByDomainName("sub100.bounded.test.org");
            assertNotNull(org);
            CachedOrganization cachedOrg = realmCache.getCache().get(org.getId(), CachedOrganization.class);
            assertNotNull(cachedOrg);
            assertEquals(DOMAIN_NAMES_CACHE_MAX_SIZE, cachedOrg.getDomainNames().size(),
                    "The domainNames list in CachedOrganization must be bounded to " + DOMAIN_NAMES_CACHE_MAX_SIZE + " entries");
        });
    }

    /**
     * Concurrent variant of {@link #testBoundedDomainNamesInCache()}.
     *
     * <p>The {@code CachedOrganization.domainNames} map is a {@code Collections.synchronizedMap}-
     * wrapped {@code LinkedHashMap} with an LRU eviction policy that fires whenever the map
     * exceeds {@link CachedOrganization#DOMAIN_NAMES_CACHE_MAX_SIZE} entries.  This test launches
     * {@code 2 × MAX_SIZE + 1} threads simultaneously, each running its own Keycloak transaction
     * and calling {@link OrganizationProvider#getByDomainName} with a unique sub-domain.  All
     * threads are held behind a start-gate latch so they hit the shared {@code CachedOrganization}
     * at the same time, maximising contention on the synchronized map, the eviction callback and
     * the Infinispan cache operations.
     *
     * <p>The test verifies:
     * <ol>
     *   <li>No exception (e.g. {@code ConcurrentModificationException}, deadlock timeout) is
     *       thrown from any thread.</li>
     *   <li>Every {@code getByDomainName} call returns a non-null result.</li>
     *   <li>After all threads finish, {@code CachedOrganization.domainNames.size()} does not
     *       exceed {@link CachedOrganization#DOMAIN_NAMES_CACHE_MAX_SIZE}.</li>
     * </ol>
     */
    @Test
    public void testBoundedDomainNamesInCacheConcurrent() {
        final String wildcardDomain = "*.concurrent.bounded.test.org";
        // Use 2×MAX+1 threads so that far more domains are submitted than the map can hold,
        // guaranteeing that evictions actually occur under concurrency.
        final int threadCount = DOMAIN_NAMES_CACHE_MAX_SIZE * 2 + 1;

        // 1. Create an org with a wildcard domain so every sub*.concurrent.bounded.test.org
        //    lookup resolves to it.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.create(null, "concurrent-bounded-org", "concurrent-bounded-org");
            org.setDomains(Set.of(new OrganizationDomainModel(wildcardDomain)));
        });

        // 2. Warm-up: resolve one domain so the CachedOrganization is already stored in the
        //    Infinispan cache before the concurrent calls start.  All threads will then find and
        //    share the same CachedOrganization object and call addDomainName() on it – the
        //    scenario that exercises the synchronized LRU map under real concurrency.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            assertNotNull(orgProvider.getByDomainName("warmup.concurrent.bounded.test.org"));
        });

        // 3. Fire all threads at the same instant via a start-gate latch.  Each thread runs its
        //    own independent transaction through KeycloakModelUtils.runJobInTransaction so that
        //    sessions do not share any per-session state (managedOrganizations map, invalidation
        //    sets, etc.) while still operating on the same shared Infinispan CachedOrganization.
        runOnServer.run(session -> {
            KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
            String realmId = session.getContext().getRealm().getId();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate  = new CountDownLatch(threadCount);
            AtomicReference<Throwable> firstError = new AtomicReference<>();

            for (int i = 0; i < threadCount; i++) {
                final String domain = "sub" + i + ".concurrent.bounded.test.org";
                executor.submit(() -> {
                    try {
                        startGate.await(); // hold until all threads are ready
                        KeycloakModelUtils.runJobInTransaction(factory, s -> {
                            s.getContext().setRealm(s.realms().getRealm(realmId));
                            OrganizationProvider op = s.getProvider(OrganizationProvider.class);
                            assertNotNull(op.getByDomainName(domain),
                                    "getByDomainName must resolve the org for domain: " + domain);
                        });
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        doneGate.countDown();
                    }
                });
            }

            startGate.countDown(); // release all threads simultaneously

            try {
                if (!doneGate.await(60, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timed out waiting for concurrent domain lookups to finish");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for concurrent domain lookups", e);
            } finally {
                executor.shutdownNow();
            }

            Throwable error = firstError.get();

            if (error != null) {
                throw new RuntimeException("A concurrent domain lookup thread failed", error);
            }
        });

        // 4. In a fresh session verify the bounded-map invariant.
        //    We obtain the org via getAllStream() – which calls getById() on a cache hit but does
        //    NOT call addDomainName() – then inspect the CachedOrganization directly from the
        //    Infinispan cache to avoid disturbing the domainNames map further.
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmCacheSession realmCache = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            OrganizationModel org = orgProvider.getAllStream("concurrent-bounded-org", true, null, null)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("concurrent-bounded-org not found"));
            CachedOrganization cachedOrg = realmCache.getCache().get(org.getId(), CachedOrganization.class);
            assertNotNull(cachedOrg, "CachedOrganization must still be present after concurrent lookups");

            int size = cachedOrg.getDomainNames().size();
            assertTrue(size <= DOMAIN_NAMES_CACHE_MAX_SIZE,
                    "domainNames size " + size + " exceeded the bound of " + DOMAIN_NAMES_CACHE_MAX_SIZE
                            + " under concurrent access – the LRU eviction is not thread-safe");
        });
    }
}
