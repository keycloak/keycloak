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
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.adapters.action.SessionStatsAction;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.adapters.action.UserStatsAction;
import org.keycloak.services.util.HttpClientBuilder;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.util.Time;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdminManager {
    protected static Logger logger = Logger.getLogger(ResourceAdminManager.class);

    public SessionStats getSessionStats(URI requestUri, KeycloakSession session, RealmModel realm, ApplicationModel application, boolean users) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            return getSessionStats(requestUri, session, realm, application, users, executor);
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }

    }

    public static ApacheHttpClient4Executor createExecutor() {
        HttpClient client = new HttpClientBuilder()
                .disableTrustManager() // todo fix this, should have a trust manager or a good default
                .build();
        return new ApacheHttpClient4Executor(client);
    }

    public SessionStats getSessionStats(URI requestUri, KeycloakSession session, RealmModel realm, ApplicationModel application, boolean users, ApacheHttpClient4Executor client) {
        String managementUrl = getManagementUrl(requestUri, application);
        if (managementUrl != null) {
            SessionStatsAction adminAction = new SessionStatsAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, application.getName());
            adminAction.setListUsers(users);
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.debugv("session stats for application: {0} url: {1}", application.getName(), managementUrl);
            ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_GET_SESSION_STATS).build().toString());
            ClientResponse<SessionStats> response = null;
            try {
                response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post(SessionStats.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                if (response.getStatus() != 200) {
                    logger.warn("Failed to get stats: " + response.getStatus());
                    return null;
                }
                SessionStats stats = response.getEntity();

                // replace with username
                if (users && stats.getUsers() != null) {
                    Map<String, UserStats> newUsers = new HashMap<String, UserStats>();
                    for (Map.Entry<String, UserStats> entry : stats.getUsers().entrySet()) {
                        UserModel user = session.users().getUserById(entry.getKey(), realm);
                        if (user == null) continue;
                        newUsers.put(user.getUsername(), entry.getValue());

                    }
                    stats.setUsers(newUsers);
                }
                return stats;
            } finally {
                response.releaseConnection();
            }
        } else {
            logger.debug("no management url.");
            return null;
        }

    }

    protected String getManagementUrl(URI requestUri, ApplicationModel application) {
        String mgmtUrl = application.getManagementUrl();
        if (mgmtUrl == null || mgmtUrl.equals("")) {
            return null;
        }

        // this is to support relative admin urls when keycloak and applications are deployed on the same machine
        return ResolveRelative.resolveRelativeUri(requestUri, mgmtUrl);

    }

    public UserStats getUserStats(URI requestUri, RealmModel realm, ApplicationModel application, UserModel user) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            return getUserStats(requestUri, realm, application, user, executor);
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }

    }


    public UserStats getUserStats(URI requestUri, RealmModel realm, ApplicationModel application, UserModel user, ApacheHttpClient4Executor client) {
        String managementUrl = getManagementUrl(requestUri, application);
        if (managementUrl != null) {
            UserStatsAction adminAction = new UserStatsAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, application.getName(), user.getId());
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.debugv("session stats for application: {0} url: {1}", application.getName(), managementUrl);
            ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_GET_USER_STATS).build().toString());
            ClientResponse<UserStats> response = null;
            try {
                response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post(UserStats.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                if (response.getStatus() != 200) {
                    logger.warn("Failed to get stats: " + response.getStatus());
                    return null;
                }
                UserStats stats = response.getEntity();
                return stats;
            } finally {
                response.releaseConnection();
            }
        } else {
            logger.debug("no management url.");
            return null;
        }

    }

    public void logoutUser(URI requestUri, RealmModel realm, String user, UserSessionModel session) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            List<ApplicationModel> resources;
            if (session != null) {
                resources = new LinkedList<ApplicationModel>();

                for (ClientSessionModel clientSession : session.getClientSessions()) {
                    ClientModel client = clientSession.getClient();
                    if (client instanceof ApplicationModel) {
                        resources.add((ApplicationModel) client);
                    }
                }
            } else {
                resources = realm.getApplications();
            }

            logger.debugv("logging out {0} resources ", resources.size());
            for (ApplicationModel resource : resources) {
                logoutApplication(requestUri, realm, resource, user, session != null ? session.getId() : null, executor, 0);
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public void logoutSession(URI requestUri, RealmModel realm, UserSessionModel session) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            List<ApplicationModel> resources = new LinkedList<ApplicationModel>();
            for (ClientSessionModel clientSession : session.getClientSessions()) {
                ClientModel client = clientSession.getClient();
                if (client instanceof ApplicationModel) {
                    resources.add((ApplicationModel) client);
                }
            }

            logger.debugv("logging out {0} resources ", resources.size());
            for (ApplicationModel resource : resources) {
                logoutApplication(requestUri, realm, resource, null, session.getId(), executor, 0);
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
                logoutApplication(requestUri, realm, resource, null, null, executor, realm.getNotBefore());
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public void logoutApplication(URI requestUri, RealmModel realm, ApplicationModel resource, String user, String session) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            resource.setNotBefore(Time.currentTime());
            logoutApplication(requestUri, realm, resource, user, session, executor, resource.getNotBefore());
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }

    }


    protected boolean logoutApplication(URI requestUri, RealmModel realm, ApplicationModel resource, String user, String session, ApacheHttpClient4Executor client, int notBefore) {
        String managementUrl = getManagementUrl(requestUri, resource);
        if (managementUrl != null) {
            LogoutAction adminAction = new LogoutAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, resource.getName(), user, session, notBefore);
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.debugv("logout user: {0} resource: {1} url: {2}", user, resource.getName(), managementUrl);
            ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_LOGOUT).build().toString());
            ClientResponse response;
            try {
                response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post(UserStats.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                boolean success = response.getStatus() == 204;
                logger.debug("logout success.");
                return success;
            } finally {
                response.releaseConnection();
            }
        } else {
            logger.debugv("Can't logout {0}: no management url", resource.getName());
            return false;
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
            logger.debugv("pushRevocation resource: {0} url: {1}", resource.getName(), managementUrl);
            ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_PUSH_NOT_BEFORE).build().toString());
            ClientResponse response = null;
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
