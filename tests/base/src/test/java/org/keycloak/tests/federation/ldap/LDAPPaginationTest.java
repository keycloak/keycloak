/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.federation.ldap;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestCleanup;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.apache.directory.api.ldap.model.exception.LdapConfigurationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link LDAPStorageProvider#loadUsersByUniqueAttribute} and {@link LDAPStorageProvider#loadUsersByDNs}
 * apply {@code firstResult}/{@code maxResults} to the underlying LDAP entries <em>before</em> importing them, not
 * after.
 * <p>
 * This matters because import (via {@code importUserFromLDAP}) is not a cheap, side-effect-free lookup: for a
 * brand-new entry it creates a local user, runs User Profile validation against it, and may remove it again. These
 * methods back paginated LDAP group/role membership resolution, where the candidate collection can be an entire
 * group's membership - so importing every entry up to the end of the requested page on every single page request,
 * instead of only the page's own entries, would multiply that cost across every page fetched.
 * <p>
 * {@code Stream.skip(n)} must pull (and therefore map) the first {@code n} elements in order to skip them, so this
 * is only guarded by keeping {@code .skip()}/{@code .limit()} <em>before</em> the {@code .map(this::importUserFromLDAP)}
 * call in the provider - not an incidental detail of how the stream happens to be written today.
 */
@KeycloakIntegrationTest
public class LDAPPaginationTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private static LDAPEmbeddedServer ldapEmbeddedServer;
    private static String ldapModelId;

    @TestSetup
    public void startLdapServerAndCreateProvider() throws Exception {
        int ldapPort = startLdapEmbeddedServer();

        ComponentRepresentation ldapRep = new ComponentRepresentation();
        ldapRep.setName("test-ldap");
        ldapRep.setProviderId(LDAPConstants.LDAP_PROVIDER);
        ldapRep.setProviderType(UserStorageProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(LDAPConstants.CONNECTION_URL, "ldap://localhost:" + ldapPort);
        config.putSingle(LDAPConstants.BASE_DN, "dc=keycloak,dc=org");
        config.putSingle(LDAPConstants.USERS_DN, "ou=People,dc=keycloak,dc=org");
        config.putSingle(LDAPConstants.BIND_DN, "uid=admin,ou=system");
        config.putSingle(LDAPConstants.BIND_CREDENTIAL, "secret");
        config.putSingle(LDAPConstants.VENDOR, LDAPConstants.VENDOR_OTHER);
        config.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.name());
        config.putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
        config.putSingle(LDAPConstants.CONNECTION_POOLING, "true");
        config.putSingle(LDAPConstants.PAGINATION, "true");
        config.putSingle(LDAPConstants.BATCH_SIZE_FOR_SYNC, "3");
        ldapRep.setConfig(config);

        try (Response response = managedRealm.admin().components().add(ldapRep)) {
            Assertions.assertEquals(201, response.getStatus(), "Failed to create the LDAP provider");
            ldapModelId = ApiUtil.getCreatedId(response);
        }
    }

    @TestCleanup
    public void stopLdapServer() throws Exception {
        Exception failure = null;

        if (ldapModelId != null) {
            try {
                managedRealm.admin().components().component(ldapModelId).remove();
            } catch (Exception e) {
                failure = e;
            }
            ldapModelId = null;
        }

        if (ldapEmbeddedServer != null) {
            try {
                ldapEmbeddedServer.stop();
            } catch (Exception e) {
                if (failure != null) {
                    failure.addSuppressed(e);
                } else {
                    failure = e;
                }
            }
            ldapEmbeddedServer = null;
        }

        if (failure != null) {
            throw failure;
        }
    }

    @AfterEach
    public void removeLdapUsers() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapProvider, realm);
        });
    }

    @Test
    public void testLoadUsersByUniqueAttributeImportsOnlyRequestedPage() {
        final int totalUsers = 10;
        final int firstResult = 5;
        final int maxResults = 3;

        int before = managedRealm.admin().users().count();

        int returned = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            List<String> usernames = new ArrayList<>();
            for (int i = 0; i < totalUsers; i++) {
                String username = "uniqueattrpageuser" + i;
                LDAPTestUtils.addLDAPUser(ldapProvider, realm, username, "First" + i, "Last" + i,
                        username + "@example.org", null, "4578");
                usernames.add(username);
            }

            try (Stream<UserModel> stream = ldapProvider.loadUsersByUniqueAttribute(
                    realm, LDAPConstants.UID, usernames, firstResult, maxResults)) {
                return (int) stream.collect(Collectors.toList()).size();
            }
        }, Integer.class);

        int after = managedRealm.admin().users().count();

        Assertions.assertEquals(maxResults, returned,
                "The method should return exactly maxResults users for a full page.");
        Assertions.assertEquals(maxResults, after - before,
                "Only maxResults entries should have been imported into the local database - pagination "
                        + "must be applied to the LDAP entries before the (expensive) import step, not after.");
    }

    @Test
    public void testLoadUsersByDNsImportsOnlyRequestedPage() {
        final int totalUsers = 10;
        final int firstResult = 5;
        final int maxResults = 3;

        int before = managedRealm.admin().users().count();

        int returned = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            List<LDAPDn> dns = new ArrayList<>();
            for (int i = 0; i < totalUsers; i++) {
                String username = "dnpageuser" + i;
                LDAPObject ldapUser = LDAPTestUtils.addLDAPUser(ldapProvider, realm, username, "First" + i,
                        "Last" + i, username + "@example.org", null, "4578");
                dns.add(ldapUser.getDn());
            }

            try (Stream<UserModel> stream = ldapProvider.loadUsersByDNs(realm, dns, firstResult, maxResults)) {
                return (int) stream.collect(Collectors.toList()).size();
            }
        }, Integer.class);

        int after = managedRealm.admin().users().count();

        Assertions.assertEquals(maxResults, returned,
                "The method should return exactly maxResults users for a full page.");
        Assertions.assertEquals(maxResults, after - before,
                "Only maxResults entries should have been imported into the local database - pagination "
                        + "must be applied to the LDAP entries before the (expensive) import step, not after.");
    }

    private static final int MAX_LDAP_BIND_ATTEMPTS = 5;

    private static int startLdapEmbeddedServer() throws Exception {
        for (int attempt = 1; ; attempt++) {
            int port = findFreePort();

            Properties serverProperties = new Properties();
            serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
            serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "false");
            serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, String.valueOf(port));

            LDAPEmbeddedServer server = new LDAPEmbeddedServer(serverProperties);
            server.init();
            try {
                server.start();
                ldapEmbeddedServer = server;
                return port;
            } catch (LdapConfigurationException e) {
                try {
                    server.stop();
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }
                if (attempt >= MAX_LDAP_BIND_ATTEMPTS || !isBindConflict(e)) {
                    throw e;
                }
            }
        }
    }

    private static boolean isBindConflict(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof BindException) {
                return true;
            }
        }
        return false;
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
