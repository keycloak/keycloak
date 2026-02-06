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
package org.keycloak.testsuite.broker;
import java.io.Closeable;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for CVE-2026-1190: Validates that SubjectConfirmationData NotOnOrAfter
 * is properly validated when Keycloak acts as a SAML broker (SP).
 *
 * Per SAML 2.0 Core spec section 2.4.1.2, the NotOnOrAfter attribute specifies
 * a time instant at which the subject can no longer be confirmed.
 *
 * @author Niels Kaspers
 */
public class KcSamlBrokerSubjectConfirmationNotOnOrAfterTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    /**
     * Tests that a SAML response with an expired SubjectConfirmationData NotOnOrAfter
     * is rejected by the broker.
     */
    @Test
    public void testExpiredSubjectConfirmationNotOnOrAfterIsRejected() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(
                AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                getConsumerRoot() + "/sales-post/saml",
                null);

        Document doc = SAML2Request.convert(loginRep);

        new SamlClientBuilder()
            .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST).build()
            .login().idp(bc.getIDPAlias()).build()

            .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                .targetAttributeSamlRequest()
                .build()

            .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

            .processSamlResponse(SamlClient.Binding.POST)    // Response from producer IdP
                .transformObject(ob -> {
                    ResponseType resp = (ResponseType) ob;
                    AssertionType assertion = resp.getAssertions().get(0).getAssertion();

                    // Set SubjectConfirmationData NotOnOrAfter to a time in the past
                    if (assertion.getSubject() != null) {
                        List<SubjectConfirmationType> confirmations = assertion.getSubject().getConfirmation();
                        if (confirmations != null) {
                            for (SubjectConfirmationType confirmation : confirmations) {
                                SubjectConfirmationDataType confirmationData = confirmation.getSubjectConfirmationData();
                                if (confirmationData == null) {
                                    confirmationData = new SubjectConfirmationDataType();
                                    confirmation.setSubjectConfirmationData(confirmationData);
                                }
                                // Set NotOnOrAfter to 1 hour in the past
                                XMLGregorianCalendar expiredTime = XMLTimeUtil.add(
                                        XMLTimeUtil.getIssueInstant(),
                                        -3600 * 1000L);
                                confirmationData.setNotOnOrAfter(expiredTime);
                            }
                        }
                    }
                    return ob;
                })
                .build()
            .execute(hr -> assertThat(hr, statusCodeIsHC(Response.Status.BAD_REQUEST)));
    }

    /**
     * Tests that a SAML response with an expired SubjectConfirmationData NotOnOrAfter
     * is accepted when within the allowed clock skew tolerance.
     */
    @Test
    public void testExpiredSubjectConfirmationNotOnOrAfterWithinClockSkewIsAccepted() throws Exception {
        // Set clock skew to 120 seconds
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.ALLOWED_CLOCK_SKEW, "120")
                .update()) {

            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(
                    AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                    getConsumerRoot() + "/sales-post/saml",
                    null);

            Document doc = SAML2Request.convert(loginRep);

            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST).build()
                .login().idp(bc.getIDPAlias()).build()

                .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                    .targetAttributeSamlRequest()
                    .build()

                .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

                .processSamlResponse(SamlClient.Binding.POST)    // Response from producer IdP
                    .transformObject(ob -> {
                        ResponseType resp = (ResponseType) ob;
                        AssertionType assertion = resp.getAssertions().get(0).getAssertion();

                        // Set SubjectConfirmationData NotOnOrAfter to 30 seconds in the past
                        // (within the 120 second clock skew tolerance)
                        if (assertion.getSubject() != null) {
                            List<SubjectConfirmationType> confirmations = assertion.getSubject().getConfirmation();
                            if (confirmations != null) {
                                for (SubjectConfirmationType confirmation : confirmations) {
                                    SubjectConfirmationDataType confirmationData = confirmation.getSubjectConfirmationData();
                                    if (confirmationData == null) {
                                        confirmationData = new SubjectConfirmationDataType();
                                        confirmation.setSubjectConfirmationData(confirmationData);
                                    }
                                    // Set NotOnOrAfter to 30 seconds in the past (within 120s clock skew)
                                    XMLGregorianCalendar slightlyExpiredTime = XMLTimeUtil.add(
                                            XMLTimeUtil.getIssueInstant(),
                                            -30 * 1000L);
                                    confirmationData.setNotOnOrAfter(slightlyExpiredTime);
                                }
                            }
                        }
                        return ob;
                    })
                    .build()

                // first-broker flow
                .updateProfile().firstName("a").lastName("b").email(bc.getUserEmail()).username(bc.getUserLogin()).build()
                .followOneRedirect()

                .getSamlResponse(SamlClient.Binding.POST);

            assertThat(samlResponse, Matchers.notNullValue());
            assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        }
    }

    /**
     * Tests that a SAML response with a valid (future) SubjectConfirmationData NotOnOrAfter
     * is accepted.
     */
    @Test
    public void testValidSubjectConfirmationNotOnOrAfterIsAccepted() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(
                AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                getConsumerRoot() + "/sales-post/saml",
                null);

        Document doc = SAML2Request.convert(loginRep);

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
            .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST).build()
            .login().idp(bc.getIDPAlias()).build()

            .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                .targetAttributeSamlRequest()
                .build()

            .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

            .processSamlResponse(SamlClient.Binding.POST)    // Response from producer IdP
                .transformObject(ob -> {
                    ResponseType resp = (ResponseType) ob;
                    AssertionType assertion = resp.getAssertions().get(0).getAssertion();

                    // Set SubjectConfirmationData NotOnOrAfter to 1 hour in the future
                    if (assertion.getSubject() != null) {
                        List<SubjectConfirmationType> confirmations = assertion.getSubject().getConfirmation();
                        if (confirmations != null) {
                            for (SubjectConfirmationType confirmation : confirmations) {
                                SubjectConfirmationDataType confirmationData = confirmation.getSubjectConfirmationData();
                                if (confirmationData == null) {
                                    confirmationData = new SubjectConfirmationDataType();
                                    confirmation.setSubjectConfirmationData(confirmationData);
                                }
                                // Set NotOnOrAfter to 1 hour in the future
                                XMLGregorianCalendar futureTime = XMLTimeUtil.add(
                                        XMLTimeUtil.getIssueInstant(),
                                        3600 * 1000L);
                                confirmationData.setNotOnOrAfter(futureTime);
                            }
                        }
                    }
                    return ob;
                })
                .build()

            // first-broker flow
            .updateProfile().firstName("a").lastName("b").email(bc.getUserEmail()).username(bc.getUserLogin()).build()
            .followOneRedirect()

            .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse, Matchers.notNullValue());
        assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    /**
     * Tests that a SAML response without SubjectConfirmationData NotOnOrAfter
     * is still accepted (optional attribute per spec).
     */
    @Test
    public void testMissingSubjectConfirmationNotOnOrAfterIsAccepted() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(
                AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                getConsumerRoot() + "/sales-post/saml",
                null);

        Document doc = SAML2Request.convert(loginRep);

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
            .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST).build()
            .login().idp(bc.getIDPAlias()).build()

            .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                .targetAttributeSamlRequest()
                .build()

            .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

            .processSamlResponse(SamlClient.Binding.POST)    // Response from producer IdP
                .transformObject(ob -> {
                    ResponseType resp = (ResponseType) ob;
                    AssertionType assertion = resp.getAssertions().get(0).getAssertion();

                    // Clear NotOnOrAfter from SubjectConfirmationData
                    if (assertion.getSubject() != null) {
                        List<SubjectConfirmationType> confirmations = assertion.getSubject().getConfirmation();
                        if (confirmations != null) {
                            for (SubjectConfirmationType confirmation : confirmations) {
                                SubjectConfirmationDataType confirmationData = confirmation.getSubjectConfirmationData();
                                if (confirmationData != null) {
                                    confirmationData.setNotOnOrAfter(null);
                                }
                            }
                        }
                    }
                    return ob;
                })
                .build()

            // first-broker flow
            .updateProfile().firstName("a").lastName("b").email(bc.getUserEmail()).username(bc.getUserLogin()).build()
            .followOneRedirect()

            .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse, Matchers.notNullValue());
        assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }
}
