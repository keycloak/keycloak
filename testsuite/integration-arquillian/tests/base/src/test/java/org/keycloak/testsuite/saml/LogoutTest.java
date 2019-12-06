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

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderCreator;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.SamlClientBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.transform.dom.DOMSource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.*;
import static org.keycloak.testsuite.util.SamlClient.Binding.*;

/**
 *
 * @author hmlnarik
 */
public class LogoutTest extends AbstractSamlTest {

    private static final String SP_PROVIDED_ID = "spProvidedId";
    private static final String SP_NAME_QUALIFIER = "spNameQualifier";
    private static final String NAME_QUALIFIER = "nameQualifier";

    private static final String BROKER_SIGN_ON_SERVICE_URL = "http://saml.idp/saml";
    private static final String BROKER_LOGOUT_SERVICE_URL = "http://saml.idp/SLO/saml";
    private static final String BROKER_SERVICE_ID = "http://saml.idp/saml";

    private ClientRepresentation salesRep;
    private ClientRepresentation sales2Rep;

    private final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();
    private final AtomicReference<String> sessionIndexRef = new AtomicReference<>();

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

        nameIdRef.set(null);
        sessionIndexRef.set(null);

        adminClient.realm(REALM_NAME).clearEvents();
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    private SAML2Object extractNameIdAndSessionIndexAndTerminate(SAML2Object so) {
        assertThat(so, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType loginResp1 = (ResponseType) so;
        final AssertionType firstAssertion = loginResp1.getAssertions().get(0).getAssertion();
        assertThat(firstAssertion, org.hamcrest.Matchers.notNullValue());
        assertThat(firstAssertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

        NameIDType nameId = (NameIDType) firstAssertion.getSubject().getSubType().getBaseID();
        AuthnStatementType firstAssertionStatement = (AuthnStatementType) firstAssertion.getStatements().iterator().next();

        nameIdRef.set(nameId);
        sessionIndexRef.set(firstAssertionStatement.getSessionIndex());

        return null;
    }

    private SamlClientBuilder prepareLogIntoTwoApps() {
        return new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()
          .login().user(bburkeUser).build()
          .processSamlResponse(POST)
            .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
            .build()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, SAML_ASSERTION_CONSUMER_URL_SALES_POST2, POST).build()
          .login().sso(true).build()    // This is a formal step
          .processSamlResponse(POST).transformObject(so -> {
            assertThat(so, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
            return null;    // Do not follow the redirect to the app from the returned response
          }).build();
    }

    @Test
    public void testLogoutDifferentBrowser() {
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

        SAMLDocumentHolder samlResponse = prepareLogIntoTwoApps()
          .clearCookies()

          .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, POST)
            .nameId(nameIdRef::get)
            .sessionIndex(sessionIndexRef::get)
            .build()

          .getSamlResponse(POST);

        assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testFrontchannelLogoutInSameBrowser() {
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .build());

        SAMLDocumentHolder samlResponse = prepareLogIntoTwoApps()
          .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, POST)
            .nameId(nameIdRef::get)
            .sessionIndex(sessionIndexRef::get)
            .build()

          .getSamlResponse(POST);

        assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertLogoutEvent(SAML_CLIENT_ID_SALES_POST);
    }

    @Test
    public void testFrontchannelLogoutNoLogoutServiceUrlSetInSameBrowser() {
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "")
            .build());

        SAMLDocumentHolder samlResponse = prepareLogIntoTwoApps()
          .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, POST)
            .nameId(nameIdRef::get)
            .sessionIndex(sessionIndexRef::get)
            .build()

          .getSamlResponse(POST);

        assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testFrontchannelLogoutDifferentBrowser() {
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .build());

        SAMLDocumentHolder samlResponse = prepareLogIntoTwoApps()
          .clearCookies()

          .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, POST)
            .nameId(nameIdRef::get)
            .sessionIndex(sessionIndexRef::get)
            .build()

          .getSamlResponse(POST);

        assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testFrontchannelLogoutWithRedirectUrlDifferentBrowser() {
        adminClient.realm(REALM_NAME)
          .clients().get(salesRep.getId())
          .update(ClientBuilder.edit(salesRep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url")
            .build());

        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "")
            .build());

        SAMLDocumentHolder samlResponse = prepareLogIntoTwoApps()
          .clearCookies()

          .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, REDIRECT)
            .nameId(nameIdRef::get)
            .sessionIndex(sessionIndexRef::get)
            .build()

          .getSamlResponse(REDIRECT);

        assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testLogoutWithPostBindingUnsetRedirectBindingSet() {
        // https://issues.jboss.org/browse/KEYCLOAK-4779
        adminClient.realm(REALM_NAME)
          .clients().get(sales2Rep.getId())
          .update(ClientBuilder.edit(sales2Rep)
            .frontchannelLogout(true)
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "")
            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url-to-sales-2")
            .build());

        SAMLDocumentHolder samlResponse = prepareLogIntoTwoApps()
          .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, POST)
            .nameId(nameIdRef::get)
            .sessionIndex(sessionIndexRef::get)
            .build()

          .processSamlResponse(REDIRECT)
            .transformDocument(doc -> {
              // Expect logout request for sales-post2
              SAML2Object so = (SAML2Object) SAMLParser.getInstance().parse(new DOMSource(doc));
              assertThat(so, isSamlLogoutRequest("http://url-to-sales-2"));

              // Emulate successful logout response from sales-post2 logout
              return new SAML2LogoutResponseBuilder()
                .destination(getAuthServerSamlEndpoint(REALM_NAME).toString())
                .issuer(SAML_CLIENT_ID_SALES_POST2)
                .logoutRequestID(((LogoutRequestType) so).getID())
                .buildDocument();
            })
            .targetAttributeSamlResponse()
            .targetUri(getAuthServerSamlEndpoint(REALM_NAME))
            .build()

          .getSamlResponse(POST);

        // Expect final successful logout response from auth server signalling final successful logout
        assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        assertThat(((StatusResponseType) samlResponse.getSamlObject()).getDestination(), is("http://url"));
        assertLogoutEvent(SAML_CLIENT_ID_SALES_POST2);
    }

    private void assertLogoutEvent(String clientId) {
        List<EventRepresentation> logoutEvents = adminClient.realm(REALM_NAME)
                .getEvents(Arrays.asList(EventType.LOGOUT.name()), clientId, null, null, null, null, null, null);

        assertFalse(logoutEvents.isEmpty());
        assertEquals(1, logoutEvents.size());

        EventRepresentation logoutEvent = logoutEvents.get(0);

        assertEquals("http://url", logoutEvent.getDetails().get(Details.REDIRECT_URI));
        assertEquals(bburkeUser.getUsername(), logoutEvent.getDetails().get(Details.USERNAME));
        assertEquals(SamlProtocol.SAML_POST_BINDING, logoutEvent.getDetails().get(Details.RESPONSE_MODE));
        assertEquals("saml", logoutEvent.getDetails().get(Details.AUTH_METHOD));
        assertNotNull(logoutEvent.getDetails().get(SamlProtocol.SAML_LOGOUT_REQUEST_ID));
    }

    private IdentityProviderRepresentation addIdentityProvider() {
        IdentityProviderRepresentation identityProvider = IdentityProviderBuilder.create()
          .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
          .alias(SAML_BROKER_ALIAS)
          .displayName("SAML")
          .setAttribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, BROKER_SIGN_ON_SERVICE_URL)
          .setAttribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL, BROKER_LOGOUT_SERVICE_URL)
          .setAttribute(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress")
          .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "false")
          .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false")
          .setAttribute(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, "false")
          .build();
        return identityProvider;
    }

    private SAML2Object createAuthnResponse(SAML2Object so) {
        AuthnRequestType req = (AuthnRequestType) so;
        try {
            final ResponseType res = new SAML2LoginResponseBuilder()
              .requestID(req.getID())
              .destination(req.getAssertionConsumerServiceURL().toString())
              .issuer(BROKER_SERVICE_ID)
              .assertionExpiration(1000000)
              .subjectExpiration(1000000)
              .requestIssuer(getAuthServerRealmBase(REALM_NAME).toString())
              .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), "a@b.c")
              .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get())
              .sessionIndex("idp:" + UUID.randomUUID())
              .buildModel();

            NameIDType nameId = (NameIDType) res.getAssertions().get(0).getAssertion().getSubject().getSubType().getBaseID();
            nameId.setNameQualifier(NAME_QUALIFIER);
            nameId.setSPNameQualifier(SP_NAME_QUALIFIER);
            nameId.setSPProvidedID(SP_PROVIDED_ID);

            return res;
        } catch (ConfigurationException | ProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private SAML2Object createIdPLogoutResponse(SAML2Object so) {
        LogoutRequestType req = (LogoutRequestType) so;
        try {
            return new SAML2LogoutResponseBuilder()
              .logoutRequestID(req.getID())
              .destination(getSamlBrokerUrl(REALM_NAME).toString())
              .issuer(BROKER_SERVICE_ID)
              .buildModel();
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testLogoutPropagatesToSamlIdentityProvider() throws IOException {
        final RealmResource realm = adminClient.realm(REALM_NAME);

        try (
          Closeable sales = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
          .setFrontchannelLogout(true)
          .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE)
          .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url")
          .update();

          Closeable idp = new IdentityProviderCreator(realm, addIdentityProvider())
          ) {
            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()

              // Virtually perform login at IdP (return artificial SAML response)
              .login().idp(SAML_BROKER_ALIAS).build()
              .processSamlResponse(REDIRECT)
                .transformObject(this::createAuthnResponse)
                .targetAttributeSamlResponse()
                .targetUri(getSamlBrokerUrl(REALM_NAME))
                .build()
              .updateProfile().username("a").email("a@b.c").firstName("A").lastName("B").build()
              .followOneRedirect()

              // Now returning back to the app
              .processSamlResponse(POST)
                .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                .build()

              // ----- Logout phase ------

              // Logout initiated from the app
              .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, REDIRECT)
                .nameId(nameIdRef::get)
                .sessionIndex(sessionIndexRef::get)
                .build()

              // Should redirect now to logout from IdP
              .processSamlResponse(REDIRECT)
                .transformObject(this::createIdPLogoutResponse)
                .targetAttributeSamlResponse()
                .targetUri(getSamlBrokerUrl(REALM_NAME))
                .build()

              .getSamlResponse(REDIRECT);

            assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        }
    }

    @Test
    public void testLogoutPropagatesToSamlIdentityProviderNameIdPreserved() throws IOException {
        final RealmResource realm = adminClient.realm(REALM_NAME);

        try (
          Closeable sales = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
          .setFrontchannelLogout(true)
          .removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE)
          .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url")
          .update();

          Closeable idp = new IdentityProviderCreator(realm, addIdentityProvider())
          ) {
            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()

              // Virtually perform login at IdP (return artificial SAML response)
              .login().idp(SAML_BROKER_ALIAS).build()
              .processSamlResponse(REDIRECT)
                .transformObject(this::createAuthnResponse)
                .targetAttributeSamlResponse()
                .targetUri(getSamlBrokerUrl(REALM_NAME))
                .build()
              .updateProfile().username("a").email("a@b.c").firstName("A").lastName("B").build()
              .followOneRedirect()

              // Now returning back to the app
              .processSamlResponse(POST)
                .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                .build()

              // ----- Logout phase ------

              // Logout initiated from the app
              .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, REDIRECT)
                .nameId(nameIdRef::get)
                .sessionIndex(sessionIndexRef::get)
                .build()

              .getSamlResponse(REDIRECT);

            assertThat(samlResponse.getSamlObject(), isSamlLogoutRequest(BROKER_LOGOUT_SERVICE_URL));
            LogoutRequestType lr = (LogoutRequestType) samlResponse.getSamlObject();
            NameIDType logoutRequestNameID = lr.getNameID();
            assertThat(logoutRequestNameID.getFormat(), is(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.getUri()));
            assertThat(logoutRequestNameID.getValue(), is("a@b.c"));
            assertThat(logoutRequestNameID.getNameQualifier(), is(NAME_QUALIFIER));
            assertThat(logoutRequestNameID.getSPProvidedID(), is(SP_PROVIDED_ID));
            assertThat(logoutRequestNameID.getSPNameQualifier(), is(SP_NAME_QUALIFIER));
        }
    }

}
