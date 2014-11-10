package org.keycloak.services.managers;

import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.keycloak.TokenIdGenerator;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.services.util.HttpClientBuilder;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.MultivaluedHashMap;
import org.keycloak.util.StringPropertyReplacer;
import org.keycloak.util.Time;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdminManager {
    protected static Logger logger = Logger.getLogger(ResourceAdminManager.class);
    private static final String APPLICATION_SESSION_HOST_PROPERTY = "${application.session.host}";

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

    // For non-cluster setup, return just single configured managementUrls
    // For cluster setup, return the management Urls corresponding to all registered cluster nodes
    private List<String> getAllManagementUrls(URI requestUri, ApplicationModel application) {
        String baseMgmtUrl = getManagementUrl(requestUri, application);
        if (baseMgmtUrl == null) {
            return Collections.emptyList();
        }

        Set<String> registeredNodesHosts = new ApplicationManager().validateRegisteredNodes(application);

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
            //logger.infov("logging out resources: {0}", clientSessions);

            for (Map.Entry<ApplicationModel, List<ClientSessionModel>> entry : clientSessions.entrySet()) {
                logoutClientSessions(requestUri, realm, entry.getKey(), entry.getValue(), executor);
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
                logoutClientSessions(requestUri, realm, entry.getKey(), entry.getValue(), executor);
            }
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public void logoutUserFromApplication(URI requestUri, RealmModel realm, ApplicationModel resource, UserModel user, KeycloakSession session) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
            List<ClientSessionModel> ourAppClientSessions = null;
            if (userSessions != null) {
                MultivaluedHashMap<ApplicationModel, ClientSessionModel> clientSessions = new MultivaluedHashMap<ApplicationModel, ClientSessionModel>();
                for (UserSessionModel userSession : userSessions) {
                    putClientSessions(clientSessions, userSession);
                }
                ourAppClientSessions = clientSessions.get(resource);
            }

            logoutClientSessions(requestUri, realm, resource, ourAppClientSessions, executor);
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }

    }

    public boolean logoutClientSession(URI requestUri, RealmModel realm, ApplicationModel resource, ClientSessionModel clientSession, ApacheHttpClient4Executor client) {
        return logoutClientSessions(requestUri, realm, resource, Arrays.asList(clientSession), client);
    }

    protected boolean logoutClientSessions(URI requestUri, RealmModel realm, ApplicationModel resource, List<ClientSessionModel> clientSessions, ApacheHttpClient4Executor client) {
        String managementUrl = getManagementUrl(requestUri, resource);
        if (managementUrl != null) {

            // Key is host, value is list of http sessions for this host
            MultivaluedHashMap<String, String> adapterSessionIds = null;
            if (clientSessions != null && clientSessions.size() > 0) {
                adapterSessionIds = new MultivaluedHashMap<String, String>();
                for (ClientSessionModel clientSession : clientSessions) {
                    String adapterSessionId = clientSession.getNote(AdapterConstants.APPLICATION_SESSION_STATE);
                    if (adapterSessionId != null) {
                        String host = clientSession.getNote(AdapterConstants.APPLICATION_SESSION_HOST);
                        adapterSessionIds.add(host, adapterSessionId);
                    }
                }
            }

            if (managementUrl.contains(APPLICATION_SESSION_HOST_PROPERTY) && adapterSessionIds != null) {
                boolean allPassed = true;
                // Send logout separately to each host (needed for single-sign-out in cluster for non-distributable apps - KEYCLOAK-748)
                for (Map.Entry<String, List<String>> entry : adapterSessionIds.entrySet()) {
                    String host = entry.getKey();
                    List<String> sessionIds = entry.getValue();
                    String currentHostMgmtUrl = managementUrl.replace(APPLICATION_SESSION_HOST_PROPERTY, host);
                    allPassed = sendLogoutRequest(realm, resource, sessionIds, client, 0, currentHostMgmtUrl) && allPassed;
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
                return sendLogoutRequest(realm, resource, allSessionIds, client, 0, managementUrl);
            }
        } else {
            logger.debugv("Can't logout {0}: no management url", resource.getName());
            return false;
        }
    }

    // Methods for logout all

    public GlobalRequestResult logoutAll(URI requestUri, RealmModel realm) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            realm.setNotBefore(Time.currentTime());
            List<ApplicationModel> resources = realm.getApplications();
            logger.debugv("logging out {0} resources ", resources.size());

            GlobalRequestResult finalResult = new GlobalRequestResult();
            for (ApplicationModel resource : resources) {
                GlobalRequestResult currentResult = logoutApplication(requestUri, realm, resource, executor, realm.getNotBefore());
                finalResult.addAll(currentResult);
            }
            return finalResult;
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public GlobalRequestResult logoutApplication(URI requestUri, RealmModel realm, ApplicationModel resource) {
        ApacheHttpClient4Executor executor = createExecutor();
        try {
            resource.setNotBefore(Time.currentTime());
            return logoutApplication(requestUri, realm, resource, executor, resource.getNotBefore());
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }


    protected GlobalRequestResult logoutApplication(URI requestUri, RealmModel realm, ApplicationModel resource, ApacheHttpClient4Executor executor, int notBefore) {
        List<String> mgmtUrls = getAllManagementUrls(requestUri, resource);
        if (mgmtUrls.isEmpty()) {
            logger.debug("No management URL or no registered cluster nodes for the application " + resource.getName());
            return new GlobalRequestResult();
        }

        if (logger.isDebugEnabled()) logger.debug("Send logoutApplication for URLs: " + mgmtUrls);

        // Propagate this to all hosts
        GlobalRequestResult result = new GlobalRequestResult();
        for (String mgmtUrl : mgmtUrls) {
            if (sendLogoutRequest(realm, resource, null, executor, notBefore, mgmtUrl)) {
                result.addSuccessRequest(mgmtUrl);
            } else {
                result.addFailedRequest(mgmtUrl);
            }
        }
        return result;
    }

    protected boolean sendLogoutRequest(RealmModel realm, ApplicationModel resource, List<String> adapterSessionIds, ApacheHttpClient4Executor client, int notBefore, String managementUrl) {
        LogoutAction adminAction = new LogoutAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, resource.getName(), adapterSessionIds, notBefore);
        String token = new TokenManager().encodeToken(realm, adminAction);
        if (logger.isDebugEnabled()) logger.debugv("logout resource {0} url: {1} sessionIds: " + adapterSessionIds, resource.getName(), managementUrl);
        ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_LOGOUT).build().toString());
        ClientResponse response;
        try {
            response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post();
        } catch (Exception e) {
            logger.warn("Logout for application '" + resource.getName() + "' failed", e);
            return false;
        }
        try {
            boolean success = response.getStatus() == 204 || response.getStatus() == 200;
            logger.debugf("logout success for %s: %s", managementUrl, success);
            return success;
        } finally {
            response.releaseConnection();
        }
    }

    public GlobalRequestResult pushRealmRevocationPolicy(URI requestUri, RealmModel realm) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            GlobalRequestResult finalResult = new GlobalRequestResult();
            for (ApplicationModel application : realm.getApplications()) {
                GlobalRequestResult currentResult = pushRevocationPolicy(requestUri, realm, application, realm.getNotBefore(), executor);
                finalResult.addAll(currentResult);
            }
            return finalResult;
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    public GlobalRequestResult pushApplicationRevocationPolicy(URI requestUri, RealmModel realm, ApplicationModel application) {
        ApacheHttpClient4Executor executor = createExecutor();

        try {
            return pushRevocationPolicy(requestUri, realm, application, application.getNotBefore(), executor);
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }


    protected GlobalRequestResult pushRevocationPolicy(URI requestUri, RealmModel realm, ApplicationModel resource, int notBefore, ApacheHttpClient4Executor executor) {
        List<String> mgmtUrls = getAllManagementUrls(requestUri, resource);
        if (mgmtUrls.isEmpty()) {
            logger.debugf("No management URL or no registered cluster nodes for the application %s", resource.getName());
            return new GlobalRequestResult();
        }

        if (logger.isDebugEnabled()) logger.info("Sending push revocation to URLS: " + mgmtUrls);

        // Propagate this to all hosts
        GlobalRequestResult result = new GlobalRequestResult();
        for (String mgmtUrl : mgmtUrls) {
            if (sendPushRevocationPolicyRequest(realm, resource, notBefore, executor, mgmtUrl)) {
                result.addSuccessRequest(mgmtUrl);
            } else {
                result.addFailedRequest(mgmtUrl);
            }
        }
        return result;
    }

    protected boolean sendPushRevocationPolicyRequest(RealmModel realm, ApplicationModel resource, int notBefore, ApacheHttpClient4Executor client, String managementUrl) {
        PushNotBeforeAction adminAction = new PushNotBeforeAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, resource.getName(), notBefore);
        String token = new TokenManager().encodeToken(realm, adminAction);
        logger.infov("pushRevocation resource: {0} url: {1}", resource.getName(), managementUrl);
        ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_PUSH_NOT_BEFORE).build().toString());
        ClientResponse response;
        try {
            response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post();
        } catch (Exception e) {
            logger.warn("Failed to send revocation request", e);
            return false;
        }
        try {
            boolean success = response.getStatus() == 204 || response.getStatus() == 200;
            logger.debugf("pushRevocation success for %s: %s", managementUrl, success);
            return success;
        } finally {
            response.releaseConnection();
        }
    }

    public GlobalRequestResult testNodesAvailability(URI requestUri, RealmModel realm, ApplicationModel application) {
        List<String> mgmtUrls = getAllManagementUrls(requestUri, application);
        if (mgmtUrls.isEmpty()) {
            logger.debug("No management URL or no registered cluster nodes for the application " + application.getName());
            return new GlobalRequestResult();
        }

        ApacheHttpClient4Executor executor = createExecutor();

        try {
            if (logger.isDebugEnabled()) logger.debug("Sending test nodes availability: " + mgmtUrls);

            // Propagate this to all hosts
            GlobalRequestResult result = new GlobalRequestResult();
            for (String mgmtUrl : mgmtUrls) {
                if (sendTestNodeAvailabilityRequest(realm, application, executor, mgmtUrl)) {
                    result.addSuccessRequest(mgmtUrl);
                } else {
                    result.addFailedRequest(mgmtUrl);
                }
            }
            return result;
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    protected boolean sendTestNodeAvailabilityRequest(RealmModel realm, ApplicationModel application, ApacheHttpClient4Executor client, String managementUrl) {
        TestAvailabilityAction adminAction = new TestAvailabilityAction(TokenIdGenerator.generateId(), Time.currentTime() + 30, application.getName());
        String token = new TokenManager().encodeToken(realm, adminAction);
        logger.debugv("testNodes availability resource: {0} url: {1}", application.getName(), managementUrl);
        ClientRequest request = client.createRequest(UriBuilder.fromUri(managementUrl).path(AdapterConstants.K_TEST_AVAILABLE).build().toString());
        ClientResponse response;
        try {
            response = request.body(MediaType.TEXT_PLAIN_TYPE, token).post();
        } catch (Exception e) {
            logger.warn("Availability test failed for uri '" + managementUrl + "'", e);
            return false;
        }
        try {
            boolean success = response.getStatus() == 204 || response.getStatus() == 200;
            logger.debugf("testAvailability success for %s: %s", managementUrl, success);
            return success;
        } finally {
            response.releaseConnection();
        }
    }

}
