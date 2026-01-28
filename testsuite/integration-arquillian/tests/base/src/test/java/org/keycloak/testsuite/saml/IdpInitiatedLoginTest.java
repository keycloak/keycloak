/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.saml;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderCreator;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.Test;

import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 *
 * @author hmlnarik
 */
public class IdpInitiatedLoginTest extends AbstractSamlTest {

    @Test
    public void testIdpInitiatedLoginPost() {
        new SamlClientBuilder()
          .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
          .login().user(bburkeUser).build()
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST));
              return null;
            })
            .build()
          .execute()
        ;
    }

    @Test
    public void testIdpInitiatedLoginPostAdminUrl() throws IOException {
        String url = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0)
                .getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
        try (Closeable c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                .setAdminUrl(url)
                .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, null)
                .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, null)
                .update()) {
            new SamlClientBuilder()
                    .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(Binding.POST)
                    .transformObject(ob -> {
                        assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                        ResponseType resp = (ResponseType) ob;
                        assertThat(resp.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST));
                        return null;
                    })
                    .build()
                    .execute();
        }
    }

    @Test
    public void testIdpInitiatedLoginRedirect() throws IOException {
        String url = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0)
                .getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
        try (Closeable c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, null)
                .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, url)
                .update()) {
            new SamlClientBuilder()
                    .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(Binding.REDIRECT)
                    .transformObject(ob -> {
                        assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                        ResponseType resp = (ResponseType) ob;
                        assertThat(resp.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST));
                        return null;
                    })
                    .build()
                    .execute();
        }
    }

    @Test
    public void testTwoConsequentIdpInitiatedLogins() {
        new SamlClientBuilder()
          .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
          .login().user(bburkeUser).build()
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST));
              return null;
            })
            .build()

          .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post2").build()
          .login().sso(true).build()
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST2));
              return null;
            })
            .build()

          .execute()
        ;

        final UsersResource users = adminClient.realm(REALM_NAME).users();
        final ClientsResource clients = adminClient.realm(REALM_NAME).clients();

        UserRepresentation bburkeUserRepresentation = users
          .search(bburkeUser.getUsername()).stream()
          .findFirst().get();

        List<UserSessionRepresentation> userSessions = users.get(bburkeUserRepresentation.getId()).getUserSessions();
        assertThat(userSessions, hasSize(1));
        Map<String, String> clientSessions = userSessions.get(0).getClients();

        Set<String> clientIds = clientSessions.values().stream()
          .flatMap(c -> clients.findByClientId(c).stream())
          .map(ClientRepresentation::getClientId)
          .collect(Collectors.toSet());

        assertThat(clientIds, containsInAnyOrder(SAML_CLIENT_ID_SALES_POST, SAML_CLIENT_ID_SALES_POST2));

    }

    @Test
    public void testIdpInitiatedLoginWithOIDCClient() {
        ClientRepresentation clientRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        adminClient.realm(REALM_NAME).clients().get(clientRep.getId()).update(ClientBuilder.edit(clientRep)
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL).build());

        new SamlClientBuilder()
                .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                .execute(r -> {
                    assertThat(r, statusCodeIsHC(Response.Status.BAD_REQUEST));
                    assertThat(r, bodyHC(containsString("Wrong client protocol.")));
                });


        adminClient.realm(REALM_NAME).clients().get(clientRep.getId()).update(ClientBuilder.edit(clientRep)
                .protocol(SamlProtocol.LOGIN_PROTOCOL).build());
    }

    @Test
    public void testSamlPostBindingPageLogin() {
        new SamlClientBuilder()
                .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                .login().user(bburkeUser).build()
                .execute(r -> {
                    assertThat(r, statusCodeIsHC(Response.Status.OK));
                    assertThat(r, bodyHC(allOf(
                            anyOf(
                              containsString("Redirecting, please wait."),
                              containsString("Authentication Redirect")
                            ),
                            containsString("<input type=\"hidden\" name=\"SAMLResponse\""), 
                            containsString(" id=\"kc-page-title\"")
                    )));
                });
    }

    @Test
    public void testSamlPostBindingPageIdP() throws Exception {
        try (IdentityProviderCreator idp = new IdentityProviderCreator(adminClient.realm(REALM_NAME), 
                IdentityProviderBuilder.create()
                    .alias("saml-idp")
                    .providerId("saml")
                    .setAttribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, "https://saml-idp-sso-service/")
                    .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "true")
                    .build())) {
            new SamlClientBuilder()
                .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                .login().idp("saml-idp").build()
                .execute(r -> {
                    assertThat(r, statusCodeIsHC(Response.Status.OK));
                    assertThat(r, bodyHC(allOf(
                            anyOf(
                              containsString("Redirecting, please wait."),
                              containsString("Authentication Redirect")
                            ),
                            containsString("Redirecting, please wait."),
                            containsString("<input type=\"hidden\" name=\"SAMLRequest\""), 
                            containsString(" id=\"kc-page-title\"")
                    )));
                });
        }
    }
}
