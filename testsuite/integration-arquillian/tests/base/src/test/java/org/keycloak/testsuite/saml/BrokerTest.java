/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.broker.IdpReviewProfileAuthenticatorFactory;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.parsers.saml.xmldsig.XmlDSigQNames;
import org.keycloak.saml.processing.core.parsers.util.HasQName;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.testsuite.updaters.IdentityProviderCreator;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.saml.SamlBackchannelArtifactResolveReceiver;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.keycloak.saml.SignatureAlgorithm.RSA_SHA1;
import static org.keycloak.testsuite.util.Matchers.isSamlStatusResponse;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.REDIRECT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 *
 * @author hmlnarik
 */
public class BrokerTest extends AbstractSamlTest {

    private IdentityProviderRepresentation addIdentityProvider(String samlEndpoint) {
        IdentityProviderRepresentation identityProvider = IdentityProviderBuilder.create()
          .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
          .alias(SAML_BROKER_ALIAS)
          .displayName("SAML")
          .setAttribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, samlEndpoint)
          .setAttribute(SAMLIdentityProviderConfig.ARTIFACT_RESOLUTION_SERVICE_URL, samlEndpoint)
          .setAttribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL, samlEndpoint)
          .setAttribute(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())
          .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "false")
          .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false")
          .setAttribute(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, "false")
          .setAttribute(SAMLIdentityProviderConfig.ARTIFACT_BINDING_RESPONSE, "false")
          .build();
        return identityProvider;
    }

    private SAML2Object createAuthnResponse(SAML2Object so) {
        AuthnRequestType req = (AuthnRequestType) so;
        try {
            final ResponseType res = new SAML2LoginResponseBuilder()
              .requestID(req.getID())
              .destination(req.getAssertionConsumerServiceURL().toString())
              .issuer("https://saml.idp/saml")
              .assertionExpiration(1000000)
              .subjectExpiration(1000000)
              .requestIssuer(getAuthServerRealmBase(REALM_NAME).toString())
              .sessionIndex("idp:" + UUID.randomUUID())
              .buildModel();

            AttributeStatementType attrStatement = new AttributeStatementType();
            AttributeType attribute = new AttributeType("mail");
            attribute.addAttributeValue("v@w.x");
            attrStatement.addAttribute(new ASTChoiceType(attribute));

            res.getAssertions().get(0).getAssertion().addStatement(attrStatement);

            return res;
        } catch (ConfigurationException | ProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testLogoutPropagatesToSamlIdentityProvider() throws IOException {
        final RealmResource realm = adminClient.realm(REALM_NAME);
        final ClientsResource clients = realm.clients();

        AuthenticationExecutionInfoRepresentation reviewProfileAuthenticator = null;
        final String firstBrokerLoginFlowAlias = UUID.randomUUID().toString();

        realm.flows().copy(realm.toRepresentation().getFirstBrokerLoginFlow(), Map.of("newName", firstBrokerLoginFlowAlias)).close();

        final IdentityProviderRepresentation rep = addIdentityProvider("https://saml.idp/saml");
        rep.setFirstBrokerLoginFlowAlias(firstBrokerLoginFlowAlias);
        rep.getConfig().put(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, "undefined");
        rep.getConfig().put(SAMLIdentityProviderConfig.PRINCIPAL_TYPE, SamlPrincipalType.ATTRIBUTE.toString());
        rep.getConfig().put(SAMLIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, "mail");

        try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, rep)) {
            List<AuthenticationExecutionInfoRepresentation> executions = realm.flows().getExecutions(firstBrokerLoginFlowAlias);
            reviewProfileAuthenticator = executions.stream()
              .filter(ex -> Objects.equals(ex.getProviderId(), IdpReviewProfileAuthenticatorFactory.PROVIDER_ID))
              .findFirst()
              .orElseGet(() -> { Assert.fail("Could not find update profile in first broker login flow"); return null; });

            reviewProfileAuthenticator.setRequirement(Requirement.DISABLED.name());
            realm.flows().updateExecutions(firstBrokerLoginFlowAlias, reviewProfileAuthenticator);

            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
                .transformObject(ar -> {
                    NameIDPolicyType nameIDPolicy = new NameIDPolicyType();
                    nameIDPolicy.setAllowCreate(Boolean.TRUE);
                    nameIDPolicy.setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.getUri());

                    ar.setNameIDPolicy(nameIDPolicy);
                    return ar;
                })
                .build()

              .login().idp(SAML_BROKER_ALIAS).build()

              // Virtually perform login at IdP (return artificial SAML response)
              .processSamlResponse(REDIRECT)
                .transformObject(this::createAuthnResponse)
                .targetAttributeSamlResponse()
                .targetUri(getSamlBrokerUrl(REALM_NAME))
                .build()
              .followOneRedirect()  // first-broker-login
              .followOneRedirect()  // after-first-broker-login
              .getSamlResponse(POST);

            assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(
              JBossSAMLURIConstants.STATUS_RESPONDER,
              JBossSAMLURIConstants.STATUS_INVALID_NAMEIDPOLICY
            ));
        } finally {
            reviewProfileAuthenticator.setRequirement(Requirement.REQUIRED.name());
            realm.flows().updateExecutions(firstBrokerLoginFlowAlias, reviewProfileAuthenticator);
        }
    }

    @Test
    public void testInResponseToSetCorrectly() throws IOException {
        final RealmResource realm = adminClient.realm(REALM_NAME);

        try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, addIdentityProvider("https://saml.idp/saml"))) {
            AtomicReference<String> serviceProvidersId = new AtomicReference<>();
            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
                .transformObject(ar -> {
                    serviceProvidersId.set(ar.getID());
                    return ar;
                })
                .build()

              .login().idp(SAML_BROKER_ALIAS).build()

              // Virtually perform login at IdP (return artificial SAML response)
              .processSamlResponse(REDIRECT)
                .transformObject(this::createAuthnResponse)
                .targetAttributeSamlResponse()
                .targetUri(getSamlBrokerUrl(REALM_NAME))
                .build()
              .followOneRedirect()  // first-broker-login
              .updateProfile().username("userInResponseTo").email("f@g.h").firstName("a").lastName("b").build()
              .followOneRedirect()  // after-first-broker-login
              .getSamlResponse(POST);

            assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
            assertThat(((ResponseType) samlResponse.getSamlObject()).getInResponseTo(), is(serviceProvidersId.get()));
        } finally {
            clearUsers(realm);
        }
    }

    private void clearUsers(final RealmResource realm) {
        realm.users().list().stream()
          .map(UserRepresentation::getId)
          .map(realm.users()::get)
          .forEach(UserResource::remove);
    }

    @Test
    public void testNoNameIDAndPrincipalFromAttribute() throws IOException {
        final String userName = "newUser-" + UUID.randomUUID();
        final RealmResource realm = adminClient.realm(REALM_NAME);
        final IdentityProviderRepresentation rep = addIdentityProvider("https://saml.idp/");
        rep.getConfig().put(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, "undefined");
        rep.getConfig().put(SAMLIdentityProviderConfig.PRINCIPAL_TYPE, SamlPrincipalType.ATTRIBUTE.toString());
        rep.getConfig().put(SAMLIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, "user");

        try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, rep)) {
            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()
                    .login().idp(SAML_BROKER_ALIAS).build()
                    // Virtually perform login at IdP (return artificial SAML response)
                    .processSamlResponse(REDIRECT)
                    .transformObject(this::createAuthnResponse)
                    .transformObject(resp -> {
                        final ResponseType rt = (ResponseType) resp;

                        final AssertionType assertion = rt.getAssertions()
                                .get(0)
                                .getAssertion();

                        // Remove NameID from subject
                        assertion.getSubject()
                                .setSubType(null);

                        // Add attribute to get principal from
                        AttributeStatementType attrStatement = new AttributeStatementType();
                        AttributeType attribute = new AttributeType("user");
                        attribute.addAttributeValue(userName);
                        attrStatement.addAttribute(new ASTChoiceType(attribute));
                        rt.getAssertions().get(0).getAssertion().addStatement(attrStatement);

                        return rt;
                    })
                    .targetAttributeSamlResponse()
                    .targetUri(getSamlBrokerUrl(REALM_NAME))
                    .build()
                    .followOneRedirect()  // first-broker-login
                    .updateProfile()
                        .username(userName)
                        .firstName("someFirstName")
                        .lastName("someLastName")
                        .email("some@email.com")
                        .build()
                    .followOneRedirect() // redirect to client
                    .assertResponse(org.keycloak.testsuite.util.Matchers.statusCodeIsHC(200))
                    .execute();
        }

        final UserRepresentation userRepresentation = realm.users()
                .search(userName)
                .stream()
                .findFirst()
                .get();

        final List<UserSessionRepresentation> userSessions = realm.users()
                .get(userRepresentation.getId())
                .getUserSessions();
        assertThat(userSessions, hasSize(1));
    }

    @Test
    public void testRedirectQueryParametersPreserved() throws IOException {
        final RealmResource realm = adminClient.realm(REALM_NAME);

        try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, addIdentityProvider("https://saml.idp/?service=name&serviceType=prod"))) {
            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()
              .login().idp(SAML_BROKER_ALIAS).build()

              // Virtually perform login at IdP (return artificial SAML response)
              .getSamlResponse(REDIRECT);

            assertThat(samlResponse.getSamlObject(), Matchers.instanceOf(AuthnRequestType.class));
            AuthnRequestType ar = (AuthnRequestType) samlResponse.getSamlObject();
            assertThat(ar.getDestination(), Matchers.equalTo(URI.create("https://saml.idp/?service=name&serviceType=prod")));

            Header[] headers = new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()
              .login().idp(SAML_BROKER_ALIAS).build()
              .doNotFollowRedirects()
              .executeAndTransform(resp -> resp.getHeaders(HttpHeaders.LOCATION));

            assertThat(headers.length, Matchers.is(1));
            assertThat(headers[0].getValue(), Matchers.containsString("https://saml.idp/?service=name&serviceType=prod"));
            assertThat(headers[0].getValue(), Matchers.containsString("SAMLRequest"));
        }
    }

    private static final String XMLNS_VETINARI = "vetinari";
    private static final String NS_VETINARI = "urn:dw:am:havelock";

    private static Element appendNewElement(Element parent, QName qName, String prefix) throws DOMException {
        Document doc = parent.getOwnerDocument();
        final Element res = doc.createElementNS(qName.getNamespaceURI(), prefix + ":" + qName.getLocalPart());
        parent.appendChild(res);
        return res;
    }

    private static void signAndAddCustomNamespaceElementToSignature(Document doc) {
        doc.getDocumentElement().setAttribute("xmlns:" + XMLNS_VETINARI, NS_VETINARI);

        BaseSAML2BindingBuilder<BaseSAML2BindingBuilder> sb = new BaseSAML2BindingBuilder();
        try {
            KeyPair keyPair = new KeyPair(SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY_PK, SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY_PK);
            sb.signWith("kn", keyPair)
              .signatureAlgorithm(RSA_SHA1)
              .signAssertions()
              .signAssertion(doc);
        } catch (ProcessingException ex) {
            throw new RuntimeException(ex);
        }

        // KeyInfo has lax and can contain custom elements, see https://www.w3.org/TR/xmldsig-core1/#sec-KeyInfo
        Element el = findFirstElement(doc, XmlDSigQNames.KEY_INFO);
        appendNewElement(el, new QName(NS_VETINARI, "Patrician"), XMLNS_VETINARI);
    }

    private static Element findFirstElement(Document doc, HasQName qName) {
        NodeList nl = doc.getElementsByTagNameNS(qName.getQName().getNamespaceURI(), qName.getQName().getLocalPart());
        return (nl == null || nl.getLength() == 0) ? null : (Element) nl.item(0);
    }

    @Test
    public void testAnyNamespacePreservedInContext() throws IOException {
        final RealmResource realm = adminClient.realm(REALM_NAME);

        try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, addIdentityProvider("https://saml.idp/"))) {
            new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
                .build()
              .login().idp(SAML_BROKER_ALIAS).build()
              // Virtually perform login at IdP (return artificial SAML response)
              .processSamlResponse(REDIRECT)
                .transformObject(this::createAuthnResponse)
                .transformDocument(BrokerTest::signAndAddCustomNamespaceElementToSignature)
                .targetAttributeSamlResponse()
                .targetUri(getSamlBrokerUrl(REALM_NAME))
                .targetBinding(POST)
                .build()
              .assertResponse(org.keycloak.testsuite.util.Matchers.statusCodeIsHC(Status.OK))
              .execute();
        }
    }

    @Test
    public void testExpiredAssertion() throws Exception {
        XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();
        XMLGregorianCalendar notBeforeInPast = XMLTimeUtil.subtract(now, 60 * 60 * 1000);
        XMLGregorianCalendar notOnOrAfterInPast = XMLTimeUtil.subtract(now, 59 * 60 * 1000);
        XMLGregorianCalendar notBeforeInFuture = XMLTimeUtil.add(now, 59 * 60 * 1000);
        XMLGregorianCalendar notOnOrAfterInFuture = XMLTimeUtil.add(now, 60 * 60 * 1000);
        // Should not pass:
        assertExpired(notBeforeInPast, notOnOrAfterInPast, false);
        assertExpired(notBeforeInFuture, notOnOrAfterInPast, false);
        assertExpired(null, notOnOrAfterInPast, false);
        assertExpired(notBeforeInFuture, notOnOrAfterInFuture, false);
        assertExpired(notBeforeInFuture, null, false);
        // Should pass:
        assertExpired(notBeforeInPast, notOnOrAfterInFuture, true);
        assertExpired(notBeforeInPast, null, true);
        assertExpired(null, notOnOrAfterInFuture, true);
        assertExpired(null, null, true);
    }

    @Test(expected = AssertionError.class)
    public void testNonexpiredAssertionShouldFail() throws Exception {
        assertExpired(null, null, false);   // Expected result (false) is it should fail but it should pass and throw
    }

    @Test(expected = AssertionError.class)
    public void testExpiredAssertionShouldFail() throws Exception {
        XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();
        XMLGregorianCalendar notBeforeInPast = XMLTimeUtil.subtract(now, 60 * 60 * 1000);
        XMLGregorianCalendar notOnOrAfterInPast = XMLTimeUtil.subtract(now, 59 * 60 * 1000);
        assertExpired(notBeforeInPast, notOnOrAfterInPast, true);   // Expected result (true) is it should succeed but it should pass and throw
    }

    private void assertExpired(XMLGregorianCalendar notBefore, XMLGregorianCalendar notOnOrAfter, boolean shouldPass) throws Exception {
        Status expectedStatus = shouldPass ? Status.OK : Status.BAD_REQUEST;
        final RealmResource realm = adminClient.realm(REALM_NAME);
        try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, addIdentityProvider("https://saml.idp/"))) {
            new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()
              .login().idp(SAML_BROKER_ALIAS).build()
              // Virtually perform login at IdP (return artificial SAML response)
              .processSamlResponse(REDIRECT)
              .transformObject(this::createAuthnResponse)
              .transformObject(resp -> {  // always invent a new user identified by a different email address
                  ResponseType rt = (ResponseType) resp;
                  AssertionType a = rt.getAssertions().get(0).getAssertion();

                  NameIDType nameId = new NameIDType();
                  nameId.setFormat(URI.create(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get()));
                  nameId.setValue(UUID.randomUUID() + "@random.email.org");
                  SubjectType subject = new SubjectType();
                  SubjectType.STSubType subType = new SubjectType.STSubType();
                  subType.addBaseID(nameId);
                  subject.setSubType(subType);
                  a.setSubject(subject);

                  ConditionsType conditions = a.getConditions();
                  conditions.setNotBefore(notBefore);
                  conditions.setNotOnOrAfter(notOnOrAfter);
                  return rt;
              })
              .targetAttributeSamlResponse()
              .targetUri(getSamlBrokerUrl(REALM_NAME))
              .build()
              .assertResponse(org.keycloak.testsuite.util.Matchers.statusCodeIsHC(expectedStatus))
              .execute();
        }
    }

    @Test
    public void testResolveArtifactBindingAsSp() {
        RealmResource realm = adminClient.realm(REALM_NAME);

        try (SamlBackchannelArtifactResolveReceiver samlBackchannelArtifactResolveReceiver = new SamlBackchannelArtifactResolveReceiver(
                8082,
                realm.clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0)
        )) {

            IdentityProviderRepresentation rep = addIdentityProvider("https://saml.idp/saml");
            rep.getConfig().put(SAMLIdentityProviderConfig.ARTIFACT_RESOLUTION_SERVICE_URL, samlBackchannelArtifactResolveReceiver.getUrl());
            rep.getConfig().put(SAMLIdentityProviderConfig.ARTIFACT_BINDING_RESPONSE, "true");

            try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, rep)) {
                SamlClientBuilder samlClientBuilder = new SamlClientBuilder();

                // trigger authentication
                samlClientBuilder.authnRequest(
                        getAuthServerSamlEndpoint(REALM_NAME),
                        SAML_CLIENT_ID_SALES_POST,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST,
                        POST
                ).setProtocolBinding(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri()).build();

                // simulate login page interaction
                samlClientBuilder.login().idp(SAML_BROKER_ALIAS).build();

                // simulate IdP response (artifact as query param)
                samlClientBuilder.processSamlResponse(REDIRECT)
                        .targetAttributeSamlArtifact()
                        .targetUri(getSamlBrokerUrl(REALM_NAME))
                        .build();

                // assert the authentication succeeded
                samlClientBuilder.assertResponse(org.keycloak.testsuite.util.Matchers.statusCodeIsHC(Status.OK));

                samlClientBuilder.execute();
            }
        } catch (Exception ex) {
            fail("unexpected error");
        }
    }

}
