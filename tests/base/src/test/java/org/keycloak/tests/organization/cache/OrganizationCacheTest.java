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
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.IdentityProviderStorageProvider.FetchMode;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.CachedCount;
import org.keycloak.models.cache.infinispan.RealmCacheSession;
import org.keycloak.models.cache.infinispan.idp.IdentityProviderListQuery;
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
import static org.keycloak.models.cache.infinispan.organization.InfinispanOrganizationProvider.cacheKeyOrgMemberCount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
