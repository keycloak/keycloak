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
package org.keycloak.services.managers;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenIdGenerator;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.util.ResolveRelative;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdminManager {
    private static final Logger logger = Logger.getLogger(ResourceAdminManager.class);
    private static final String CLIENT_SESSION_HOST_PROPERTY = "${application.session.host}";

    private KeycloakSession session;

    public ResourceAdminManager(KeycloakSession session) {
        this.session = session;
    }

    public static String resolveUri(KeycloakSession session, String rootUrl, String uri) {
        return ResolveRelative.resolveRelativeUri(session, rootUrl, uri);

   }

    public static String getManagementUrl(KeycloakSession session, ClientModel client) {
        String mgmtUrl = client.getManagementUrl();
        if (mgmtUrl == null || mgmtUrl.equals("")) {
            return null;
        }

        return ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), mgmtUrl);
    }

    // For non-cluster setup, return just single configured managementUrls
    // For cluster setup, return the management Urls corresponding to all registered cluster nodes
    private List<String> getAllManagementUrls(ClientModel client) {
        String baseMgmtUrl = getManagementUrl(session, client);
        if (baseMgmtUrl == null) {
            return Collections.emptyList();
        }

        Set<String> registeredNodesHosts = new ClientManager().validateRegisteredNodes(client);

        // No-cluster setup
        if (registeredNodesHosts.isEmpty()) {
            return Arrays.asList(baseMgmtUrl);
        }

        List<String> result = new LinkedList<String>();
        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(baseMgmtUrl);
        for (String nodeHost : registeredNodesHosts) {
            String currentNodeUri = uriBuilder.clone().host(nodeHost).build().toString();
            result.add(currentNodeUri);
        }

        return result;
    }

    public Response logoutClientSession(RealmModel realm, ClientModel resource, AuthenticatedClientSessionModel clientSession) {
        return logoutClientSessions(realm, resource, Arrays.asList(clientSession));
    }

    protected Response logoutClientSessions(RealmModel realm, ClientModel resource, List<AuthenticatedClientSessionModel> clientSessions) {
        String managementUrl = getManagementUrl(session, resource);
        if (managementUrl != null) {

            // Key is host, value is list of http sessions for this host
            MultivaluedHashMap<String, String> adapterSessionIds = null;
            List<String> userSessions = new LinkedList<>();
            if (clientSessions != null && clientSessions.size() > 0) {
                adapterSessionIds = new MultivaluedHashMap<String, String>();
                for (AuthenticatedClientSessionModel clientSession : clientSessions) {
                    String adapterSessionId = clientSession.getNote(AdapterConstants.CLIENT_SESSION_STATE);
                    if (adapterSessionId != null) {
                        String host = clientSession.getNote(AdapterConstants.CLIENT_SESSION_HOST);
                        adapterSessionIds.add(host, adapterSessionId);
                    }
                    if (clientSession.getUserSession() != null) userSessions.add(clientSession.getUserSession().getId());
                }
            }

            if (adapterSessionIds == null || adapterSessionIds.isEmpty()) {
                logger.debugv("Can't logout {0}: no logged adapter sessions", resource.getClientId());
                return null;
            }

            if (managementUrl.contains(CLIENT_SESSION_HOST_PROPERTY)) {
                // Send logout separately to each host (needed for single-sign-out in cluster for non-distributable apps - KEYCLOAK-748)
                for (Map.Entry<String, List<String>> entry : adapterSessionIds.entrySet()) {
                    String host = entry.getKey();
                    List<String> sessionIds = entry.getValue();
                    String currentHostMgmtUrl = managementUrl.replace(CLIENT_SESSION_HOST_PROPERTY, host);
                    sendLogoutRequest(realm, resource, sessionIds, userSessions, 0, currentHostMgmtUrl);
                }
                return Response.ok().build();

            } else {
                // Send single logout request
                List<String> allSessionIds = new ArrayList<String>();
                for (List<String> currentIds : adapterSessionIds.values()) {
                    allSessionIds.addAll(currentIds);
                }

                return sendLogoutRequest(realm, resource, allSessionIds, userSessions, 0, managementUrl);
            }
        } else {
            logger.debugv("Can't logout {0}: no management url", resource.getClientId());
            return null;
        }
    }

    public Response logoutClientSessionWithBackchannelLogoutUrl(ClientModel resource,
            AuthenticatedClientSessionModel clientSession) {
        String backchannelLogoutUrl = getBackchannelLogoutUrl(session, resource);
        // Send logout separately to each host (needed for single-sign-out in cluster for non-distributable apps -
        // KEYCLOAK-748)
        if (backchannelLogoutUrl.contains(CLIENT_SESSION_HOST_PROPERTY)) {
            String host = clientSession.getNote(AdapterConstants.CLIENT_SESSION_HOST);
            String currentHostMgmtUrl = backchannelLogoutUrl.replace(CLIENT_SESSION_HOST_PROPERTY, host);
            return sendBackChannelLogoutRequestToClientUri(resource, clientSession, currentHostMgmtUrl);
        } else {
            return sendBackChannelLogoutRequestToClientUri(resource, clientSession, backchannelLogoutUrl);
        }
    }

    public static String getBackchannelLogoutUrl(KeycloakSession session, ClientModel client) {
        String backchannelLogoutUrl = OIDCAdvancedConfigWrapper.fromClientModel(client).getBackchannelLogoutUrl();
        if (backchannelLogoutUrl == null || backchannelLogoutUrl.equals("")) {
            return null;
        }

        return ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), backchannelLogoutUrl);
    }

    protected Response sendBackChannelLogoutRequestToClientUri(ClientModel resource,
                                                              AuthenticatedClientSessionModel clientSessionModel, String managementUrl) {
        UserModel user = clientSessionModel.getUserSession().getUser();

        HttpPost post = null;
        ClientModel previousClient = session.getContext().getClient();
        try {
            session.getContext().setClient(resource);

            LogoutToken logoutToken = session.tokens().initLogoutToken(resource, user, clientSessionModel);
            String token = session.tokens().encode(logoutToken);
            if (logger.isDebugEnabled()) {
                logger.debugv("logout resource {0} url: {1} sessionIds: ", resource.getClientId(), managementUrl);
            }

            post = new HttpPost(managementUrl);
            List<NameValuePair> parameters = new LinkedList<>();
            if (logoutToken != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.LOGOUT_TOKEN, token));
            }
            CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
            UrlEncodedFormEntity formEntity;
            formEntity = new UrlEncodedFormEntity(parameters);
            post.setEntity(formEntity);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                try {
                    int status = response.getStatusLine().getStatusCode();
                    EntityUtils.consumeQuietly(response.getEntity());
                    boolean success = status == 204 || status == 200;
                    logger.debugf("logout success for %s: %s", managementUrl, success);
                    return Response.status(status).build();
                } finally {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        } catch (IOException e) {
            ServicesLogger.LOGGER.logoutFailed(e, resource.getClientId());
            return Response.serverError().build();
        } finally {
            session.getContext().setClient(previousClient);
            if (post != null) {
                post.reset();
            }
        }
    }

    // Methods for logout all

    public GlobalRequestResult logoutAll(RealmModel realm) {
        realm.setNotBefore(Time.currentTime());

        GlobalRequestResult finalResult = new GlobalRequestResult();
        AtomicInteger counter = new AtomicInteger(0);
        realm.getClientsStream().forEach(c -> {
            try {
                counter.getAndIncrement();
                GlobalRequestResult currentResult = logoutClient(realm, c, realm.getNotBefore());
                finalResult.addAll(currentResult);
            } catch (ModelIllegalStateException ex) {
                // currently, GlobalRequestResult doesn't allow for information about clients that we were unable to retrieve.
                logger.warn("unable to retrieve client information for logout, skipping resource", ex);
            }
        });
        logger.debugv("logging out {0} resources ", counter);

        return finalResult;
    }

    public GlobalRequestResult logoutClient(RealmModel realm, ClientModel resource) {
        resource.setNotBefore(Time.currentTime());
        return logoutClient(realm, resource, resource.getNotBefore());
    }


    protected GlobalRequestResult logoutClient(RealmModel realm, ClientModel resource, int notBefore) {

        if (!resource.isEnabled()) {
            return new GlobalRequestResult();
        }

        List<String> mgmtUrls = getAllManagementUrls(resource);
        if (mgmtUrls.isEmpty()) {
            logger.debug("No management URL or no registered cluster nodes for the client " + resource.getClientId());
            return new GlobalRequestResult();
        }

        if (logger.isDebugEnabled()) logger.debug("Send logoutClient for URLs: " + mgmtUrls);

        // Propagate this to all hosts
        GlobalRequestResult result = new GlobalRequestResult();
        for (String mgmtUrl : mgmtUrls) {
            if (sendLogoutRequest(realm, resource, null, null, notBefore, mgmtUrl) != null) {
                result.addSuccessRequest(mgmtUrl);
            } else {
                result.addFailedRequest(mgmtUrl);
            }
        }
        return result;
    }

    protected Response sendLogoutRequest(RealmModel realm, ClientModel resource, List<String> adapterSessionIds, List<String> userSessions, int notBefore, String managementUrl) {
        LogoutAction adminAction = new LogoutAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, resource.getClientId(), adapterSessionIds, notBefore, userSessions);
        String token = session.tokens().encode(adminAction);
        if (logger.isDebugEnabled()) logger.debugv("logout resource {0} url: {1} sessionIds: " + adapterSessionIds, resource.getClientId(), managementUrl);
        URI target = UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_LOGOUT).build();
        try {
            int status = session.getProvider(HttpClientProvider.class).postText(target.toString(), token);
            boolean success = status == 204 || status == 200;
            logger.debugf("logout success for %s: %s", managementUrl, success);
            return Response.ok().build();
        } catch (IOException e) {
            ServicesLogger.LOGGER.logoutFailed(e, resource.getClientId());
            return Response.serverError().build();
        }
    }

    public GlobalRequestResult pushRealmRevocationPolicy(RealmModel realm) {
        GlobalRequestResult finalResult = new GlobalRequestResult();
        realm.getClientsStream().forEach(c -> {
            GlobalRequestResult currentResult = pushRevocationPolicy(realm, c, realm.getNotBefore());
            finalResult.addAll(currentResult);
        });
        return finalResult;
    }

    public GlobalRequestResult pushClientRevocationPolicy(RealmModel realm, ClientModel client) {
        return pushRevocationPolicy(realm, client, client.getNotBefore());
    }


    protected GlobalRequestResult pushRevocationPolicy(RealmModel realm, ClientModel resource, int notBefore) {
        List<String> mgmtUrls = getAllManagementUrls(resource);
        if (mgmtUrls.isEmpty()) {
            logger.debugf("No management URL or no registered cluster nodes for the client %s", resource.getClientId());
            return new GlobalRequestResult();
        }

        if (logger.isDebugEnabled()) logger.debug("Sending push revocation to URLS: " + mgmtUrls);

        // Propagate this to all hosts
        GlobalRequestResult result = new GlobalRequestResult();
        for (String mgmtUrl : mgmtUrls) {
            if (sendPushRevocationPolicyRequest(realm, resource, notBefore, mgmtUrl)) {
                result.addSuccessRequest(mgmtUrl);
            } else {
                result.addFailedRequest(mgmtUrl);
            }
        }
        return result;
    }

    protected boolean sendPushRevocationPolicyRequest(RealmModel realm, ClientModel resource, int notBefore, String managementUrl) {
        String protocol = resource.getProtocol();
        if (protocol == null) {
            protocol = OIDCLoginProtocol.LOGIN_PROTOCOL;
        }
        LoginProtocol loginProtocol = (LoginProtocol) session.getProvider(LoginProtocol.class, protocol);
        return loginProtocol == null
          ? false
          : loginProtocol.sendPushRevocationPolicyRequest(realm, resource, notBefore, managementUrl);
    }

    public GlobalRequestResult testNodesAvailability(RealmModel realm, ClientModel client) {
        List<String> mgmtUrls = getAllManagementUrls(client);
        if (mgmtUrls.isEmpty()) {
            logger.debug("No management URL or no registered cluster nodes for the application " + client.getClientId());
            return new GlobalRequestResult();
        }


        if (logger.isDebugEnabled()) logger.debug("Sending test nodes availability: " + mgmtUrls);

        // Propagate this to all hosts
        GlobalRequestResult result = new GlobalRequestResult();
        for (String mgmtUrl : mgmtUrls) {
            if (sendTestNodeAvailabilityRequest(realm, client, mgmtUrl)) {
                result.addSuccessRequest(mgmtUrl);
            } else {
                result.addFailedRequest(mgmtUrl);
            }
        }
        return result;
    }

    protected boolean sendTestNodeAvailabilityRequest(RealmModel realm, ClientModel client, String managementUrl) {
        TestAvailabilityAction adminAction = new TestAvailabilityAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, client.getClientId());
        String token = session.tokens().encode(adminAction);
        logger.debugv("testNodes availability resource: {0} url: {1}", client.getClientId(), managementUrl);
        URI target = UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_TEST_AVAILABLE).build();
        try {
            int status = session.getProvider(HttpClientProvider.class).postText(target.toString(), token);
            boolean success = status == 204 || status == 200;
            logger.debugf("testAvailability success for %s: %s", managementUrl, success);
            return success;
        } catch (IOException e) {
            ServicesLogger.LOGGER.availabilityTestFailed(managementUrl);
            return false;
        }
   }

}
