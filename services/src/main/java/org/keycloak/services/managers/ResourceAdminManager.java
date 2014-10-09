package org.keycloak.services.managers;

import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.keycloak.TokenIdGenerator;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.services.util.HttpClientBuilder;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.util.MultivaluedHashMap;
import org.keycloak.util.StringPropertyReplacer;
import org.keycloak.util.Time;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdminManager {
    protected static Logger logger = Logger.getLogger(ResourceAdminManager.class);
    private static final String KC_SESSION_HOST = "${kc_session_host}";

    public static ApacheHttpClient4Executor createExecutor() {
        HttpClient client = new HttpClientBuilder()
                .disableTrustManager() // todo fix this, should have a trust manager or a good default
                .build();
        return new ApacheHttpClient4Executor(client);
    }

    public static String getManagementUrl(URI requestUri, ApplicationModel application) {
        String mgmtUrl = application.getManagementUrl();
        if (mgmtUrl == null || mgmtUrl.equals("")) {
            return null;
        }

        // this is to support relative admin urls when keycloak and applications are deployed on the same machine
        String absoluteURI = ResolveRelative.resolveRelativeUri(requestUri, mgmtUrl);

        // this is for resolving URI like "http://${jboss.host.name}:8080/..." in order to send request to same machine and avoid request to LB in cluster environment
        return StringPropertyReplacer.replaceProperties(absoluteURI);
    }

    public void logoutUser(URI requestUri, RealmModel realm, UserModel user, KeycloakSession keycloakSession) {
        List<UserSessionModel> userSessions = keycloakSession.sessions().getUserSessions(realm, user);
        logoutUserSessions(requestUri, realm, userSessions);
    }

    protected void logoutUserSessions(URI requestUri, RealmModel realm, List<UserSessionModel> userSessions) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            // Map from "app" to clientSessions for this app
            MultivaluedHashMap<ApplicationModel, ClientSessionModel> clientSessions = new MultivaluedHashMap<ApplicationModel, ClientSessionModel>();
            for (UserSessionModel userSession : userSessions) {
                putClientSessions(clientSessions, userSession);
            }

            logger.debugv("logging out {0} resources ", clientSessions.size());
            logger.infov("logging out resources: " + clientSessions);

            for (Map.Entry<ApplicationModel, List<ClientSessionModel>> entry : clientSessions.entrySet()) {
                logoutApplication(requestUri, realm, entry.getKey(), entry.getValue(), executor, 0);
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    private void putClientSessions(MultivaluedHashMap<ApplicationModel, ClientSessionModel> clientSessions, UserSessionModel userSession) {
        for (ClientSessionModel clientSession : userSession.getClientSessions()) {
            ClientModel client = clientSession.getClient();
            if (client instanceof ApplicationModel) {
                clientSessions.add((ApplicationModel)client, clientSession);
            }
        }
    }

    public void logoutSession(URI requestUri, RealmModel realm, UserSessionModel session) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            // Map from "app" to clientSessions for this app
            MultivaluedHashMap<ApplicationModel, ClientSessionModel> clientSessions = new MultivaluedHashMap<ApplicationModel, ClientSessionModel>();
            putClientSessions(clientSessions, session);

            logger.debugv("logging out {0} resources ", clientSessions.size());
            for (Map.Entry<ApplicationModel, List<ClientSessionModel>> entry : clientSessions.entrySet()) {
                logoutApplication(requestUri, realm, entry.getKey(), entry.getValue(), executor, 0);
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public void logoutAll(URI requestUri, RealmModel realm) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            realm.setNotBefore(Time.currentTime());
            List<ApplicationModel> resources = realm.getApplications();
            logger.debugv("logging out {0} resources ", resources.size());
            for (ApplicationModel resource : resources) {
                logoutApplication(requestUri, realm, resource, (List<ClientSessionModel>)null, executor, realm.getNotBefore());
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public void logoutApplication(URI requestUri, RealmModel realm, ApplicationModel resource, List<UserSessionModel> userSessions) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            resource.setNotBefore(Time.currentTime());

            List<ClientSessionModel> ourAppClientSessions = null;
            if (userSessions != null) {
                MultivaluedHashMap<ApplicationModel, ClientSessionModel> clientSessions = new MultivaluedHashMap<ApplicationModel, ClientSessionModel>();
                for (UserSessionModel userSession : userSessions) {
                    putClientSessions(clientSessions, userSession);
                }
                ourAppClientSessions = clientSessions.get(resource);
            }

            logoutApplication(requestUri, realm, resource, ourAppClientSessions, executor, resource.getNotBefore());
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }

    }

    public boolean logoutApplication(URI requestUri, RealmModel realm, ApplicationModel resource, ClientSessionModel clientSession, ApacheHttpClient4Executor client, int notBefore) {
        return logoutApplication(requestUri, realm, resource, Arrays.asList(clientSession), client, notBefore);
    }

    protected boolean logoutApplication(URI requestUri, RealmModel realm, ApplicationModel resource, List<ClientSessionModel> clientSessions, ApacheHttpClient4Executor client, int notBefore) {
        String managementUrl = getManagementUrl(requestUri, resource);
        if (managementUrl != null) {

            // Key is host, value is list of http sessions for this host
            MultivaluedHashMap<String, String> adapterSessionIds = null;
            if (clientSessions != null && clientSessions.size() > 0) {
                adapterSessionIds = new MultivaluedHashMap<String, String>();
                for (ClientSessionModel clientSession : clientSessions) {
                    String adapterSessionId = clientSession.getNote(AdapterConstants.HTTP_SESSION_ID);
                    if (adapterSessionId != null) {
                        String host = clientSession.getNote(AdapterConstants.HTTP_SESSION_HOST);
                        adapterSessionIds.add(host, adapterSessionId);
                    }
                }
            }

            if (managementUrl.contains(KC_SESSION_HOST) && adapterSessionIds != null) {
                boolean allPassed = true;
                // Send logout separately to each host (needed for single-sign-out in cluster for non-distributable apps - KEYCLOAK-748)
                for (Map.Entry<String, List<String>> entry : adapterSessionIds.entrySet()) {
                    String host = entry.getKey();
                    List<String> sessionIds = entry.getValue();
                    String currentHostMgmtUrl = managementUrl.replace(KC_SESSION_HOST, host);
                    allPassed = logoutApplicationOnHost(realm, resource, sessionIds, client, notBefore, currentHostMgmtUrl) && allPassed;
                }

                return allPassed;
            } else {
                // Send single logout request
                List<String> allSessionIds = null;
                if (adapterSessionIds != null) {
                    allSessionIds = new ArrayList<String>();
                    for (List<String> currentIds : adapterSessionIds.values()) {
                        allSessionIds.addAll(currentIds);
                    }
                }
                return logoutApplicationOnHost(realm, resource, allSessionIds, client, notBefore, managementUrl);
            }
        } else {
            logger.debugv("Can't logout {0}: no management url", resource.getName());
            return false;
        }
    }

    protected boolean logoutApplicationOnHost(RealmModel realm, ApplicationModel resource, List<String> adapterSessionIds, ApacheHttpClient4Executor client, int notBefore, String managementUrl) {
        LogoutAction adminAction = new LogoutAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, resource.getName(), adapterSessionIds, notBefore);
        String token = new TokenManager().encodeToken(realm, adminAction);
        logger.infov("logout resource {0} url: {1} sessionIds: " + adapterSessionIds, resource.getName(), managementUrl);
        ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_LOGOUT).build().toString());
        ClientResponse response;
        try {
            response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post(UserStats.class);
        } catch (Exception e) {
            logger.warn("Logout for application '" + resource.getName() + "' failed", e);
            return false;
        }
        try {
            boolean success = response.getStatus() == 204;
            logger.debug("logout success.");
            return success;
        } finally {
            response.releaseConnection();
        }
    }

    public void pushRealmRevocationPolicy(URI requestUri, RealmModel realm) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            for (ApplicationModel application : realm.getApplications()) {
                pushRevocationPolicy(requestUri, realm, application, realm.getNotBefore(), executor);
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public void pushApplicationRevocationPolicy(URI requestUri, RealmModel realm, ApplicationModel application) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            pushRevocationPolicy(requestUri, realm, application, application.getNotBefore(), executor);
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }


    protected boolean pushRevocationPolicy(URI requestUri, RealmModel realm, ApplicationModel resource, int notBefore, ApacheHttpClient4Executor client) {
        if (notBefore <= 0) return false;
        String managementUrl = getManagementUrl(requestUri, resource);
        if (managementUrl != null) {
            PushNotBeforeAction adminAction = new PushNotBeforeAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, resource.getName(), notBefore);
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.infov("pushRevocation resource: {0} url: {1}", resource.getName(), managementUrl);
            ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_PUSH_NOT_BEFORE).build().toString());
            ClientResponse response;
            try {
                response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                boolean success = response.getStatus() == 204;
                logger.debug("pushRevocation success.");
                return success;
            } finally {
                response.releaseConnection();
            }
        } else {
            logger.debug("no management URL for application: " + resource.getName());
            return false;
        }


    }
}
