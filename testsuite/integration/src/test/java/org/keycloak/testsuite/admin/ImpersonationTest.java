/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.rule.KeycloakRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.Map;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class ImpersonationTest {

    static String impersonatedUserId;

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule( new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            impersonatedUserId = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm).getId();
            {
                UserModel masterImpersonator = manager.getSession().users().addUser(adminstrationRealm, "master-impersonator");
                masterImpersonator.setEnabled(true);
                ClientModel adminRealmClient = adminstrationRealm.getClientByClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(appRealm));
                RoleModel masterImpersonatorRole = adminRealmClient.getRole(ImpersonationConstants.IMPERSONATION_ROLE);
                masterImpersonator.grantRole(masterImpersonatorRole);
            }
            {
                UserModel masterBadImpersonator = manager.getSession().users().addUser(adminstrationRealm, "master-bad-impersonator");
                masterBadImpersonator.setEnabled(true);
            }

            {
                UserModel impersonator = manager.getSession().users().addUser(appRealm, "impersonator");
                impersonator.setEnabled(true);
                ClientModel appRealmClient = appRealm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
                RoleModel realmImpersonatorRole = appRealmClient.getRole(ImpersonationConstants.IMPERSONATION_ROLE);
                impersonator.grantRole(realmImpersonatorRole);
            }
            {
                UserModel impersonator = manager.getSession().users().addUser(appRealm, "realm-admin");
                impersonator.setEnabled(true);
                ClientModel appRealmClient = appRealm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
                RoleModel realmImpersonatorRole = appRealmClient.getRole(AdminRoles.REALM_ADMIN);
                impersonator.grantRole(realmImpersonatorRole);
            }
            {
                UserModel badimpersonator = manager.getSession().users().addUser(appRealm, "bad-impersonator");
                badimpersonator.setEnabled(true);
            }
        }
    });

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);


    private static String createAdminToken(String username, String realm) {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminRealm = manager.getRealm(realm);
            ClientModel adminConsole = adminRealm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
            TokenManager tm = new TokenManager();
            UserModel admin = session.users().getUserByUsername(username, adminRealm);
            ClientSessionModel clientSession = session.sessions().createClientSession(adminRealm, adminConsole);
            clientSession.setNote(OIDCLoginProtocol.ISSUER, "http://localhost:8081/auth/realms/" + realm);
            UserSessionModel userSession = session.sessions().createUserSession(adminRealm, admin, "admin", null, "form", false, null, null);
            AccessToken token = tm.createClientAccessToken(session, tm.getAccess(null, adminConsole, admin), adminRealm, adminConsole, admin, userSession, clientSession);
            return tm.encodeToken(adminRealm, token);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void testImpersonateByMasterAdmin() {
        // test that composite is set up right for impersonation role
        testSuccessfulImpersonation("admin", Config.getAdminRealm());
    }

    @Test
    public void testImpersonateByMasterImpersonator() {
        testSuccessfulImpersonation("master-impersonator", Config.getAdminRealm());
    }

    @Test
    public void testImpersonateByTestImpersonator() {
        testSuccessfulImpersonation("impersonator", "test");
    }

    @Test
    public void testImpersonateByTestAdmin() {
        // test that composite is set up right for impersonation role
        testSuccessfulImpersonation("realm-admin", "test");
    }

    @Test
    public void testImpersonateByTestBadImpersonator() {
        testForbiddenImpersonation("bad-impersonator", "test");
    }

    @Test
    public void testImpersonateByMastertBadImpersonator() {
        testForbiddenImpersonation("master-bad-impersonator", Config.getAdminRealm());
    }



    protected void testSuccessfulImpersonation(String admin, String adminRealm) {
        Client client = createClient(admin, adminRealm);
        WebTarget impersonate = createImpersonateTarget(client);
        Map data = impersonate.request().post(null, Map.class);
        Assert.assertNotNull(data);
        Assert.assertNotNull(data.get("redirect"));

        events.expect(EventType.IMPERSONATE)
                .session(AssertEvents.isUUID())
                .user(impersonatedUserId)
                .detail(Details.IMPERSONATOR, admin)
                .detail(Details.IMPERSONATOR_REALM, adminRealm)
                .client((String) null).assertEvent();

        client.close();
    }

    protected void testForbiddenImpersonation(String admin, String adminRealm) {
        Client client = createClient(admin, adminRealm);
        WebTarget impersonate = createImpersonateTarget(client);
        Response response = impersonate.request().post(null);
        response.close();
        client.close();
    }


    protected WebTarget createImpersonateTarget(Client client) {
        UriBuilder authBase = UriBuilder.fromUri("http://localhost:8081/auth");
        WebTarget adminRealms = client.target(AdminRoot.realmsUrl(authBase));
        WebTarget realmTarget = adminRealms.path("test");
        return realmTarget.path("users").path(impersonatedUserId).path("impersonation");
    }

    protected Client createClient(String admin, String adminRealm) {
        String token = createAdminToken(admin, adminRealm);
        final String authHeader = "Bearer " + token;
        ClientRequestFilter authFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
            }
        };
        return ClientBuilder.newBuilder().register(authFilter).build();
    }

}
