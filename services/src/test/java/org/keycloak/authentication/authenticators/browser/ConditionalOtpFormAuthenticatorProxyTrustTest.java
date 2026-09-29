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
package org.keycloak.authentication.authenticators.browser;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.Test;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationSelectionOption;
import org.keycloak.authentication.FlowStatus;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.FormPartValue;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_FOR_HTTP_HEADER;

/**
 * Unit tests for the proxy-trust guard in {@link ConditionalOtpFormAuthenticator}.
 *
 * <p>The skip-OTP header rule must only be honoured when the request arrives via a trusted proxy
 * ({@link HttpRequest#isProxyTrusted()} returns {@code true}). Without this guard an attacker who
 * already knows a victim's password can inject a matching {@code X-Forwarded-Host} header directly
 * in the browser-flow credential POST and bypass the second factor entirely (Issue #1179).
 *
 * <p>The force-OTP header rule does not require a proxy-trust gate because forcing OTP is
 * conservative — it can only make authentication stricter, never weaker.
 */
public class ConditionalOtpFormAuthenticatorProxyTrustTest {

    private static final String SKIP_PATTERN  = "X-Forwarded-Host: (1\\.2\\.3\\.4|1\\.2\\.3\\.5)";
    private static final String FORCE_PATTERN = "X-Forwarded-For: 10\\.0\\.0\\..*";

    /**
     * When the proxy is NOT trusted, a matching skip-OTP header must be ignored.
     * The authenticator must NOT call {@code context.success()} — OTP is not skipped.
     */
    @Test
    public void skipHeader_untrustedProxy_otpIsNotSkipped() {
        StubContext ctx = run(skipConfig(SKIP_PATTERN),
                header("X-Forwarded-Host", "1.2.3.4"), /*proxyTrusted=*/false);

        assertThat("OTP must not be skipped when proxy is untrusted", ctx.wasSuccessCalled(), is(false));
    }

    /**
     * When the proxy IS trusted and the header matches, {@code context.success()} must be called.
     */
    @Test
    public void skipHeader_trustedProxy_matchingHeader_otpIsSkipped() {
        StubContext ctx = run(skipConfig(SKIP_PATTERN),
                header("X-Forwarded-Host", "1.2.3.4"), /*proxyTrusted=*/true);

        assertThat("OTP must be skipped when proxy is trusted and header matches", ctx.wasSuccessCalled(), is(true));
    }

    /**
     * Trusted proxy but header value does NOT match — skip rule must not fire.
     */
    @Test
    public void skipHeader_trustedProxy_nonMatchingHeader_otpIsNotSkipped() {
        StubContext ctx = run(skipConfig(SKIP_PATTERN),
                header("X-Forwarded-Host", "9.9.9.9"), /*proxyTrusted=*/true);

        assertThat("OTP must not be skipped when header value does not match", ctx.wasSuccessCalled(), is(false));
    }

    /**
     * Trusted proxy, skip pattern configured, but no matching header present — must abstain.
     */
    @Test
    public void skipHeader_trustedProxy_noHeader_otpIsNotSkipped() {
        StubContext ctx = run(skipConfig(SKIP_PATTERN),
                emptyHeaders(), /*proxyTrusted=*/true);

        assertThat("OTP must not be skipped when no matching header is present", ctx.wasSuccessCalled(), is(false));
    }

    /**
     * Force-OTP rule must fire regardless of proxy trust on untrusted path — OTP form is shown.
     */
    @Test
    public void forceHeader_untrustedProxy_matchingHeader_otpIsForced() {
        StubContext ctx = run(forceConfig(FORCE_PATTERN),
                header("X-Forwarded-For", "10.0.0.42"), /*proxyTrusted=*/false);

        assertThat("success() must not be called when force-OTP header matches", ctx.wasSuccessCalled(), is(false));
        assertThat("challenge() must be called to show the OTP form", ctx.wasChallengeCalled(), is(true));
    }

    /**
     * Force-OTP rule must also fire when proxy IS trusted.
     */
    @Test
    public void forceHeader_trustedProxy_matchingHeader_otpIsForced() {
        StubContext ctx = run(forceConfig(FORCE_PATTERN),
                header("X-Forwarded-For", "10.0.0.1"), /*proxyTrusted=*/true);

        assertThat("success() must not be called when force-OTP header matches", ctx.wasSuccessCalled(), is(false));
        assertThat("challenge() must be called to show the OTP form", ctx.wasChallengeCalled(), is(true));
    }

    /**
     * With no header config at all the header-check step abstains and the OTP form is shown.
     */
    @Test
    public void noHeaderConfig_otpFormShown() {
        StubContext ctx = run(Collections.emptyMap(),
                header("X-Forwarded-Host", "1.2.3.4"), /*proxyTrusted=*/false);

        assertThat("success() must not be called when no header config is present", ctx.wasSuccessCalled(), is(false));
        assertThat("challenge() must be called (OTP form shown by default)", ctx.wasChallengeCalled(), is(true));
    }

    private static Map<String, String> skipConfig(String pattern) {
        Map<String, String> config = new HashMap<>();
        config.put(SKIP_OTP_FOR_HTTP_HEADER, pattern);
        return config;
    }

    private static Map<String, String> forceConfig(String pattern) {
        Map<String, String> config = new HashMap<>();
        config.put(FORCE_OTP_FOR_HTTP_HEADER, pattern);
        return config;
    }

    private static MultivaluedMap<String, String> header(String name, String value) {
        MultivaluedHashMap<String, String> h = new MultivaluedHashMap<>();
        h.add(name, value);
        return h;
    }

    private static MultivaluedMap<String, String> emptyHeaders() {
        return new MultivaluedHashMap<>();
    }

    private StubContext run(Map<String, String> config,
                            MultivaluedMap<String, String> requestHeaders,
                            boolean proxyTrusted) {
        StubContext ctx = new StubContext(new StubHttpRequest(requestHeaders, proxyTrusted), config);
        new ConditionalOtpFormAuthenticator().authenticate(ctx);
        return ctx;
    }

    /** Minimal {@link HttpRequest} — only isProxyTrusted() and getHttpHeaders() are consulted. */
    private static class StubHttpRequest implements HttpRequest {
        private final MultivaluedMap<String, String> headers;
        private final boolean proxyTrusted;

        StubHttpRequest(MultivaluedMap<String, String> headers, boolean proxyTrusted) {
            this.headers = headers;
            this.proxyTrusted = proxyTrusted;
        }

        @Override public boolean isProxyTrusted() { return proxyTrusted; }

        @Override
        public HttpHeaders getHttpHeaders() {
            return new HttpHeaders() {
                @Override public MultivaluedMap<String, String> getRequestHeaders() { return headers; }
                @Override public List<String> getRequestHeader(String name) { return headers.getOrDefault(name, Collections.emptyList()); }
                @Override public String getHeaderString(String name) { List<String> v = headers.get(name); return v != null && !v.isEmpty() ? v.get(0) : null; }
                @Override public Map<String, jakarta.ws.rs.core.Cookie> getCookies() { return Collections.emptyMap(); }
                @Override public MediaType getMediaType() { return null; }
                @Override public List<MediaType> getAcceptableMediaTypes() { return Collections.emptyList(); }
                @Override public List<java.util.Locale> getAcceptableLanguages() { return Collections.emptyList(); }
                @Override public java.util.Locale getLanguage() { return null; }
                @Override public java.util.Date getDate() { return null; }
                @Override public int getLength() { return -1; }
            };
        }

        @Override public String getHttpMethod() { return "POST"; }
        @Override public MultivaluedMap<String, String> getDecodedFormParameters() { return new MultivaluedHashMap<>(); }
        @Override public MultivaluedMap<String, FormPartValue> getMultiPartFormParameters() { return new MultivaluedHashMap<>(); }
        @Override public X509Certificate[] getClientCertificateChain() { return null; }
        @Override public UriInfo getUri() { return null; }
    }

    /**
     * Records whether {@code success()} or {@code challenge()} was called.
     * Supplies the authenticator config and the stubs needed to reach each branch.
     */
    private static class StubContext implements AuthenticationFlowContext {
        private final StubHttpRequest httpRequest;
        private final AuthenticatorConfigModel configModel;
        private boolean successCalled;
        private boolean challengeCalled;

        StubContext(StubHttpRequest httpRequest, Map<String, String> config) {
            this.httpRequest = httpRequest;
            AuthenticatorConfigModel model = new AuthenticatorConfigModel();
            model.setConfig(config);
            this.configModel = model;
        }

        boolean wasSuccessCalled()   { return successCalled; }
        boolean wasChallengeCalled() { return challengeCalled; }

        // -- observed outcomes --
        @Override public void success()                        { successCalled = true; }
        @Override public void success(String credentialType)   { successCalled = true; }
        @Override public void challenge(Response r)            { challengeCalled = true; }
        @Override public void forceChallenge(Response r)       { challengeCalled = true; }
        @Override public void failureChallenge(AuthenticationFlowError e, Response r) { challengeCalled = true; }

        // -- required by authenticate() path --
        @Override public HttpRequest getHttpRequest()          { return httpRequest; }
        @Override public AuthenticatorConfigModel getAuthenticatorConfig() { return configModel; }
        @Override public UserModel getUser()                   { return new StubUser(); }
        @Override public RealmModel getRealm()                  { return null; }
        @Override public KeycloakSession getSession()           { return null; }

        @Override public AuthenticationExecutionModel getExecution() {
            AuthenticationExecutionModel exec = new AuthenticationExecutionModel();
            exec.setId("stub-execution-id");
            return exec;
        }

        @Override public AuthenticationSessionModel getAuthenticationSession() {
            return new StubAuthenticationSession();
        }

        @Override public LoginFormsProvider form() { return new StubLoginFormsProvider(); }

        // -- no-op / null for everything else --
        @Override public EventBuilder getEvent()                { return null; }
        @Override public EventBuilder newEvent()                { return null; }
        @Override public AuthenticationFlowModel getTopLevelFlow() { return null; }
        @Override public ClientConnection getConnection()       { return null; }
        @Override public UriInfo getUriInfo()                   { return null; }
        @Override public BruteForceProtector getProtector()     { return null; }
        @Override public FormMessage getForwardedErrorMessage() { return null; }
        @Override public FormMessage getForwardedSuccessMessage() { return null; }
        @Override public FormMessage getForwardedInfoMessage()  { return null; }
        @Override public void setForwardedInfoMessage(String m, Object... p) {}
        @Override public String generateAccessCode()            { return null; }
        @Override public AuthenticationExecutionModel.Requirement getCategoryRequirementFromCurrentFlow(String c) { return null; }
        @Override public void failure(AuthenticationFlowError e) {}
        @Override public void failure(AuthenticationFlowError e, Response r) {}
        @Override public void failure(AuthenticationFlowError e, Response r, String d, String u) {}
        @Override public void attempted()                       {}
        @Override public FlowStatus getStatus()                 { return null; }
        @Override public AuthenticationFlowError getError()     { return null; }
        @Override public String getEventDetails()               { return null; }
        @Override public String getUserErrorMessage()           { return null; }
        @Override public void setUser(UserModel user)           {}
        @Override public List<AuthenticationSelectionOption> getAuthenticationSelections() { return Collections.emptyList(); }
        @Override public void setAuthenticationSelections(List<AuthenticationSelectionOption> l) {}
        @Override public void clearUser()                       {}
        @Override public void attachUserSession(UserSessionModel s) {}
        @Override public String getFlowPath()                   { return null; }
        @Override public URI getActionUrl(String code)          { return null; }
        @Override public URI getActionTokenUrl(String token)    { return null; }
        @Override public URI getRefreshExecutionUrl()           { return null; }
        @Override public URI getRefreshUrl(boolean p)           { return null; }
        @Override public void cancelLogin()                     {}
        @Override public void resetFlow()                       {}
        @Override public void resetFlow(Runnable r)             {}
        @Override public void fork()                            {}
        @Override public void forkWithSuccessMessage(FormMessage m) {}
        @Override public void forkWithErrorMessage(FormMessage m) {}
    }

    /** Fluent no-op stub for {@link LoginFormsProvider} — all setters return {@code this}. */
    private static class StubLoginFormsProvider implements LoginFormsProvider {
        @Override public LoginFormsProvider setExecution(String e)  { return this; }
        @Override public LoginFormsProvider setAttribute(String n, Object v) { return this; }
        @Override public LoginFormsProvider setError(String m, Object... p)  { return this; }
        @Override public LoginFormsProvider setErrors(List<FormMessage> ms)  { return this; }
        @Override public LoginFormsProvider addError(FormMessage m)          { return this; }
        @Override public LoginFormsProvider addSuccess(FormMessage m)        { return this; }
        @Override public LoginFormsProvider setSuccess(String m, Object... p){ return this; }
        @Override public LoginFormsProvider setInfo(String m, Object... p)   { return this; }
        @Override public LoginFormsProvider setMessage(org.keycloak.forms.login.MessageType t, String m, Object... p) { return this; }
        @Override public LoginFormsProvider setDetachedAuthSession()         { return this; }
        @Override public LoginFormsProvider setUser(UserModel u)             { return this; }
        @Override public LoginFormsProvider setResponseHeader(String n, String v) { return this; }
        @Override public LoginFormsProvider setFormData(MultivaluedMap<String, String> d) { return this; }
        @Override public LoginFormsProvider setStatus(Response.Status s)     { return this; }
        @Override public LoginFormsProvider setActionUri(URI u)              { return this; }
        @Override public LoginFormsProvider setAuthContext(AuthenticationFlowContext c) { return this; }
        @Override public LoginFormsProvider setAttributeMapper(Function<Map<String, Object>, Map<String, Object>> f) { return this; }
        @Override public LoginFormsProvider setClientSessionCode(String c)   { return this; }
        @Override public LoginFormsProvider setAccessRequest(List<AuthorizationDetails> l) { return this; }
        @Override public LoginFormsProvider setAuthenticationSession(AuthenticationSessionModel s) { return this; }

        @Override public Response createLoginTotp()           { return Response.ok().build(); }
        @Override public Response createForm(String form)     { return Response.ok().build(); }
        @Override public Response createResponse(UserModel.RequiredAction a) { return Response.ok().build(); }
        @Override public Response createLoginUsernamePassword() { return Response.ok().build(); }
        @Override public Response createLoginUsername()       { return Response.ok().build(); }
        @Override public Response createLoginPassword()       { return Response.ok().build(); }
        @Override public Response createOtpReset()            { return Response.ok().build(); }
        @Override public Response createPasswordReset()       { return Response.ok().build(); }
        @Override public Response createLoginRecoveryAuthnCode() { return Response.ok().build(); }
        @Override public Response createLoginWebAuthn()       { return Response.ok().build(); }
        @Override public Response createRegistration()        { return Response.ok().build(); }
        @Override public Response createInfoPage()            { return Response.ok().build(); }
        @Override public Response createUpdateProfilePage()   { return Response.ok().build(); }
        @Override public Response createIdpLinkConfirmLinkPage() { return Response.ok().build(); }
        @Override public Response createIdpLinkConfirmOverrideLinkPage() { return Response.ok().build(); }
        @Override public Response createIdpLinkEmailPage()    { return Response.ok().build(); }
        @Override public Response createLoginExpiredPage()    { return Response.ok().build(); }
        @Override public Response createErrorPage(Response.Status s) { return Response.ok().build(); }
        @Override public Response createWebAuthnErrorPage()   { return Response.ok().build(); }
        @Override public Response createOAuthGrant()          { return Response.ok().build(); }
        @Override public Response createSelectAuthenticator() { return Response.ok().build(); }
        @Override public Response createOAuth2DeviceVerifyUserCodePage() { return Response.ok().build(); }
        @Override public Response createCode()                { return Response.ok().build(); }
        @Override public Response createX509ConfirmPage()     { return Response.ok().build(); }
        @Override public Response createSamlPostForm()        { return Response.ok().build(); }
        @Override public Response createFrontChannelLogoutPage() { return Response.ok().build(); }
        @Override public Response createLogoutConfirmPage()   { return Response.ok().build(); }
        @Override public void addScript(String url)           {}
        @Override public String getMessage(String m, Object... p) { return m; }
        @Override public void close()                         {}
    }

    /** Minimal stub for {@link AuthenticationSessionModel} — only {@code getAuthNote()} is called. */
    private static class StubAuthenticationSession implements AuthenticationSessionModel {
        @Override public String getAuthNote(String n) { return null; }
        @Override public void setAuthNote(String n, String v) {}
        @Override public void removeAuthNote(String n) {}
        @Override public void clearAuthNotes() {}
        @Override public String getRedirectUri() { return null; }
        @Override public void setRedirectUri(String u) {}
        @Override public RealmModel getRealm() { return null; }
        @Override public ClientModel getClient() { return null; }
        @Override public String getTabId() { return null; }
        @Override public org.keycloak.sessions.RootAuthenticationSessionModel getParentSession() { return null; }
        @Override public Map<String, org.keycloak.sessions.CommonClientSessionModel.ExecutionStatus> getExecutionStatus() { return Collections.emptyMap(); }
        @Override public void setExecutionStatus(String authExecId, org.keycloak.sessions.CommonClientSessionModel.ExecutionStatus status) {}
        @Override public void clearExecutionStatus() {}
        @Override public UserModel getAuthenticatedUser() { return null; }
        @Override public void setAuthenticatedUser(UserModel user) {}
        @Override public Set<String> getRequiredActions() { return Collections.emptySet(); }
        @Override public void addRequiredAction(String action) {}
        @Override public void removeRequiredAction(String action) {}
        @Override public void addRequiredAction(UserModel.RequiredAction action) {}
        @Override public void removeRequiredAction(UserModel.RequiredAction action) {}
        @Override public void setUserSessionNote(String n, String v) {}
        @Override public Map<String, String> getUserSessionNotes() { return Collections.emptyMap(); }
        @Override public void clearUserSessionNotes() {}
        @Override public String getClientNote(String n) { return null; }
        @Override public void setClientNote(String n, String v) {}
        @Override public void removeClientNote(String n) {}
        @Override public void clearClientNotes() {}
        @Override public Map<String, String> getClientNotes() { return Collections.emptyMap(); }
        @Override public String getAction() { return null; }
        @Override public void setAction(String action) {}
        @Override public Set<String> getClientScopes() { return Collections.emptySet(); }
        @Override public void setClientScopes(Set<String> clientScopes) {}
        @Override public String getProtocol() { return null; }
        @Override public void setProtocol(String method) {}
    }

    /** Minimal stub for {@link UserModel} — only getAttributeStream() is consulted. */
    private static class StubUser implements UserModel {
        @Override public Stream<String> getAttributeStream(String name) { return Stream.empty(); }
        @Override public String getId()                        { return null; }
        @Override public String getUsername()                  { return null; }
        @Override public void setUsername(String u)            {}
        @Override public Long getCreatedTimestamp()            { return null; }
        @Override public void setCreatedTimestamp(Long t)      {}
        @Override public boolean isEnabled()                   { return true; }
        @Override public void setEnabled(boolean e)            {}
        @Override public void setSingleAttribute(String n, String v) {}
        @Override public void setAttribute(String n, List<String> v) {}
        @Override public void removeAttribute(String n)        {}
        @Override public String getFirstAttribute(String n)    { return null; }
        @Override public Map<String, List<String>> getAttributes() { return Collections.emptyMap(); }
        @Override public Stream<String> getRequiredActionsStream() { return Stream.empty(); }
        @Override public void addRequiredAction(String a)      {}
        @Override public void removeRequiredAction(String a)   {}
        @Override public void addRequiredAction(RequiredAction a) {}
        @Override public void removeRequiredAction(RequiredAction a) {}
        @Override public String getFirstName()                 { return null; }
        @Override public void setFirstName(String f)           {}
        @Override public String getLastName()                  { return null; }
        @Override public void setLastName(String l)            {}
        @Override public String getEmail()                     { return null; }
        @Override public void setEmail(String e)               {}
        @Override public boolean isEmailVerified()             { return false; }
        @Override public void setEmailVerified(boolean v)      {}
        @Override public Stream<GroupModel> getGroupsStream()  { return Stream.empty(); }
        @Override public void joinGroup(GroupModel g)          {}
        @Override public void leaveGroup(GroupModel g)         {}
        @Override public boolean isMemberOf(GroupModel g)      { return false; }
        @Override public String getFederationLink()            { return null; }
        @Override public void setFederationLink(String l)      {}
        @Override public String getServiceAccountClientLink()  { return null; }
        @Override public void setServiceAccountClientLink(String l) {}
        @Override public Stream<RoleModel> getRealmRoleMappingsStream() { return Stream.empty(); }
        @Override public Stream<RoleModel> getClientRoleMappingsStream(ClientModel c) { return Stream.empty(); }
        @Override public boolean hasRole(RoleModel r)          { return false; }
        @Override public void grantRole(RoleModel r)           {}
        @Override public Stream<RoleModel> getRoleMappingsStream() { return Stream.empty(); }
        @Override public void deleteRoleMapping(RoleModel r)   {}
        @Override public SubjectCredentialManager credentialManager() { return null; }
    }
}
