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

package org.keycloak.testsuite.client.resources;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.components.TestProvider;
import org.keycloak.testsuite.rest.representation.AuthenticatorState;
import org.keycloak.utils.MediaType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import org.infinispan.commons.time.TimeService;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@Path("/testing")
@Consumes(MediaType.APPLICATION_JSON)
public interface TestingResource {

    @GET
    @Path("/time-offset")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> getTimeOffset();

    @PUT
    @Path("/time-offset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> setTimeOffset(Map<String, String> time);

    @POST
    @Path("/poll-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    EventRepresentation pollEvent();

    @POST
    @Path("/poll-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    AdminEventRepresentation pollAdminEvent();

    @POST
    @Path("/clear-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    void clearEventQueue();

    @POST
    @Path("/clear-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    void clearAdminEventQueue();

    @GET
    @Path("/clear-event-store")
    @Produces(MediaType.APPLICATION_JSON)
    void clearEventStore();

    @GET
    @Path("/clear-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    void clearEventStore(@QueryParam("realmId") String realmId);

    @GET
    @Path("/clear-expired-events")
    @Produces(MediaType.APPLICATION_JSON)
    void clearExpiredEvents();

    /**
     * Query events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param realmId     The realm
     * @param types       The types of events to return
     * @param client      App or oauth client name
     * @param user        User id
     * @param dateFrom    From date
     * @param dateTo      To date
     * @param ipAddress   IP address
     * @param firstResult Paging offset
     * @param maxResults  Paging size
     * @return
     */
    @Path("query-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventRepresentation> queryEvents(@QueryParam("realmId") String realmId, @QueryParam("type") List<String> types, @QueryParam("client") String client,
            @QueryParam("user") String user, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo,
            @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults);

    @PUT
    @Path("/on-event")
    @Consumes(MediaType.APPLICATION_JSON)
    public void onEvent(final EventRepresentation rep);

    @GET
    @Path("/clear-admin-event-store")
    @Produces(MediaType.APPLICATION_JSON)
    void clearAdminEventStore();

    @GET
    @Path("/clear-admin-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    void clearAdminEventStore(@QueryParam("realmId") String realmId);

    @GET
    @Path("/clear-admin-event-store-older-than")
    @Produces(MediaType.APPLICATION_JSON)
    void clearAdminEventStore(@QueryParam("realmId") String realmId, @QueryParam("olderThan") long olderThan);

    /**
     * Get admin events
     * <p>
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param realmId
     * @param operationTypes
     * @param authRealm
     * @param authClient
     * @param authUser user id
     * @param authIpAddress
     * @param resourcePath
     * @param dateFrom
     * @param dateTo
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Path("query-admin-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<AdminEventRepresentation> getAdminEvents(@QueryParam("realmId") String realmId, @QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
            @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
            @QueryParam("resourcePath") String resourcePath, @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults);

    @POST
    @Path("/on-admin-event")
    @Consumes(MediaType.APPLICATION_JSON)
    void onAdminEvent(final AdminEventRepresentation rep, @QueryParam("includeRepresentation") boolean includeRepresentation);

    @GET
    @Path("/get-sso-cookie")
    @Produces(MediaType.APPLICATION_JSON)
    String getSSOCookieValue();

    @POST
    @Path("/remove-user-session")
    @Produces(MediaType.APPLICATION_JSON)
    void removeUserSession(@QueryParam("realm") final String realm, @QueryParam("session") final String sessionId);

    @POST
    @Path("/remove-user-sessions")
    @Produces(MediaType.APPLICATION_JSON)
    void removeUserSessions(@QueryParam("realm") final String realm);

    @GET
    @Path("/get-last-session-refresh")
    @Produces(MediaType.APPLICATION_JSON)
    Integer getLastSessionRefresh(@QueryParam("realm") final String realm, @QueryParam("session") final String sessionId, @QueryParam("offline") boolean offline);

    @POST
    @Path("/remove-expired")
    @Produces(MediaType.APPLICATION_JSON)
    void removeExpired(@QueryParam("realm") final String realm);

    /**
     * Will set Inifispan's {@link TimeService} that is aware of Keycloak time shifts to the infinispan {@code CacheManager} before the test.
     * This will allow infinispan expiration to be aware of Keycloak {@link org.keycloak.common.util.Time#setOffset}
     */
    @POST
    @Path("/set-testing-infinispan-time-service")
    @Produces(MediaType.APPLICATION_JSON)
    void setTestingInfinispanTimeService();

    @POST
    @Path("/revert-testing-infinispan-time-service")
    @Produces(MediaType.APPLICATION_JSON)
    void revertTestingInfinispanTimeService();

    @GET
    @Path("/get-client-sessions-count")
    @Produces(MediaType.APPLICATION_JSON)
    Integer getClientSessionsCountInUserSession(@QueryParam("realm") final String realmName, @QueryParam("session") final String sessionId);

    @Path("/cache/{cache}")
    TestingCacheResource cache(@PathParam("cache") String cacheName);

    @Path("/ldap/{realm}")
    TestingLDAPResource ldap(@PathParam("realm") final String realmName);

    @POST
    @Path("/update-pass-through-auth-state")
    @Produces(MediaType.APPLICATION_JSON)
    AuthenticatorState updateAuthenticator(AuthenticatorState state);

    @GET
    @Path("/valid-credentials")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean validCredentials(@QueryParam("realmName") String realmName, @QueryParam("userName") String userName, @QueryParam("password") String password);

    @GET
    @Path("/user-by-federated-identity")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByFederatedIdentity(@QueryParam("realmName") String realmName,
                                                         @QueryParam("identityProvider") String identityProvider,
                                                         @QueryParam("userId") String userId,
                                                         @QueryParam("userName") String userName);

    @GET
    @Path("/user-by-username-from-fed-factory")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByUsernameFromFedProviderFactory(@QueryParam("realmName") String realmName,
                                                                      @QueryParam("userName") String userName);

    @GET
    @Path("/get-client-auth-flow")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowRepresentation getClientAuthFlow(@QueryParam("realmName") String realmName);

    @GET
    @Path("/get-reset-cred-flow")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowRepresentation getResetCredFlow(@QueryParam("realmName") String realmName);

    @GET
    @Path("/get-user-by-service-account-client")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByServiceAccountClient(@QueryParam("realmName") String realmName, @QueryParam("clientId") String clientId);

    @Path("export-import")
    TestingExportImportResource exportImport();

    @GET
    @Path("/test-component")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, TestProvider.DetailsRepresentation> getTestComponentDetails();

    @GET
    @Path("/test-amphibian-component")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Map<String, Object>> getTestAmphibianComponentDetails();


    @GET
    @Path("/identity-config")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> getIdentityProviderConfig(@QueryParam("alias") String alias);

    @PUT
    @Path("/set-krb5-conf-file")
    @Consumes(MediaType.APPLICATION_JSON)
    void setKrb5ConfFile(@QueryParam("krb5-conf-file") String krb5ConfFile);

    @POST
    @Path("/suspend-periodic-tasks")
    @Produces(MediaType.APPLICATION_JSON)
    Response suspendPeriodicTasks();

    @POST
    @Path("/restore-periodic-tasks")
    @Produces(MediaType.APPLICATION_JSON)
    Response restorePeriodicTasks();

    @Path("generate-audience-client-scope")
    @POST
    @NoCache
    String generateAudienceClientScope(@QueryParam("realm") final String realmName, final @QueryParam("clientId") String clientId);

    @GET
    @Path("/uncaught-error")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    Response uncaughtError();

    @GET
    @Path("/uncaught-error")
    Response uncaughtErrorJson();

    @POST
    @Path("/run-on-server")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    String runOnServer(String runOnServer);

    @POST
    @Path("/run-model-test-on-server")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    String runModelTestOnServer(@QueryParam("testClassName") String testClassName,
                                @QueryParam("testMethodName") String testMethodName);

    @GET
    @Path("js/keycloak.js")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    String getJavascriptAdapter();

    @GET
    @Path("/get-javascript-testing-environment")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    String getJavascriptTestingEnvironment();

    @POST
    @Path("/enable-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    Response enableFeature(@PathParam("feature") String feature);

    @POST
    @Path("/disable-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    Response disableFeature(@PathParam("feature") String feature);

    /**
     * If property-value is null, the system property will be unset (removed) on the server
     */
    @GET
    @Path("/set-system-property")
    @Consumes(MediaType.TEXT_HTML_UTF_8)
    void setSystemPropertyOnServer(@QueryParam("property-name") String propertyName, @QueryParam("property-value") String propertyValue);

    /**
     * Re-initialize specified provider factory with system properties scope. This will allow to change providerConfig in runtime with {@link #setSystemPropertyOnServer}
     *
     * This works just for the provider factories, which can be re-initialized without any side-effects (EG. some functionality already dependent
     * on the previously initialized properties, which cannot be easily changed in runtime)
     *
     * @param providerType fully qualified class name of provider (subclass of org.keycloak.provider.Provider)
     * @param providerId provider Id
     * @param systemPropertiesPrefix prefix to be used for system properties
     */
    @GET
    @Path("/reinitialize-provider-factory-with-system-properties-scope")
    @Consumes(MediaType.TEXT_HTML_UTF_8)
    @NoCache
    void reinitializeProviderFactoryWithSystemPropertiesScope(@QueryParam("provider-type") String providerType, @QueryParam("provider-id") String providerId,
                                                              @QueryParam("system-properties-prefix") String systemPropertiesPrefix);


    /**
     * This method is here just to have all endpoints from TestingResourceProvider available here.
     *
     * But usually it is requested to call this endpoint through WebDriver. See URLUtils.sendPOSTWithWebDriver for more details
     */
    @GET
    @Path("/simulate-post-request")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    Response simulatePostRequest(@QueryParam("postRequestUrl") String postRequestUrl,
                                         @QueryParam("encodedFormParameters") String encodedFormParameters);

    /**
     * Display message to Error Page - for testing purposes
     *
     * @param message message
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/display-error-message")
    Response displayErrorMessage(@QueryParam("message") String message);
}
