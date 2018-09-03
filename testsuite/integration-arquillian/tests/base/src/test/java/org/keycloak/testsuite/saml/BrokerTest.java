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

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.broker.IdpReviewProfileAuthenticatorFactory;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.updaters.IdentityProviderCreator;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_NAME;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_ASSERTION_CONSUMER_URL_SALES_POST;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_CLIENT_ID_SALES_POST;
import static org.keycloak.testsuite.util.Matchers.isSamlStatusResponse;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.REDIRECT;

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
          .setAttribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL, samlEndpoint)
          .setAttribute(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())
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
              .issuer("http://saml.idp/saml")
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
        String firstBrokerLoginFlowAlias = null;
        try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, addIdentityProvider("http://saml.idp/saml"))) {
            IdentityProviderRepresentation idpRepresentation = idp.identityProvider().toRepresentation();
            firstBrokerLoginFlowAlias = idpRepresentation.getFirstBrokerLoginFlowAlias();
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
    public void testRedirectQueryParametersPreserved() throws IOException {
        final RealmResource realm = adminClient.realm(REALM_NAME);

        try (IdentityProviderCreator idp = new IdentityProviderCreator(realm, addIdentityProvider("http://saml.idp/?service=name&serviceType=prod"))) {
            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()
              .login().idp(SAML_BROKER_ALIAS).build()

              // Virtually perform login at IdP (return artificial SAML response)
              .getSamlResponse(REDIRECT);

            assertThat(samlResponse.getSamlObject(), Matchers.instanceOf(AuthnRequestType.class));
            AuthnRequestType ar = (AuthnRequestType) samlResponse.getSamlObject();
            assertThat(ar.getDestination(), Matchers.equalTo(URI.create("http://saml.idp/?service=name&serviceType=prod")));

            Header[] headers = new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST).build()
              .login().idp(SAML_BROKER_ALIAS).build()
              .doNotFollowRedirects()
              .executeAndTransform(resp -> resp.getHeaders(HttpHeaders.LOCATION));

            assertThat(headers.length, Matchers.is(1));
            assertThat(headers[0].getValue(), Matchers.containsString("http://saml.idp/?service=name&serviceType=prod"));
            assertThat(headers[0].getValue(), Matchers.containsString("SAMLRequest"));
        }
    }

}
