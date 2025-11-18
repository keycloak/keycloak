/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.common.Profile;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextClassRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.LevelOfAssuranceFlowTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
@EnableFeature(value = Profile.Feature.STEP_UP_AUTHENTICATION_SAML)
public class LevelOfAssuranceFlowSamlTest extends AbstractSamlTest {

    UserRepresentation otpUser;

    public LevelOfAssuranceFlowSamlTest() {
        otpUser = createUserRepresentation("user-with-one-configured-otp", "otp1@redhat.com", null, null, true);
        Users.setPasswordFor(otpUser, CredentialRepresentation.PASSWORD);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation testSaml = testRealms.iterator().next();
        testSaml.setOtpPolicyAlgorithm("HmacSHA1");
        testSaml.setOtpPolicyDigits(6);
        testSaml.setOtpPolicyInitialCounter(0);
        testSaml.setOtpPolicyLookAheadWindow(1);
        testSaml.setOtpPolicyPeriod(30);
        testSaml.setOtpPolicyType("totp");
        testSaml.setOtpPolicyCodeReusable(Boolean.TRUE);
    }

    @Test
    public void differentLevels() {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        // first request for level 1 password
        SamlClient samlClient = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .execute(this::assertResponsePassword);

        // request for level 1 password again, should be automatically done
        samlClient.execute(new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .assertResponse(this::assertResponsePassword)
                .getSteps());

        // request for level 2, should enforce OTP login
        samlClient.execute(new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .assertResponse(this::assertResponseTimeSyncToken)
                .getSteps());

        // request for level 3, by default max-age is 0 for otp, otp again and push button
        samlClient.execute(new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:custom:authentication:pushbutton")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .addStep(new PushButtonStep())
                .assertResponse(this::assertResponsePushButton)
                .getSteps());

    }

    @Test
    public void differentLevelsRedirect() {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        // first request for level 1 password
        SamlClient samlClient = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.REDIRECT)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build().doNotFollowRedirects()
                .execute(response -> assertResponse(response, "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport",
                        SamlClient.Binding.REDIRECT));

        // request for level 2, should enforce OTP login
        samlClient.execute(new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.REDIRECT)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build().doNotFollowRedirects()
                .assertResponse(response -> assertResponse(response, "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken",
                        SamlClient.Binding.REDIRECT))
                .getSteps());

        // request for level 3, by default max-age is 0 for otp, otp again and push button
        samlClient.execute(new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.REDIRECT)
                .addAuthnContextClassRef("urn:custom:authentication:pushbutton")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .addStep(new PushButtonStep()).doNotFollowRedirects()
                .assertResponse(response -> assertResponse(response, "urn:custom:authentication:pushbutton",
                        SamlClient.Binding.REDIRECT))
                .getSteps());
    }

    @Test
    public void invalidAuthnContextClassRef() {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        // request for an undefined authn context class ref
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard")
                .relayState("0123456789")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .execute(this::assertErrorSamlResponsePost);
    }

    @Test
    public void invalidAuthnContextClassRefRedirect() {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        // request for an undefined authn context class ref
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.REDIRECT)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build().doNotFollowRedirects()
                .execute(this::assertErrorSamlResponseRedirect);
    }

    private void minimunAuthnContextClassRefTimeSyncTokenTest() {
        // login with password is not enough because minimum is TimeSyncToken
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .execute(this::assertErrorSamlResponsePost);

        // login with TimeSyncToken should work as the minimum is OK
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);
    }

    @Test
    public void minimunAuthnContextClassRefTimeSyncToken() throws IOException {
        executeTest(this::minimunAuthnContextClassRefTimeSyncTokenTest, "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken");
    }

    @Test
    public void noAuthnContextClassRef() {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .execute(this::assertResponsePassword);
    }

    private void authnContextClassRefNotReachedTest() {
        // ask for level 4 that will not be fullfilled
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:custom:authentication:level4")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .addStep(new PushButtonStep())
                .execute(this::assertErrorAuthenticationRequirementsNotFullfilled);
    }

    @Test
    public void authnContextClassRefNotReached() throws IOException {
        Map<String, String> loaMap = Map.of(
                "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport", "1",
                "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken", "2",
                "urn:custom:authentication:pushbutton", "3",
                "urn:custom:authentication:level4", "4"
        );
        executeTest(this::authnContextClassRefNotReachedTest, loaMap, "");
    }

    private void authnContextClassRefIncorrectMatchWithFlowTest() {
        // ask password wich in flow is 1 but requesting is 0, it means that final level does not match with the requested level
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .execute(this::assertResponseUnspecified);
    }

    @Test
    public void authnContextClassRefIncorrectMatchWithFlow() throws IOException {
        Map<String, String> loaMap = Map.of(
                "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport", "0",
                "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken", "1",
                "urn:custom:authentication:pushbutton", "2"
        );
        executeTest(this::authnContextClassRefIncorrectMatchWithFlowTest, loaMap, "");
    }

    @Test
    public void authnContextClassRefOrder() {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        // first known authn context class ref is TimeSyncToken
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // first known authn context class ref is PasswordProtectedTransport
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .addAuthnContextClassRef("urn:custom:authentication:unknown")
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .execute(this::assertResponsePassword);
    }

    @Test
    public void invalidAuthnContextClassRefUri() throws IOException {
        // change the realm, because in realn there is no check
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        try (RealmAttributeUpdater realm = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
                .setAttribute(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(Map.of("invalid uri", "1")))
                .setAttribute(Constants.ACR_URI_MAP, JsonSerialization.writeValueAsString(Map.of("invalid uri", "invalid uri")))
                .update()) {

            // the name of the acr loa map is not a valid URI, check unspecified is used
            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                            SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                    .build()
                    .login().user(otpUser).build()
                    .execute(this::assertResponseUnspecified);
        }
    }

    @Test
    public void loaMapNotDefinedForSaml() throws IOException {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        // define no map for saml
        try (RealmAttributeUpdater realm = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
                .setAttribute(Constants.ACR_URI_MAP, "")
                .update()) {

            // the name of the acr loa map is not a valid URI, check unspecified is used
            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                            SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                    .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                    .build()
                    .login().user(otpUser).build()
                    .execute(this::assertResponseUnspecified);
        }
    }

    @Test
    public void noStepUpAuthentticationForSaml() throws IOException {
        // change the realm to use the default browser flow - no step-up authentication
        try (RealmAttributeUpdater realm = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
                .setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW)
                .update()) {

            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                            SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                    .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                    .build()
                    .login().user(bburkeUser).build()
                    .execute(this::assertResponseUnspecified);
        }
    }

    private void exactComparisonWithMinTimeSyncTest() {
        // request with a class ref less than min => error
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.EXACT)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .execute(this::assertErrorSamlResponsePost);

        // request with class equals to min => requested level
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.EXACT)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // request with class greater than min => requested level
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.EXACT)
                .addAuthnContextClassRef("urn:custom:authentication:pushbutton")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .addStep(new PushButtonStep())
                .execute(this::assertResponsePushButton);
    }

    private void exactComparisonNoMinTest() {
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.EXACT)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .execute(this::assertResponsePassword);

        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.EXACT)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);
    }

    @Test
    public void exactComparison() throws IOException {
        executeTest(this::exactComparisonWithMinTimeSyncTest, "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken");
        executeTest(this::exactComparisonNoMinTest);
    }

    private void minimumComparisonWithMinTimeSyncTest() {
        // request with a class ref less than min => min level TimeSync
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MINIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // request with class equals to min => min level TimeSync
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MINIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // request with min greater than min => request level pushbutton
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MINIMUM)
                .addAuthnContextClassRef("urn:custom:authentication:pushbutton")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .addStep(new PushButtonStep())
                .execute(this::assertResponsePushButton);
    }

    private void minimumComparisonNoMinTest() {
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MINIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .execute(this::assertResponsePassword);

        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MINIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);
    }

    @Test
    public void minimumComparison() throws IOException {
        executeTest(this::minimumComparisonWithMinTimeSyncTest, "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken");
        executeTest(this::minimumComparisonNoMinTest);
    }

    private void maximumComparisonWithMinTimeSyncTest() {
        // request with a class ref less than min => error
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MAXIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .execute(this::assertErrorSamlResponsePost);

        // request with a class ref equals or greater than min => that level
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MAXIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // request with a class ref equals or greater than min => that level
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MAXIMUM)
                .addAuthnContextClassRef("urn:custom:authentication:pushbutton")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .addStep(new PushButtonStep())
                .execute(this::assertResponsePushButton);
    }

    private void maximumComparisonWithNoMinTest() {
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MAXIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .execute(this::assertResponsePassword);
    }

    @Test
    public void maximumComparison() throws IOException {
        executeTest(this::maximumComparisonWithMinTimeSyncTest, "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken");
        executeTest(this::maximumComparisonWithNoMinTest);
    }

    private void betterComparisonWithMinTimeSyncTest() {
        // request with a class ref less than min => min is returned
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.BETTER)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // request with a class ref equals to min => next level is returned
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.BETTER)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .addStep(new PushButtonStep())
                .execute(this::assertResponsePushButton);

        // request with a class equals to max => error
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.BETTER)
                .addAuthnContextClassRef("urn:custom:authentication:pushbutton")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .execute(this::assertErrorSamlResponsePost);
    }

    private void betterComparisonNoMinTest() {
        // always next level is returned
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.BETTER)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .addStep(new PushButtonStep())
                .execute(this::assertResponsePushButton);

        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.BETTER)
                .addAuthnContextClassRef("urn:custom:authentication:pushbutton")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .execute(this::assertErrorSamlResponsePost);
    }

    @Test
    public void betterComparison() throws IOException {
        executeTest(this::betterComparisonWithMinTimeSyncTest, "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken");
        executeTest(this::betterComparisonNoMinTest);
    }

    private void severalAuthnContextClassRefTest() {
        // exact should return the first one that accepts min
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // minimum should return the first one that accepts min
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MINIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // maximum should return the first one that is min or greater
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.MAXIMUM)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);

        // better should return the next one to first which is valid
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_SIG,
                        SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, SamlClient.Binding.POST)
                .setComparison(AuthnContextComparisonType.BETTER)
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .addAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken")
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(otpUser).build()
                .otpLogin().otp(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A")).build()
                .execute(this::assertResponseTimeSyncToken);
    }

    @Test
    public void severalAuthnContextClassRef() throws IOException {
        executeTest(this::severalAuthnContextClassRefTest, "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken");
    }

    private void executeTest(Runnable test) throws IOException {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);
        test.run();
    }

    private void executeTest(Runnable test, String minAcr) throws IOException {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST_SIG)
                .setAttribute(Constants.MINIMUM_ACR_VALUE, minAcr)
                .update()) {
            test.run();
        }
    }

    private void executeTest(Runnable test, Map<String, String> acrLoaMap, String minAcr) throws IOException {
        LevelOfAssuranceFlowTest.configureStepUpFlow(REALM_NAME, testingClient);

        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST_SIG)
                .setAttribute(Constants.MINIMUM_ACR_VALUE, minAcr)
                .setAttribute(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap))
                .update()) {
            test.run();
        }
    }

    private void assertErrorAuthenticationRequirementsNotFullfilled(CloseableHttpResponse response) {
        assertErrorPage(response, "Authentication requirements not fulfilled");
    }

    private void assertErrorPage(CloseableHttpResponse response, String errorMessage) {
        try {
            MatcherAssert.assertThat(response, Matchers.statusCodeIsHC(Status.BAD_REQUEST));
            String page = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            MatcherAssert.assertThat(page, CoreMatchers.containsString(errorMessage));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertErrorSamlResponsePost(CloseableHttpResponse response) {
        assertErrorSamlResponse(response, SamlClient.Binding.POST);
    }

    private void assertErrorSamlResponseRedirect(CloseableHttpResponse response) {
        assertErrorSamlResponse(response, SamlClient.Binding.REDIRECT);
    }

    private void assertErrorSamlResponse(CloseableHttpResponse response, SamlClient.Binding binding) {
        try {
            SAMLDocumentHolder holder = binding.extractResponse(response);
            MatcherAssert.assertThat(holder.getSamlObject(), Matchers.isSamlStatusResponse(
                    JBossSAMLURIConstants.STATUS_RESPONDER, JBossSAMLURIConstants.STATUS_NOAUTHN_CTX));
            ResponseType responseType = (ResponseType) holder.getSamlObject();
            Assert.assertNotNull(responseType.getInResponseTo());
            Assert.assertNotNull(responseType.getID());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertResponseUnspecified(CloseableHttpResponse response) {
        assertResponse(response, "urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified", SamlClient.Binding.POST);
    }

    private void assertResponsePassword(CloseableHttpResponse response) {
        assertResponse(response, "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport", SamlClient.Binding.POST);
    }

    private void assertResponseTimeSyncToken(CloseableHttpResponse response) {
        assertResponse(response, "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken", SamlClient.Binding.POST);
    }

    private void assertResponsePushButton(CloseableHttpResponse response) {
        assertResponse(response, "urn:custom:authentication:pushbutton", SamlClient.Binding.POST);
    }

    private void assertResponse(CloseableHttpResponse response, String classRef, SamlClient.Binding binding) {
        try {
            SAMLDocumentHolder holder = binding.extractResponse(response);
            MatcherAssert.assertThat(holder.getSamlObject(), Matchers.isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
            ResponseType responseType = (ResponseType) holder.getSamlObject();
            Assert.assertNotNull(responseType.getInResponseTo());
            Assert.assertNotNull(responseType.getID());
            Optional<URI> authContextClassRefOpt = responseType.getAssertions().stream()
                    .map(ResponseType.RTChoiceType::getAssertion)
                    .map(AssertionType::getStatements)
                    .flatMap(s -> s.stream())
                    .filter(AuthnStatementType.class::isInstance)
                    .map(AuthnStatementType.class::cast)
                    .findAny()
                    .map(AuthnStatementType::getAuthnContext)
                    .filter(Objects::nonNull)
                    .map(AuthnContextType::getSequence)
                    .filter(Objects::nonNull)
                    .map(AuthnContextType.AuthnContextTypeSequence::getClassRef)
                    .filter(Objects::nonNull)
                    .map(AuthnContextClassRefType::getValue);
            Assert.assertTrue(authContextClassRefOpt.isPresent());
            Assert.assertEquals(classRef, authContextClassRefOpt.get().toString());
        } catch (IOException e)  {
            throw new RuntimeException(e);
        }
    }

    private static class PushButtonStep implements SamlClient.Step {

        @Override
        public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
            MatcherAssert.assertThat(currentResponse, Matchers.statusCodeIsHC(Status.OK));
            String pageContent = EntityUtils.toString(currentResponse.getEntity(), StandardCharsets.UTF_8);
            org.jsoup.nodes.Document page = Jsoup.parse(pageContent);
            Elements forms = page.getElementsByTag("form");
            Assert.assertEquals(1, forms.size());
            Element form = forms.get(0);
            HttpPost res = new HttpPost(form.attr("action"));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(Collections.emptyList(), StandardCharsets.UTF_8);
            res.setEntity(formEntity);
            return res;
        }
    }
}
