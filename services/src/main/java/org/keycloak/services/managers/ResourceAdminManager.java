package org.keycloak.services.managers;

import org.apache.http.client.HttpClient;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.TokenIdGenerator;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.adapters.action.SessionStatsAction;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.adapters.action.UserStatsAction;
import org.keycloak.services.util.HttpClientBuilder;
import org.keycloak.util.Time;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdminManager {
    protected static Logger logger = Logger.getLogger(ResourceAdminManager.class);

    public SessionStats getSessionStats(RealmModel realm, ApplicationModel application, boolean users) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            return getSessionStats(realm, application, users, executor);
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

    public SessionStats getSessionStats(RealmModel realm, ApplicationModel application, boolean users, ApacheHttpClient4Executor client) {
        String managementUrl = application.getManagementUrl();
        if (managementUrl != null) {
            SessionStatsAction adminAction = new SessionStatsAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, application.getName());
            adminAction.setListUsers(users);
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.info("session stats for application: {0} url: {1}", application.getName(), managementUrl);
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
                        UserModel user = realm.getUserById(entry.getKey());
                        if (user == null) continue;
                        newUsers.put(user.getLoginName(), entry.getValue());

                    }
                    stats.setUsers(newUsers);
                }
                return stats;
            } finally {
                response.releaseConnection();
            }
        } else {
            logger.info("no management url.");
            return null;
        }

    }

    public UserStats getUserStats(RealmModel realm, ApplicationModel application, UserModel user) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            return getUserStats(realm, application, user, executor);
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }

    }


    public UserStats getUserStats(RealmModel realm, ApplicationModel application, UserModel user, ApacheHttpClient4Executor client) {
        String managementUrl = application.getManagementUrl();
        if (managementUrl != null) {
            UserStatsAction adminAction = new UserStatsAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, application.getName(), user.getId());
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.info("session stats for application: {0} url: {1}", application.getName(), managementUrl);
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
            logger.info("no management url.");
            return null;
        }

    }

    public void logoutUser(RealmModel realm, UserModel user) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            // don't set user notBefore as we don't want a database hit on a user driven logout
            List<ApplicationModel> resources = realm.getApplications();
            logger.debug("logging out {0} resources ", resources.size());
            for (ApplicationModel resource : resources) {
                logoutApplication(realm, resource, user.getId(), executor, 0);
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }
    public void logoutAll(RealmModel realm) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            realm.setNotBefore(Time.currentTime());
            List<ApplicationModel> resources = realm.getApplications();
            logger.debug("logging out {0} resources ", resources.size());
            for (ApplicationModel resource : resources) {
                logoutApplication(realm, resource, null, executor, realm.getNotBefore());
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public void logoutApplication(RealmModel realm, ApplicationModel resource, String user) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            resource.setNotBefore(Time.currentTime());
            logoutApplication(realm, resource, user, executor, resource.getNotBefore());
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }

    }


    protected boolean logoutApplication(RealmModel realm, ApplicationModel resource, String user, ApacheHttpClient4Executor client, int notBefore) {
        String managementUrl = resource.getManagementUrl();
        if (managementUrl != null) {
            LogoutAction adminAction = new LogoutAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, resource.getName(), user, notBefore);
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.info("logout user: {0} resource: {1} url: {2}", user, resource.getName(), managementUrl);
            ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_LOGOUT).build().toString());
            ClientResponse response = null;
            try {
                response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post(UserStats.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                boolean success = response.getStatus() == 204;
                logger.info("logout success.");
                return success;
            } finally {
                response.releaseConnection();
            }
        } else {
            logger.info("Can't logout" + resource.getName() + " no mgmt url.");
            return false;
        }
    }

    public void pushRealmRevocationPolicy(RealmModel realm) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            for (ApplicationModel application : realm.getApplications()) {
                pushRevocationPolicy(realm, application, realm.getNotBefore(), executor);
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public void pushApplicationRevocationPolicy(RealmModel realm, ApplicationModel application) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            pushRevocationPolicy(realm, application, application.getNotBefore(), executor);
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }


    protected boolean pushRevocationPolicy(RealmModel realm, ApplicationModel resource, int notBefore, ApacheHttpClient4Executor client) {
        if (notBefore <= 0) return false;
        String managementUrl = resource.getManagementUrl();
        if (managementUrl != null) {
            PushNotBeforeAction adminAction = new PushNotBeforeAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, resource.getName(), notBefore);
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.info("pushRevocation resource: {0} url: {1}", resource.getName(), managementUrl);
            ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_PUSH_NOT_BEFORE).build().toString());
            ClientResponse response = null;
            try {
                response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                boolean success = response.getStatus() == 204;
                logger.info("pushRevocation success.");
                return success;
            } finally {
                response.releaseConnection();
            }
        } else {
            logger.info("no management URL for application: " + resource.getName());
            return false;
        }


    }
}
