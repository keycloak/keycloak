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

import org.jboss.resteasy.util.BasicAuthHelper;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.TokenService;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.adapter.CustomerDatabaseServlet;
import org.keycloak.testsuite.adapter.CustomerServlet;
import org.keycloak.testsuite.adapter.ProductServlet;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testutils.KeycloakServer;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.PublicKey;
import java.util.Map;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class AdminAPITest {

    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(RealmManager manager, RealmModel adminRealm) {


        }
    };

    private static String createToken() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminRealm = manager.getRealm(Config.getAdminRealm());
            ApplicationModel adminConsole = adminRealm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
            TokenManager tm = new TokenManager();
            UserModel admin = adminRealm.getUser("admin");
            UserSessionModel userSession = adminRealm.createUserSession(admin, null);
            AccessToken token = tm.createClientAccessToken(null, adminRealm, adminConsole, admin, userSession);
            return tm.encodeToken(adminRealm, token);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    protected void testCreateRealm(RealmRepresentation rep) {
        String token = createToken();
        final String authHeader = "Bearer " + token;
        ClientRequestFilter authFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
            }
        };
        Client client = ClientBuilder.newBuilder().register(authFilter).build();
        UriBuilder authBase = UriBuilder.fromUri("http://localhost:8081/auth");
        WebTarget adminRealms = client.target(AdminRoot.realmsUrl(authBase));
        String realmName = rep.getRealm();


        // create with just name, enabled, and id, just like admin console
        RealmRepresentation newRep = new RealmRepresentation();
        newRep.setRealm(rep.getRealm());
        newRep.setEnabled(rep.isEnabled());
        {
            Response response = adminRealms.request().post(Entity.json(newRep));
            Assert.assertEquals(201, response.getStatus());
            response.close();
        }





        // delete realm
        {
            Response response = adminRealms.path(realmName).request().delete();
            Assert.assertEquals(204, response.getStatus());
            response.close();

        }
    }

    protected void testCreateRealm(String path) {
        RealmRepresentation rep = KeycloakServer.loadJson(getClass().getResourceAsStream(path), RealmRepresentation.class);
        Assert.assertNotNull(rep);
        testCreateRealm(rep);
    }

    @Test
    public void testAdminApi() {
        testCreateRealm("/admin-test/testrealm.json");
    }

}
