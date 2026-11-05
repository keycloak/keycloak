/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oidc;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ClaimsParameterTokenMapper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.JwksProvider;
import org.keycloak.testframework.oauth.OIDCClientApplicationProvider;
import org.keycloak.testframework.oauth.OIDCClientApplicationProviderConfig;
import org.keycloak.testframework.oauth.OIDCClientApplicationProviderConfiguration;
import org.keycloak.testframework.oauth.annotations.InjectJwksProvider;
import org.keycloak.testframework.oauth.annotations.InjectOIDCClientApplicationProvider;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.KeyUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for supporting advanced parameters of OIDC specs (max_age, prompt, ...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class OIDCAdvancedRequestParamsTest extends AbstractOIDCScopeTest {

    private static final String REQUEST_URI_CONTEXT_PATH = "/get-oidc-request";
    private static final String CLIENT_ID = "test-app";
    private static final String USERNAME = "test-user@localhost";
    private static final String PASSWORD = "password";
    private static final String STATE = "mystate2";
    // The admin client omits null values when serializing a representation, empty value is set
    private static final String CLEAR_ATTRIBUTE = "";

    @InjectRealm(config = OIDCAdvancedRequestParamsRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectJwksProvider
    JwksProvider jwksProvider;

    @InjectOIDCClientApplicationProvider(config = OIDCAdvancedRequestParamsClientApplicationProviderConfig.class)
    OIDCClientApplicationProvider oidcClientApplicationProvider;

    @InjectPage
    RegisterPage registerPage;

    // Max_age

    @Test
    public void testMaxAge1() {
        long authTime = loginAndGetAuthTime();

        timeOffSet.set(10);

        // Assert I need to log in again through the login form. But username field is not present
        oauth.loginForm().maxAge(1).open();
        loginPage.assertCurrent();
        Assertions.assertFalse(loginPage.isUsernameInputPresent());
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        // Assert that authTime was updated
        Assertions.assertTrue(authTime + 10 <= pollLoginAndGetAuthTime());
    }

    @Test
    public void testMaxAge10000() {
        long authTime = loginAndGetAuthTime();

        timeOffSet.set(10);

        // Assert that I will be automatically logged through  cookie
        oauth.loginForm().maxAge(10000).open();

        // Assert that authTime is still the same
        Assertions.assertEquals(authTime, pollLoginAndGetAuthTime());
    }

    @Test
    public void testMaxAgeIntegerOverflow() {
        long authTime = loginAndGetAuthTime();

        timeOffSet.set(10);

        // max_age close to Integer.MAX_VALUE must not overflow the authTime+maxAge sum and
        // incorrectly force re-authentication
        oauth.loginForm().maxAge(Integer.MAX_VALUE).open();

        // Assert that authTime is still the same (no re-auth happened)
        Assertions.assertEquals(authTime, pollLoginAndGetAuthTime());
    }

    // Prompt

    @Test
    public void promptNoneNotLogged() {
        String expectedIssuer = oauth.doWellKnownRequest().getIssuer();

        oauth.loginForm().prompt("none").open();

        Assertions.assertTrue(oauth.parseLoginResponse().isError());
        Assertions.assertNull(events.poll());

        // Assert error response was sent because not logged in
        AuthorizationEndpointResponse resp = oauth.parseLoginResponse();
        Assertions.assertEquals(expectedIssuer, resp.getIssuer());
        Assertions.assertNull(resp.getCode());
        Assertions.assertEquals(OAuthErrorException.LOGIN_REQUIRED, resp.getError());
    }

    @Test
    public void promptNoneSuccess() {
        oauth.openLoginForm();
        authenticatePassword(USERNAME, PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).details(Details.USERNAME, USERNAME);
        long authTime = sendTokenRequestAndGetIDToken(loginEvent).getAuth_time();

        timeOffSet.set(10);

        // Assert user still logged with previous authTime
        oauth.loginForm().prompt("none").open();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        Assertions.assertEquals(authTime, pollLoginAndGetAuthTime());
    }

    @Test
    public void promptNoneConsentRequired() {
        requireConsent();

        // Assert error shown when trying prompt=none and consent not yet granted
        oauth.loginForm().prompt("none").open();
        Assertions.assertTrue(oauth.parseLoginResponse().isError());

        AuthorizationEndpointResponse resp = oauth.parseLoginResponse();
        Assertions.assertNull(resp.getCode());
        Assertions.assertEquals(OAuthErrorException.LOGIN_REQUIRED, resp.getError());

        // Login and confirm consent
        oauth.openLoginForm();
        authenticatePassword(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        grantPage.accept();

        assertConsentLoginEvent(Details.CONSENT_VALUE_CONSENT_GRANTED);

        // Consent not required anymore. Login with prompt=none should success
        oauth.loginForm().prompt("none").open();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        resp = oauth.parseLoginResponse();
        Assertions.assertNotNull(resp.getCode());
        Assertions.assertNull(resp.getError());

        assertConsentLoginEvent(Details.CONSENT_VALUE_PERSISTED_CONSENT);
    }

    @Test
    public void promptLogin() {
        oauth.openLoginForm();
        authenticatePassword(USERNAME, PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).details(Details.USERNAME, USERNAME);
        IDToken oldIdToken = sendTokenRequestAndGetIDToken(loginEvent);

        timeOffSet.set(10);

        // SSO login first WITHOUT prompt=login ( Tests KEYCLOAK-5248 )
        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).details(Details.USERNAME, USERNAME);
        IDToken newIdToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Assert that authTime wasn't updated
        Assertions.assertEquals(oldIdToken.getAuth_time(), newIdToken.getAuth_time());

        timeOffSet.set(20);

        // Assert need to re-authenticate with prompt=login
        oauth.loginForm().prompt("login").open();
        loginPage.assertCurrent();
        Assertions.assertFalse(loginPage.isUsernameInputPresent());
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).details(Details.USERNAME, USERNAME);
        newIdToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Assert that authTime was updated
        Assertions.assertTrue(oldIdToken.getAuth_time() + 20 <= newIdToken.getAuth_time(),
                "Expected auth time to change. old auth time: " + oldIdToken.getAuth_time() + " , new auth time: " + newIdToken.getAuth_time());

        // Assert userSession didn't change
        Assertions.assertEquals(oldIdToken.getSessionState(), newIdToken.getSessionState());
    }

    @Test
    public void promptCreate() {
        oauth.loginForm().prompt("create").open();
        registerPage.assertCurrent();
    }

    @Test
    public void promptCreateShouldFailWhenRegistrationsAreDisabled() {
        managedRealm.updateWithCleanup(r -> r.registrationAllowed(false));

        oauth.loginForm().prompt("create").open();
        errorPage.assertCurrent();
        Assertions.assertTrue(errorPage.getError().contains("Registration not allowed"));
    }

    @Test
    public void promptConsent() {
        requireConsent();

        oauth.openLoginForm();
        authenticatePassword(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        grantPage.accept();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        assertConsentLoginEvent(Details.CONSENT_VALUE_CONSENT_GRANTED);

        // Re-login without prompt=consent. The previous persistent consent was used
        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        assertConsentLoginEvent(Details.CONSENT_VALUE_PERSISTED_CONSENT);

        // Re-login with prompt=consent. Assert grant page displayed again, will need to grant consent again
        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_CONSENT).open();
        grantPage.assertCurrent();
        grantPage.accept();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.expectLoginSuccess(events.poll())
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
    }

    // DISPLAY & OTHERS

    @Test
    public void nonSupportedParams() {
        oauth.loginForm().param("display", "popup").param("foo", "foobar").param("claims_locales", "fr").open();

        authenticatePassword(USERNAME, PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).details(Details.USERNAME, USERNAME);

        Assertions.assertNotNull(sendTokenRequestAndGetIDToken(loginEvent));
    }

    // REQUEST & REQUEST_URI

    @Test
    public void requestObjectNotRequiredNotProvided() {
        setRequestObjectRequired(null);

        AuthorizationEndpointResponse response = oauth.loginForm().state(STATE).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, STATE);
    }

    @Test
    public void requestObjectNotRequiredProvidedInRequestParam() {
        setRequestObjectRequired(null);

        String requestObject = createOidcRequest(oauth.getRedirectUri(), "10", STATE);

        AuthorizationEndpointResponse response = oauth.loginForm().request(requestObject).state(STATE).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, STATE);
    }

    @Test
    public void requestObjectNotRequiredProvidedInRequestUriParam() {
        setRequestUris(oidcClientApplicationProvider.getUri());
        setRequestObjectRequired(null);
        setOidcRequest(oauth.getRedirectUri(), "10", STATE, "none");

        AuthorizationEndpointResponse response = oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).state(STATE).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, STATE);
        Assertions.assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }

    @Test
    public void requestObjectRequiredNotProvided() {
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI);

        // Send request without request object
        oauth.loginForm().state(STATE).open();
        assertInvalidRequest();
    }

    @Test
    public void requestObjectRequiredProvidedInRequestParam() {
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI);

        String requestObject = createOidcRequest(oauth.getRedirectUri(), "10", STATE);

        AuthorizationEndpointResponse response = oauth.loginForm().request(requestObject).state(STATE).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, STATE);
    }

    @Test
    public void requestObjectSupersedesQueryParameter() {
        String stateInRequestObject = "stateInRequestObject";
        String stateInQueryParameter = "stateInQueryParameter";

        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI);

        String requestObject = createOidcRequest(oauth.getRedirectUri(), "10", stateInRequestObject);

        AuthorizationEndpointResponse response = oauth.loginForm().request(requestObject).state(stateInQueryParameter).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, stateInRequestObject);
        Assertions.assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }

    @Test
    public void requestObjectClientIdAndResponseTypeTest() {
        // Test that "client_id" mandatory in the query even if set in the "request" object
        String request = createOidcRequest(oauth.getRedirectUri(), "10", "some-state");
        oauth.client(null);
        oauth.loginForm().request(request).state("some-state").open();
        assertInvalidRequest();

        // Test that "response_type" mandatory in the query even if set in the "request" object
        oauth.client(CLIENT_ID, PASSWORD);
        oauth.responseType(null);
        oauth.loginForm().request(request).state("some-state").open();
        assertInvalidRequestResponse("some-state");

        // Test that different "client_id" in the query and in the request object is disallowed
        oauth.client("test-app-scope");
        oauth.responseType(OAuth2Constants.CODE);
        oauth.loginForm().request(request).state("some-state").open();
        assertInvalidRequest();

        // Test that different "response_type" in the query and in the request object is disallowed
        oauth.client(CLIENT_ID, PASSWORD);
        oauth.responseType(OAuth2Constants.CODE + " " + OAuth2Constants.ID_TOKEN);
        oauth.loginForm().request(request).state("some-state").open();
        oauth.responseMode("query"); // Keycloak falls back to query in this case
        assertInvalidRequestResponse("some-state");
        oauth.responseMode(null);

        // Test that "client_id" and "response_type" are not mandatory in the request object
        Map<String, Object> oidcRequest = new HashMap<>();
        oidcRequest.put(OIDCLoginProtocol.REDIRECT_URI_PARAM, oauth.getRedirectUri());
        oidcRequest.put(OIDCLoginProtocol.STATE_PARAM, "request-state");
        String requestObjectString = oidcClientApplicationProvider.signRequestObject(oidcRequest, "none");

        oauth.client(CLIENT_ID, PASSWORD);
        oauth.responseType(OAuth2Constants.CODE);
        AuthorizationEndpointResponse response = oauth.loginForm().request(requestObjectString).state("some-state").doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, "request-state");
        Assertions.assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }

    @Test
    public void requestObjectRequiredProvidedInRequestUriParam() {
        setRequestUris(oidcClientApplicationProvider.getUri());
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI);
        setOidcRequest(oauth.getRedirectUri(), "10", STATE, "none");

        AuthorizationEndpointResponse response = oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).state(STATE).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, STATE);
    }

    @Test
    public void requestObjectRequiredAsRequestParamNotProvided() {
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST);

        oauth.loginForm().state(STATE).open();
        assertInvalidRequest();
    }

    @Test
    public void requestObjectRequiredAsRequestParamProvidedInRequestParam() {
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST);

        String requestObject = createOidcRequest(oauth.getRedirectUri(), "10", STATE);

        AuthorizationEndpointResponse response = oauth.loginForm().request(requestObject).state(STATE).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, STATE);
    }

    @Test
    public void requestObjectRequiredAsRequestParamProvidedInRequestUriParam() {
        setRequestUris(oidcClientApplicationProvider.getUri());
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST);
        setOidcRequest(oauth.getRedirectUri(), "10", null, "none");

        oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).state(STATE).open();
        assertInvalidRequest();
    }

    @Test
    public void requestObjectRequiredAsRequestUriParamNotProvided() {
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_URI);

        oauth.loginForm().state(STATE).open();
        assertInvalidRequest();
    }

    @Test
    public void requestObjectRequiredAsRequestUriParamProvidedInRequestParam() {
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_URI);

        String requestObject = createOidcRequest(oauth.getRedirectUri(), "10", null);

        oauth.loginForm().request(requestObject).state(STATE).open();
        assertInvalidRequest();
    }

    @Test
    public void requestObjectRequiredAsRequestUriParamProvidedInRequestUriParam() {
        setRequestUris(oidcClientApplicationProvider.getUri());
        setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_URI);
        setOidcRequest(oauth.getRedirectUri(), "10", STATE, "none");

        AuthorizationEndpointResponse response = oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).state(STATE).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, STATE);
    }

    @Test
    public void requestParamUnsigned() {
        String validRedirectUri = oauth.getRedirectUri();

        // Send request object with invalid redirect uri.
        String requestStr = createOidcRequest("http://invalid", null, null);

        oauth.loginForm().request(requestStr).state(STATE).open();
        errorPage.assertCurrent();
        Assertions.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        // Assert the value from request object has bigger priority then from the query parameter.
        oauth.redirectUri("http://invalid");
        requestStr = createOidcRequest(validRedirectUri, "10", STATE);

        AuthorizationEndpointResponse response = oauth.loginForm().request(requestStr).state(STATE).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, STATE);
        Assertions.assertTrue(driver.getCurrentUrl().startsWith(validRedirectUri));
    }

    @Test
    public void requestUriParamUnsigned() {
        String validRedirectUri = oauth.getRedirectUri();

        // Send request object with invalid redirect uri.
        setOidcRequest("http://invalid", null, "mystate1", "none");
        setRequestUris(oidcClientApplicationProvider.getUri());

        oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).open();
        errorPage.assertCurrent();
        Assertions.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        // Assert the value from request object has bigger priority then from the query parameter.
        oauth.redirectUri("http://invalid");
        setOidcRequest(validRedirectUri, "10", "mystate1", "none");

        AuthorizationEndpointResponse response = oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, "mystate1");
        Assertions.assertFalse(oauth.parseLoginResponse().isRedirected());
    }

    @Test
    public void requestUriParamWithAllowedRequestUris() {
        setOidcRequest(oauth.getRedirectUri(), "10", "mystate1", "none");

        updateClient(clientRep -> clientRep.setRootUrl(UriUtils.getOrigin(oidcClientApplicationProvider.getUri())));

        // Test with the relative allowed request_uri - should pass
        String absoluteRequestUri = oidcClientApplicationProvider.getUri();
        String requestUri = absoluteRequestUri.substring(UriUtils.getOrigin(absoluteRequestUri).length());
        setRequestUris(requestUri);

        oauth.loginForm().requestUri(absoluteRequestUri).open();
        loginPage.assertCurrent();

        // Test with the relative and star at the end - should pass
        requestUri = requestUri.replace(REQUEST_URI_CONTEXT_PATH, "/*");
        setRequestUris(requestUri);

        oauth.loginForm().requestUri(absoluteRequestUri).open();
        loginPage.assertCurrent();

        // Test absolute and wildcard at the end - should pass
        requestUri = absoluteRequestUri.replace(REQUEST_URI_CONTEXT_PATH, "/*");
        setRequestUris(requestUri);

        oauth.loginForm().requestUri(absoluteRequestUri).open();
        loginPage.assertCurrent();

        // Test star only as wildcard - should pass
        setRequestUris("*");

        oauth.loginForm().requestUri(absoluteRequestUri).open();
        loginPage.assertCurrent();

        // Test with multiple request_uris - should pass
        setRequestUris("/foo", requestUri);

        oauth.loginForm().requestUri(absoluteRequestUri).open();
        loginPage.assertCurrent();

        // Test invalid request_uris - should fail
        setRequestUris("/foo", requestUri.replace("/*", "/foo"));

        oauth.loginForm().requestUri(absoluteRequestUri).open();
        assertInvalidRequest();

        // Test with no request_uri set at all - should fail
        setRequestUris();

        oauth.loginForm().requestUri(absoluteRequestUri).open();
        assertInvalidRequest();
    }

    @Test
    public void requestUriParamSigned() {
        String validRedirectUri = oauth.getRedirectUri();

        setRequestUris(oidcClientApplicationProvider.getUri());

        // Set required signature for request_uri
        updateClientConfig(config -> config.setRequestObjectSignatureAlg(Algorithm.RS256));

        // Verify unsigned request_uri will fail
        setOidcRequest(validRedirectUri, "10", null, "none");
        oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).open();
        assertInvalidRequest();

        // Verify signed request_uri will fail due to failed signature validation
        setOidcRequest(validRedirectUri, "10", "mystate3", Algorithm.RS256);
        oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).open();
        assertInvalidRequest();

        // Update clientModel with publicKey for signing
        CertificateRepresentation cert = new CertificateRepresentation();
        cert.setPublicKey(jwksProvider.getKeysAsPem().get(JwksProvider.PUBLIC_KEY));
        updateClient(clientRep -> CertificateInfoHelper.updateClientRepresentationCertificateInfo(clientRep, cert, JWTClientAuthenticator.ATTR_PREFIX));

        // set time offset, so that new keys are downloaded
        timeOffSet.set(20);

        // Check signed request_uri will pass
        AuthorizationEndpointResponse response = oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).doLogin(USERNAME, PASSWORD);
        assertLoginSuccess(response, "mystate3");
    }

    @Test
    public void requestUriParamSignedExpectedES256ActualRS256() {
        // will fail
        requestUriParamSignedIn(Algorithm.ES256, Algorithm.RS256);
    }

    @Test
    public void requestUriParamSignedExpectedNoneActualES256() {
        // will fail
        requestUriParamSignedIn("none", Algorithm.ES256);
    }

    @Test
    public void requestUriParamSignedExpectedNoneActualNone() {
        // will success
        requestUriParamSignedIn("none", "none");
    }

    @Test
    public void requestUriParamSignedExpectedES256ActualES256() {
        // will success
        requestUriParamSignedIn(Algorithm.ES256, Algorithm.ES256);
    }

    @Test
    public void requestUriParamSignedExpectedES384ActualES384() {
        // will success
        requestUriParamSignedIn(Algorithm.ES384, Algorithm.ES384);
    }

    @Test
    public void requestUriParamSignedExpectedES512ActualES512() {
        // will success
        requestUriParamSignedIn(Algorithm.ES512, Algorithm.ES512);
    }

    @Test
    public void requestUriParamSignedExpectedRS384ActualRS384() {
        // will success
        requestUriParamSignedIn(Algorithm.RS384, Algorithm.RS384);
    }

    @Test
    public void requestUriParamSignedExpectedRS512ActualRS512() {
        // will success
        requestUriParamSignedIn(Algorithm.RS512, Algorithm.RS512);
    }

    @Test
    public void requestUriParamSignedExpectedPS256ActualPS256() {
        // will success
        requestUriParamSignedIn(Algorithm.PS256, Algorithm.PS256);
    }

    @Test
    public void requestUriParamSignedExpectedPS384ActualPS384() {
        // will success
        requestUriParamSignedIn(Algorithm.PS384, Algorithm.PS384);
    }

    @Test
    public void requestUriParamSignedExpectedPS512ActualPS512() {
        // will success
        requestUriParamSignedIn(Algorithm.PS512, Algorithm.PS512);
    }

    @Test
    public void requestUriParamSignedExpectedEd25519ActualEd25519() {
        // will success
        requestUriParamSignedIn(Algorithm.EdDSA, Algorithm.EdDSA, Algorithm.Ed25519);
    }

    @Test
    public void requestUriParamSignedExpectedES256ActualEd448() {
        // will fail
        requestUriParamSignedIn(Algorithm.ES256, Algorithm.EdDSA, Algorithm.Ed448);
    }

    @Test
    public void requestUriParamSignedExpectedAnyActualES256() {
        // Algorithm is null if 'any'
        // will success
        requestUriParamSignedIn(null, Algorithm.ES256);
    }

    // LOGIN_HINT

    @Test
    public void loginHint() {
        oauth.loginForm().loginHint("test-user%40localhost").open();

        loginPage.assertCurrent();
        Assertions.assertEquals(USERNAME, loginPage.getUsername());
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, USERNAME);
    }

    // CLAIMS
    // included in the session client notes, so custom providers can make use of it

    @Test
    public void processClaimsQueryParam() throws IOException {
        Map<String, Object> claims = Map.of("id_token", Map.of("test_claim", Map.of("essential", true)));
        String claimsJson = JsonSerialization.writeValueAsString(claims);

        oauth.loginForm().param(OIDCLoginProtocol.CLAIMS_PARAM, URLEncoder.encode(claimsJson, StandardCharsets.UTF_8)).open();

        loginAndAssertClaimsInSession(claimsJson);
    }

    @Test
    public void processClaimsRequestParam() throws IOException {
        Map<String, Object> claims = Map.of("id_token", Map.of("test_claim", Map.of("essential", true)));
        String claimsJson = JsonSerialization.writeValueAsString(claims);

        oauth.loginForm().param(OIDCLoginProtocol.REQUEST_PARAM, createUnsignedClaimsRequest(claims)).open();

        loginAndAssertClaimsInSession(claimsJson);
    }

    @Test
    public void processClaimsRequestParamSupported() {
        String profileScopeId = managedRealm.admin().clientScopes().findAll().stream()
                .filter(scope -> "profile".equals(scope.getName()))
                .findFirst().orElseThrow().getId();
        oauth.clientResource().removeDefaultClientScope(profileScopeId);

        ProtocolMappersResource mappers = oauth.clientResource().getProtocolMappers();
        mappers.createMapper(ModelToRepresentation.toRepresentation(ClaimsParameterTokenMapper.createMapper("claimsParameterTokenMapper", true, false))).close();

        Map<String, Object> claims = Map.of(
                "id_token", Map.of(
                        "email", Map.of("essential", true),
                        "preferred_username", Map.of("essential", true),
                        "family_name", Map.of("essential", false),
                        "given_name", Map.of("wesentlich", true),
                        "name", Map.of("essential", true)),
                "userinfo", Map.of(
                        "preferred_username", Map.of("essential", "Ja"),
                        "family_name", Map.of("essential", true),
                        "given_name", Map.of("essential", true)));

        oauth.loginForm().request(createUnsignedClaimsRequest(claims)).doLogin(USERNAME, PASSWORD);
        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent);

        AccessTokenResponse accessTokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        IDToken idToken = oauth.verifyIDToken(accessTokenResponse.getIdToken());
        Assertions.assertEquals(USERNAME, idToken.getEmail());
        Assertions.assertEquals(USERNAME, idToken.getPreferredUsername());
        Assertions.assertNull(idToken.getFamilyName());
        Assertions.assertNull(idToken.getGivenName());
        Assertions.assertEquals("Tom Brady", idToken.getName());

        UserInfoResponse userInfoResponse = oauth.userInfoRequest(accessTokenResponse.getAccessToken()).send();
        Assertions.assertEquals(200, userInfoResponse.getStatusCode());
        UserInfo userInfo = userInfoResponse.getUserInfo();
        Assertions.assertEquals(USERNAME, userInfo.getEmail());
        Assertions.assertNull(userInfo.getPreferredUsername());
        Assertions.assertEquals("Brady", userInfo.getFamilyName());
        Assertions.assertEquals("Tom", userInfo.getGivenName());
        Assertions.assertNull(userInfo.getName());
        EventAssertion.assertSuccess(events.poll()).type(EventType.USER_INFO_REQUEST).sessionId(accessTokenResponse.getSessionState()).clientId(CLIENT_ID);

        oauth.doLogout(accessTokenResponse.getRefreshToken());
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT)
                .sessionId(accessTokenResponse.getSessionState()).clientId(oauth.getClientId()).withoutDetails(Details.REDIRECT_URI);

        claims = Map.of(
                "id_token", Map.of("test_claim", Map.of("essential", true)),
                "access_token", Map.of(
                        "email", Map.of("essential", true),
                        "preferred_username", Map.of("essential", true),
                        "family_name", Map.of("essential", true),
                        "given_name", Map.of("essential", true),
                        "name", Map.of("essential", true)));

        oauth.loginForm().request(createUnsignedClaimsRequest(claims)).doLogin(USERNAME, PASSWORD);
        loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent);

        accessTokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        idToken = oauth.verifyIDToken(accessTokenResponse.getIdToken());
        Assertions.assertEquals(USERNAME, idToken.getEmail()); // "email" default scope still remains
        Assertions.assertNull(idToken.getPreferredUsername());
        Assertions.assertNull(idToken.getFamilyName());
        Assertions.assertNull(idToken.getGivenName());
        Assertions.assertNull(idToken.getName());

        userInfoResponse = oauth.userInfoRequest(accessTokenResponse.getAccessToken()).send();
        Assertions.assertEquals(200, userInfoResponse.getStatusCode());
        userInfo = userInfoResponse.getUserInfo();
        Assertions.assertEquals(USERNAME, userInfo.getEmail());
        Assertions.assertNull(userInfo.getPreferredUsername());
        Assertions.assertNull(userInfo.getFamilyName());
        Assertions.assertNull(userInfo.getGivenName());
        Assertions.assertNull(userInfo.getName());
    }

    // ENCRYPTED & SIGNED REQUEST OBJECTS

    @Test
    public void testSignedRequestObject() throws IOException {
        oauth.loginForm().request(createAndSignRequestObject()).doLogin(USERNAME, PASSWORD);
        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void testWrongEncryptionAlgorithm() throws Exception {
        updateClientConfig(config -> config.setRequestObjectEncryptionAlg(JWEConstants.RSA_OAEP_256));

        // Request object encrypted with another algorithm than the one required by the client is rejected
        oauth.loginForm().request(createEncryptedRequestObject(JWEConstants.RSA_OAEP)).open();
        assertInvalidRequest();

        // Request object encrypted with the required algorithm is accepted
        oauth.loginForm().request(createEncryptedRequestObject(JWEConstants.RSA_OAEP_256)).doLogin(USERNAME, PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        // Encrypted request objects are still accepted once the client no longer requires an algorithm
        updateClientConfig(config -> config.setRequestObjectEncryptionAlg(CLEAR_ATTRIBUTE));
        assertRequestObjectEncryptionConfig(null, null);

        logout();

        oauth.loginForm().request(createEncryptedRequestObject(JWEConstants.RSA_OAEP_256)).doLogin(USERNAME, PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testWrongContentEncryptionAlgorithm() throws Exception {
        updateClientConfig(config -> {
            config.setRequestObjectEncryptionAlg(JWEConstants.RSA_OAEP_256);
            config.setRequestObjectEncryptionEnc(JWEConstants.A192GCM);
        });
        assertRequestObjectEncryptionConfig(JWEConstants.RSA_OAEP_256, JWEConstants.A192GCM);

        // The request object content is encrypted with A256GCM, which is not the required A192GCM
        oauth.loginForm().request(createEncryptedRequestObject(JWEConstants.RSA_OAEP_256)).open();
        assertInvalidRequest();

        // Request object encrypted with the required content encryption algorithm is accepted
        updateClientConfig(config -> config.setRequestObjectEncryptionEnc(JWEConstants.A256GCM));
        assertRequestObjectEncryptionConfig(JWEConstants.RSA_OAEP_256, JWEConstants.A256GCM);

        oauth.loginForm().request(createEncryptedRequestObject(JWEConstants.RSA_OAEP_256)).doLogin(USERNAME, PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        // Encrypted request objects are still accepted once the client no longer requires an algorithm
        updateClientConfig(config -> {
            config.setRequestObjectEncryptionAlg(CLEAR_ATTRIBUTE);
            config.setRequestObjectEncryptionEnc(CLEAR_ATTRIBUTE);
        });
        assertRequestObjectEncryptionConfig(null, null);

        logout();

        oauth.loginForm().request(createEncryptedRequestObject(JWEConstants.RSA_OAEP_256)).doLogin(USERNAME, PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testSignedAndEncryptedRequestObject() throws IOException, JWEException {
        oauth.loginForm().request(createEncryptedRequestObject(JWEConstants.RSA_OAEP_256)).doLogin(USERNAME, PASSWORD);
        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void testRealmPublicKeyEncryptedRequestObjectUsingRSA_OAEP_256WithA256GCM() throws Exception {
        assertRequestObjectEncryption(new JWEHeader(JWEConstants.RSA_OAEP_256, JWEConstants.A256GCM, null));
    }

    @Test
    public void testRealmPublicKeyEncryptedRequestObjectUsingRSA_OAEPWithA128CBC_HS256() throws Exception {
        assertRequestObjectEncryption(new JWEHeader(JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256, null));
    }

    @Test
    public void testRealmPublicKeyEncryptedRequestObjectUsingKid() throws Exception {
        KeysMetadataRepresentation.KeyMetadataRepresentation encKey = KeyUtils.findActiveEncryptingKey(managedRealm.admin(), Algorithm.RSA_OAEP);
        assertRequestObjectEncryption(new JWEHeader(JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256, null, encKey.getKid()));
    }

    /**
     * A JWE whose header {@code alg} differs from the provisioned algorithm of the decryption key must be rejected.
     */
    @Test
    public void testRequestObjectEncryptedWithWrongKeyManagementAlgorithmIsRejected() throws Exception {
        KeysMetadataRepresentation.KeyMetadataRepresentation encKey = KeyUtils.findActiveEncryptingKey(managedRealm.admin(), Algorithm.RSA_OAEP);

        String request = encryptRequestObject(
                new JWEHeader(JWEConstants.RSA1_5, JWEConstants.A128CBC_HS256, null, encKey.getKid()),
                encKey.getKid(), createAndSignRequestObject().getBytes());
        oauth.loginForm().request(request).open();

        assertInvalidRequest();
    }

    /**
     * A passive key is still advertised in the JWKS, so it must be usable when referenced by {@code kid},
     * but must not be picked when the {@code kid} is missing.
     */
    @Test
    public void testRequestObjectEncryptedWithPassiveKey() throws Exception {
        KeysMetadataRepresentation.KeyMetadataRepresentation encKey = KeyUtils.findActiveEncryptingKey(managedRealm.admin(), Algorithm.RSA_OAEP);
        ComponentRepresentation component = managedRealm.admin().components().component(encKey.getProviderId()).toRepresentation();
        component.getConfig().putSingle(Attributes.ACTIVE_KEY, String.valueOf(false));
        managedRealm.admin().components().component(encKey.getProviderId()).update(component);

        byte[] content = createAndSignRequestObject().getBytes();

        String requestWithKid = encryptRequestObject(
                new JWEHeader(JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256, null, encKey.getKid()), encKey.getKid(), content);
        oauth.loginForm().request(requestWithKid).open();
        loginPage.assertCurrent();

        String requestWithoutKid = encryptRequestObject(
                new JWEHeader(JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256, null), encKey.getKid(), content);
        oauth.loginForm().request(requestWithoutKid).open();
        assertInvalidRequest();
    }

    /**
     * When a client requires signed request objects, a JWE-encrypted request object whose
     * decrypted content is raw JSON (not a nested JWS) must be rejected.
     */
    @Test
    public void testJweWithUnsignedJsonShouldBeRejectedWhenSignatureRequired() throws Exception {
        updateClientConfig(config -> config.setRequestObjectSignatureAlg(Algorithm.RS256));

        Map<String, Object> requestObject = createRequestObjectClaims();
        requestObject.put(OIDCLoginProtocol.NONCE_PARAM, KeycloakModelUtils.generateId());

        String request = encryptRequestObject(new JWEHeader(JWEConstants.RSA_OAEP, JWEConstants.A256GCM, null), null,
                JsonSerialization.writeValueAsBytes(requestObject));
        oauth.loginForm().request(request).open();

        assertInvalidRequest();
    }

    // Helpers

    private long loginAndGetAuthTime() {
        oauth.doLogin(USERNAME, PASSWORD);
        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent);

        // Check that authTime is available and set to current time
        long authTime = sendTokenRequestAndGetIDToken(loginEvent).getAuth_time();
        long currentTime = Time.currentTime();
        Assertions.assertTrue(authTime <= currentTime && authTime + 3 >= currentTime);
        return authTime;
    }

    private long pollLoginAndGetAuthTime() {
        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent);
        return sendTokenRequestAndGetIDToken(loginEvent).getAuth_time();
    }

    private IDToken sendTokenRequestAndGetIDToken(EventRepresentation loginEvent) {
        return oauth.verifyIDToken(sendTokenRequestAndGetResponse(loginEvent).getIdToken());
    }

    private AccessTokenResponse sendTokenRequestAndGetResponse(EventRepresentation loginEvent) {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        Assertions.assertEquals(200, response.getStatusCode());
        EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(loginEvent.getSessionId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID))
                .details(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID);
        return response;
    }

    private void logout() {
        String code = oauth.parseLoginResponse().getCode();
        String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        oauth.logoutForm().idTokenHint(idTokenHint).open();
    }

    private void assertRequestObjectEncryptionConfig(String expectedAlg, String expectedEnc) {
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(oauth.clientResource().toRepresentation());
        Assertions.assertEquals(expectedAlg, config.getRequestObjectEncryptionAlg());
        Assertions.assertEquals(expectedEnc, config.getRequestObjectEncryptionEnc());
    }

    private void assertConsentLoginEvent(String expectedConsent) {
        EventAssertion.expectLoginSuccess(events.poll())
                .details(Details.USERNAME, USERNAME)
                .details(Details.CONSENT, expectedConsent)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri());
    }

    private void assertLoginSuccess(AuthorizationEndpointResponse response, String expectedState) {
        Assertions.assertNotNull(response.getCode());
        Assertions.assertEquals(expectedState, response.getState());
    }

    private void assertInvalidRequest() {
        errorPage.assertCurrent();
        Assertions.assertEquals("Invalid Request", errorPage.getError());
    }

    private void assertInvalidRequestResponse(String expectedState) {
        Assertions.assertTrue(oauth.parseLoginResponse().isError());
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        Assertions.assertEquals(expectedState, response.getState());
    }

    private void loginAndAssertClaimsInSession(String claimsJson) {
        // need to login so session id can be read from event
        authenticatePassword(USERNAME, PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).details(Details.USERNAME, USERNAME);
        String sessionId = loginEvent.getSessionId();
        String clientId = loginEvent.getClientId();

        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            String clientUuid = realmModel.getClientByClientId(clientId).getId();
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, sessionId);

            String claimsInSession = userSession.getAuthenticatedClientSessionByClient(clientUuid).getNote(OIDCLoginProtocol.CLAIMS_PARAM);
            Assertions.assertEquals(claimsJson, claimsInSession);
        });
    }

    private void updateClient(Consumer<ClientRepresentation> update) {
        ClientResource clientResource = oauth.clientResource();
        ClientRepresentation clientRep = clientResource.toRepresentation();
        update.accept(clientRep);
        clientResource.update(clientRep);
    }

    private void updateClientConfig(Consumer<OIDCAdvancedConfigWrapper> update) {
        updateClient(clientRep -> update.accept(OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep)));
    }

    private void requireConsent() {
        updateClient(clientRep -> clientRep.setConsentRequired(true));
    }

    private void setRequestUris(String... requestUris) {
        List<String> uris = requestUris.length == 0 ? List.of(CLEAR_ATTRIBUTE) : Arrays.asList(requestUris);
        updateClientConfig(config -> config.setRequestUris(uris));
    }

    private void setRequestObjectRequired(String requestObjectRequired) {
        updateClientConfig(config -> config.setRequestObjectRequired(requestObjectRequired));
    }

    private void requestUriParamSignedIn(String expectedAlgorithm, String actualAlgorithm) {
        requestUriParamSignedIn(expectedAlgorithm, actualAlgorithm, null);
    }

    private void requestUriParamSignedIn(String expectedAlgorithm, String actualAlgorithm, String curve) {
        setOidcRequest(oauth.getRedirectUri(), "10", "mystate3", actualAlgorithm, curve);

        // Set required signature for request_uri and use jwks_url
        updateClientConfig(config -> {
            config.setRequestObjectSignatureAlg(expectedAlgorithm);
            config.setUseJwksUrl(true);
            config.setJwksUrl(jwksProvider.getUri());
            config.setRequestUris(List.of(oidcClientApplicationProvider.getUri()));
        });

        // set time offset so that new keys are downloaded
        timeOffSet.set(20);

        if (expectedAlgorithm == null || expectedAlgorithm.equals(actualAlgorithm)) {
            // Check signed request_uri will pass
            AuthorizationEndpointResponse response = oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).doLogin(USERNAME, PASSWORD);
            assertLoginSuccess(response, "mystate3");
        } else {
            // Verify signed request_uri will fail due to failed signature validation
            oauth.loginForm().requestUri(oidcClientApplicationProvider.getUri()).open();
            assertInvalidRequest();
        }
    }

    private String createOidcRequest(String redirectUri, String maxAge, String state) {
        return oidcClientApplicationProvider.createRequest(createOidcRequestOverrides(redirectUri, maxAge, state));
    }

    private Map<String, Object> createOidcRequestOverrides(String redirectUri, String maxAge, String state) {
        Map<String, Object> oidcRequest = new HashMap<>();
        oidcRequest.put(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);

        if (state != null) {
            oidcRequest.put(OIDCLoginProtocol.STATE_PARAM, state);
        }

        if (maxAge != null) {
            oidcRequest.put(OIDCLoginProtocol.MAX_AGE_PARAM, Integer.parseInt(maxAge));
        }

        return oidcRequest;
    }

    private void setOidcRequest(String redirectUri, String maxAge, String state, String algorithm) {
        setOidcRequest(redirectUri, maxAge, state, algorithm, null);
    }

    private void setOidcRequest(String redirectUri, String maxAge, String state, String algorithm, String curve) {
        Map<String, Object> overrides = createOidcRequestOverrides(redirectUri, maxAge, state);
        if (algorithm == null || "none".equals(algorithm)) {
            oidcClientApplicationProvider.setRequest(overrides);
        } else {
            // Replace the previous key so the JWKS endpoint publishes the key used for this request.
            jwksProvider.generateKeys(algorithm, curve, true, false, null);
            Map<String, Object> request = baseRequestClaims(redirectUri);
            request.putAll(overrides);
            oidcClientApplicationProvider.setOidcRequest(request, algorithm);
        }
    }

    private String createUnsignedClaimsRequest(Map<String, Object> claims) {
        return oidcClientApplicationProvider.createRequest(Map.of(
                OIDCLoginProtocol.REDIRECT_URI_PARAM, oauth.getRedirectUri(),
                OIDCLoginProtocol.CLAIMS_PARAM, claims));
    }

    private Map<String, Object> createRequestObjectClaims() {
        Map<String, Object> requestObject = baseRequestClaims(oauth.getRedirectUri());
        requestObject.put("jti", KeycloakModelUtils.generateId());
        long iat = Time.currentTime();
        requestObject.put("iat", iat);
        requestObject.put("exp", iat + 300);
        requestObject.put("nbf", iat);
        return requestObject;
    }

    private Map<String, Object> baseRequestClaims(String redirectUri) {
        Map<String, Object> claims = new HashMap<>(new OIDCAdvancedRequestParamsClientApplicationProviderConfig().build().getClaims());
        claims.put(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        return claims;
    }

    private String createAndSignRequestObject() {
        // use and set jwks_url
        updateClientConfig(config -> {
            config.setUseJwksUrl(true);
            config.setJwksUrl(jwksProvider.getUri());
        });

        jwksProvider.generateKeys(Algorithm.RS256, null, true, false, null);
        return oidcClientApplicationProvider.signRequestObject(createRequestObjectClaims(), Algorithm.RS256);
    }

    private String createEncryptedRequestObject(String encAlg) throws IOException, JWEException {
        return encryptRequestObject(new JWEHeader(encAlg, JWEConstants.A256GCM, null), null, createAndSignRequestObject().getBytes());
    }

    private void assertRequestObjectEncryption(JWEHeader jweHeader) throws IOException, JWEException {
        byte[] content = JsonSerialization.writeValueAsBytes(createRequestObjectClaims());

        oauth.loginForm().request(encryptRequestObject(jweHeader, jweHeader.getKeyId(), content)).doLogin(USERNAME, PASSWORD);
        EventAssertion.expectLoginSuccess(events.poll());
    }

    /**
     * @param publicKeyKid kid of the realm encryption key to use, or {@code null} to use the active key for the algorithm of the header
     */
    private String encryptRequestObject(JWEHeader header, String publicKeyKid, byte[] content) throws JWEException {
        String kid = publicKeyKid != null
                ? publicKeyKid
                : KeyUtils.findActiveEncryptingKey(managedRealm.admin(), header.getAlgorithm()).getKid();

        JWE jwe = new JWE().header(header).content(content);
        jwe.getKeyStorage().setEncryptionKey(JWKSUtils.getKeysForUse(oauth.keys().getRealmKeys(), JWK.Use.ENCRYPTION).get(kid));
        return jwe.encodeJwe();
    }

    public static class OIDCAdvancedRequestParamsClientApplicationProviderConfig implements OIDCClientApplicationProviderConfig {

        @Override
        public OIDCClientApplicationProviderConfiguration build() {
            OIDCClientApplicationProviderConfiguration config = new OIDCClientApplicationProviderConfiguration();
            config.setClientId(CLIENT_ID);
            config.setResponseType(OAuth2Constants.CODE);
            config.setScope("openid");
            config.setSigningAlgorithm("none");
            return config;
        }
    }

    private static class OIDCAdvancedRequestParamsRealmConfig implements RealmConfig {

        private static ComponentExportRepresentation createEncryptionKey(String name, String priority, String algorithm) {
            ComponentExportRepresentation key = new ComponentExportRepresentation();
            key.setName(name);
            key.setProviderId("rsa-enc-generated");
            key.setConfig(new MultivaluedHashMap<>());
            key.getConfig().putSingle("priority", priority);
            key.getConfig().putSingle(Attributes.KEY_USE, KeyUse.ENC.getSpecName());
            key.getConfig().putSingle("algorithm", algorithm);
            return key;
        }

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            ComponentExportRepresentation rsaOaepKey = createEncryptionKey("enc-generated", "150", JWEConstants.RSA_OAEP);
            ComponentExportRepresentation rsaOaep256Key = createEncryptionKey("enc-generated-256", "200", JWEConstants.RSA_OAEP_256);

            MultivaluedHashMap<String, ComponentExportRepresentation> components = new MultivaluedHashMap<>();
            components.add(KeyProvider.class.getName(), rsaOaepKey);
            components.add(KeyProvider.class.getName(), rsaOaep256Key);

            return realm.users(UserBuilder.create(USERNAME)
                    .password(PASSWORD)
                    .email(USERNAME)
                    .name("Tom", "Brady"))
                    .clients(ClientBuilder.create("test-app-scope")
                            .secret("password")
                                .redirectUris("*"))
                    .registrationAllowed(true)
                    .enabledEventTypes(EventType.LOGIN.name(), EventType.LOGOUT.name(), EventType.CODE_TO_TOKEN.name(), EventType.USER_INFO_REQUEST.name())
                    .update(rep -> rep.setComponents(components));
        }
    }
}
