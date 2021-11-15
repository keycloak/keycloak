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

package org.keycloak.testsuite.rest;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.HtmlUtils;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.rest.representation.TestAuthenticationChannelRequest;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.utils.MediaType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class TestApplicationResourceProvider implements RealmResourceProvider {

    private KeycloakSession session;

    private final BlockingQueue<LogoutAction> adminLogoutActions;
    private final BlockingQueue<LogoutToken> frontChannelLogoutTokens;
    private final BlockingQueue<LogoutToken> backChannelLogoutTokens;
    private final BlockingQueue<PushNotBeforeAction> adminPushNotBeforeActions;
    private final BlockingQueue<TestAvailabilityAction> adminTestAvailabilityAction;
    private final TestApplicationResourceProviderFactory.OIDCClientData oidcClientData;

    private final ConcurrentMap<String, TestAuthenticationChannelRequest> authenticationChannelRequests;
    private final ConcurrentMap<String, ClientNotificationEndpointRequest> cibaClientNotifications;

    @Context
    HttpRequest request;

    public TestApplicationResourceProvider(KeycloakSession session, BlockingQueue<LogoutAction> adminLogoutActions,
            BlockingQueue<LogoutToken> backChannelLogoutTokens,
            BlockingQueue<LogoutToken> frontChannelLogoutTokens,
            BlockingQueue<PushNotBeforeAction> adminPushNotBeforeActions,
            BlockingQueue<TestAvailabilityAction> adminTestAvailabilityAction,
            TestApplicationResourceProviderFactory.OIDCClientData oidcClientData,
            ConcurrentMap<String, TestAuthenticationChannelRequest> authenticationChannelRequests,
            ConcurrentMap<String, ClientNotificationEndpointRequest> cibaClientNotifications) {
        this.session = session;
        this.adminLogoutActions = adminLogoutActions;
        this.backChannelLogoutTokens = backChannelLogoutTokens;
        this.frontChannelLogoutTokens = frontChannelLogoutTokens;
        this.adminPushNotBeforeActions = adminPushNotBeforeActions;
        this.adminTestAvailabilityAction = adminTestAvailabilityAction;
        this.oidcClientData = oidcClientData;
        this.authenticationChannelRequests = authenticationChannelRequests;
        this.cibaClientNotifications = cibaClientNotifications;
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Path("/admin/k_logout")
    public void adminLogout(String data) throws JWSInputException {
        adminLogoutActions.add(new JWSInput(data).readJsonContent(LogoutAction.class));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/admin/backchannelLogout")
    public void backchannelLogout() throws JWSInputException {
        backChannelLogoutTokens.add(new JWSInput(request.getDecodedFormParameters().getFirst(OAuth2Constants.LOGOUT_TOKEN)).readJsonContent(LogoutToken.class));
    }

    @GET
    @Path("/admin/frontchannelLogout")
    public void frontchannelLogout(@QueryParam("sid") String sid, @QueryParam("iss") String issuer) {
        LogoutToken token = new LogoutToken();
        token.setSid(sid);
        token.issuer(issuer);
        frontChannelLogoutTokens.add(token);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Path("/admin/k_push_not_before")
    public void adminPushNotBefore(String data) throws JWSInputException {
        adminPushNotBeforeActions.add(new JWSInput(data).readJsonContent(PushNotBeforeAction.class));
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Path("/admin/k_test_available")
    public void testAvailable(String data) throws JWSInputException {
        adminTestAvailabilityAction.add(new JWSInput(data).readJsonContent(TestAvailabilityAction.class));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-admin-logout")
    public LogoutAction getAdminLogoutAction() throws InterruptedException {
        return adminLogoutActions.poll(10, TimeUnit.SECONDS);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-backchannel-logout")
    public LogoutToken getBackChannelLogoutAction() throws InterruptedException {
        return backChannelLogoutTokens.poll(20, TimeUnit.SECONDS);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-frontchannel-logout")
    public LogoutToken getFrontChannelLogoutAction() throws InterruptedException {
        return frontChannelLogoutTokens.poll(20, TimeUnit.SECONDS);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-admin-not-before")
    public PushNotBeforeAction getAdminPushNotBefore() throws InterruptedException {
        return adminPushNotBeforeActions.poll(10, TimeUnit.SECONDS);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-test-available")
    public TestAvailabilityAction getTestAvailable() throws InterruptedException {
        return adminTestAvailabilityAction.poll(10, TimeUnit.SECONDS);
    }

    @POST
    @Path("/clear-admin-actions")
    public Response clearAdminActions() {
        adminLogoutActions.clear();
        adminPushNotBeforeActions.clear();
        return Response.noContent().build();
    }

    @POST
    @Consumes(javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML_UTF_8)
    @Path("/{action}")
    public Response post(@PathParam("action") String action) {
        if ("clear-admin-actions".equals(action)) {
            return clearAdminActions();
        }
        MultivaluedMap<String, String> formParams = request.getDecodedFormParameters();
        String title = "APP_REQUEST";
        if (action.equals("auth")) {
            title = "AUTH_RESPONSE";
        } else if (action.equals("logout")) {
            title = "LOGOUT_REQUEST";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>" + title + "</title></head><body>");

        sb.append("<b>Form parameters: </b><br>");
        for (String paramName : formParams.keySet()) {
            sb.append(paramName).append(": ").append("<span id=\"")
                    .append(paramName).append("\">")
                    .append(HtmlUtils.escapeAttribute(formParams.getFirst(paramName)))
                    .append("</span><br>");
        }
        sb.append("<br>");

        UriBuilder base = UriBuilder.fromUri("/auth");
        sb.append("<a href=\"" + RealmsResource.accountUrl(base).build("test").toString() + "\" id=\"account\">account</a>");

        sb.append("</body></html>");
        return Response.ok(sb.toString()).build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML_UTF_8)
    @Path("/{action}")
    public String get(@PathParam("action") String action) {
        //String requestUri = session.getContext().getUri().getRequestUri().toString();

        String title = "APP_REQUEST";
         if (action.equals("auth")) {
            title = "AUTH_RESPONSE";
        } else if (action.equals("logout")) {
            title = "LOGOUT_REQUEST";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>" + title + "</title></head><body>");
        UriBuilder base = UriBuilder.fromUri("/auth");
        sb.append("<a href=\"" + RealmsResource.accountUrl(base).build("test").toString() + "\" id=\"account\">account</a>");

        sb.append("</body></html>");
        return sb.toString();
    }

    @GET
    @NoCache
    @Produces(MediaType.TEXT_HTML_UTF_8)
    @Path("/get-account-profile")
    public String getAccountProfile(@QueryParam("token") String token, @QueryParam("account-uri") String accountUri) {
        StringBuilder sb = new StringBuilder();
        sb.append("function getProfile() {\n");
        sb.append(" var req = new XMLHttpRequest();\n");
        sb.append(" req.open('GET', '" + accountUri + "', false);\n");
        if (token != null) {
            sb.append(" req.setRequestHeader('Authorization', 'Bearer " + token + "');\n");
        }
        sb.append(" req.setRequestHeader('Accept', 'application/json');\n");
        sb.append(" req.send(null);\n");
        sb.append(" document.getElementById('profileOutput').innerHTML=\"<span id='innerOutput'>\" + req.status + '///' + req.responseText; + \"</span>\"\n");
        sb.append("}");
        String jsScript = sb.toString();

        sb = new StringBuilder();
        sb.append("<html><head><title>Account Profile JS Test</title><script>\n")
                .append(jsScript)
                .append( "</script></head>\n")
                .append("<body onload='getProfile()'><div id='profileOutput'></div></body>")
                .append("</html>");
        return sb.toString();
    }

    @Path("/oidc-client-endpoints")
    public TestingOIDCEndpointsApplicationResource getTestingOIDCClientEndpoints() {
        return new TestingOIDCEndpointsApplicationResource(oidcClientData, authenticationChannelRequests, cibaClientNotifications);
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }
}
