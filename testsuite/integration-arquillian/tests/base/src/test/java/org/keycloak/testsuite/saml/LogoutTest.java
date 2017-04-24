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

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.*;
import static org.keycloak.testsuite.util.SamlClient.Binding.*;

/**
 *
 * @author hmlnarik
 */
public class LogoutTest extends AbstractSamlTest {

    private ClientRepresentation salesRep;
    private ClientRepresentation sales2Rep;

    private SamlClient samlClient;

    @Before
    public void setup() {
        salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        sales2Rep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST2).get(0);

        adminClient.realm(REALM_NAME)
          .clients().get(salesRep.getId())
          .update(ClientBuilder.edit(salesRep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "http://url")
            .build());

        samlClient = new SamlClient(getAuthServerSamlEndpoint(REALM_NAME));
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    private Document prepareLogoutFromSalesAfterLoggingIntoTwoApps() throws ParsingException, IllegalArgumentException, UriBuilderException, ConfigurationException, ProcessingException {
        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, REALM_NAME);
        Document doc = SAML2Request.convert(loginRep);
        SAMLDocumentHolder resp = samlClient.login(bburkeUser, doc, null, POST, POST, false, true);
        assertThat(resp.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType loginResp1 = (ResponseType) resp.getSamlObject();

        loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST2, SAML_ASSERTION_CONSUMER_URL_SALES_POST2, REALM_NAME);
        doc = SAML2Request.convert(loginRep);
        resp = samlClient.subsequentLoginViaSSO(doc, null, POST, POST);
        assertThat(resp.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType loginResp2 = (ResponseType) resp.getSamlObject();

        AssertionType firstAssertion = loginResp1.getAssertions().get(0).getAssertion();
        assertThat(firstAssertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));
        NameIDType nameId = (NameIDType) firstAssertion.getSubject().getSubType().getBaseID();
        AuthnStatementType firstAssertionStatement = (AuthnStatementType) firstAssertion.getStatements().iterator().next();

        return new SAML2LogoutRequestBuilder()
          .destination(getAuthServerSamlEndpoint(REALM_NAME).toString())
          .issuer(SAML_CLIENT_ID_SALES_POST)
          .sessionIndex(firstAssertionStatement.getSessionIndex())
          .userPrincipal(nameId.getValue(), nameId.getFormat().toString())
          .buildDocument();
    }

    @Test
    public void testLogoutInSameBrowser() throws ParsingException, ConfigurationException, ProcessingException {
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(false)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE)
            .build());

        Document logoutDoc = prepareLogoutFromSalesAfterLoggingIntoTwoApps();

        samlClient.logout(logoutDoc, null, POST, POST);
    }

    @Test
    public void testLogoutDifferentBrowser() throws ParsingException, ConfigurationException, ProcessingException {
        // This is in fact the same as admin logging out a session from admin console.
        // This always succeeds as it is essentially the same as backend logout which
        // does not report errors to client but only to the server log
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(false)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE)
            .build());

        Document logoutDoc = prepareLogoutFromSalesAfterLoggingIntoTwoApps();

        samlClient.execute((client, context, strategy) -> {
            HttpUriRequest post = POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, logoutDoc);
            CloseableHttpResponse response = client.execute(post, HttpClientContext.create());
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            return response;
        });
    }

    @Test
    public void testFrontchannelLogoutInSameBrowser() throws ParsingException, ConfigurationException, ProcessingException {
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE)
            .build());

        Document logoutDoc = prepareLogoutFromSalesAfterLoggingIntoTwoApps();

        samlClient.execute((client, context, strategy) -> {
            HttpUriRequest post = POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, logoutDoc);
            CloseableHttpResponse response = client.execute(post, context);
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            return response;
        });
    }

    @Test
    public void testFrontchannelLogoutNoLogoutServiceUrlSetInSameBrowser() throws ParsingException, ConfigurationException, ProcessingException {
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE)
            .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE)
            .build());

        Document logoutDoc = prepareLogoutFromSalesAfterLoggingIntoTwoApps();

        samlClient.execute((client, context, strategy) -> {
            HttpUriRequest post = POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, logoutDoc);
            CloseableHttpResponse response = client.execute(post, context);
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            return response;
        });
    }

    @Test
    public void testFrontchannelLogoutDifferentBrowser() throws ParsingException, ConfigurationException, ProcessingException {
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE)
            .build());

        Document logoutDoc = prepareLogoutFromSalesAfterLoggingIntoTwoApps();

        samlClient.execute((client, context, strategy) -> {
            HttpUriRequest post = POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, logoutDoc);
            CloseableHttpResponse response = client.execute(post, HttpClientContext.create());
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            return response;
        });
    }

    @Test
    public void testFrontchannelLogoutWithRedirectUrlDifferentBrowser() throws ParsingException, ConfigurationException, ProcessingException {
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url")
            .build());

        Document logoutDoc = prepareLogoutFromSalesAfterLoggingIntoTwoApps();

        samlClient.execute((client, context, strategy) -> {
            HttpUriRequest post = POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, logoutDoc);
            CloseableHttpResponse response = client.execute(post, HttpClientContext.create());
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            return response;
        });
    }

    @Test
    public void testLogoutWithPostBindingUnsetRedirectBindingSet() throws ParsingException, ConfigurationException, ProcessingException {
        // https://issues.jboss.org/browse/KEYCLOAK-4779
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url")
            .build());

        Document logoutDoc = prepareLogoutFromSalesAfterLoggingIntoTwoApps();

        SAMLDocumentHolder resp = samlClient.getSamlResponse(REDIRECT, (client, context, strategy) -> {
            strategy.setRedirectable(false);
            HttpUriRequest post = POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, logoutDoc);
            return client.execute(post, context);
        });

        // Expect logout request for sales-post2
        assertThat(resp.getSamlObject(), isSamlLogoutRequest("http://url"));
        Document logoutRespDoc = new SAML2LogoutResponseBuilder()
          .destination(getAuthServerSamlEndpoint(REALM_NAME).toString())
          .issuer(SAML_CLIENT_ID_SALES_POST2)
          .logoutRequestID(((LogoutRequestType) resp.getSamlObject()).getID())
          .buildDocument();

        // Emulate successful logout response from sales-post2 logout
        resp = samlClient.getSamlResponse(POST, (client, context, strategy) -> {
            strategy.setRedirectable(false);
            HttpUriRequest post = POST.createSamlUnsignedResponse(getAuthServerSamlEndpoint(REALM_NAME), null, logoutRespDoc);
            return client.execute(post, context);
        });

        // Expect final successful logout response from auth server signalling final successful logout
        assertThat(resp.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

}
