package org.keycloak.services.offline;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OfflineClientSessionModel;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Time;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineTokenUtils {

    protected static Logger logger = Logger.getLogger(OfflineTokenUtils.class);

    public static void persistOfflineSession(KeycloakSession kcSession, RealmModel realm, ClientSessionModel clientSession, UserSessionModel userSession) {
        UserModel user = userSession.getUser();
        ClientModel client = clientSession.getClient();

        // First verify if we already have offlineToken for this user+client . If yes, then invalidate it (This is to avoid leaks)
        Collection<OfflineClientSessionModel> clientSessions = kcSession.users().getOfflineClientSessions(realm, user);
        for (OfflineClientSessionModel existing : clientSessions) {
            if (existing.getClientId().equals(client.getId())) {
                if (logger.isTraceEnabled()) {
                    logger.tracef("Removing existing offline token for user '%s' and client '%s' . ClientSessionID was '%s' . Offline token will be replaced with new one",
                            user.getUsername(), client.getClientId(), existing.getClientSessionId());
                }

                kcSession.users().removeOfflineClientSession(realm, user, existing.getClientSessionId());

                // Check if userSession is ours. If not, then check if it has other clientSessions and remove it otherwise
                if (!existing.getUserSessionId().equals(userSession.getId())) {
                    checkUserSessionHasClientSessions(kcSession, realm, user, existing.getUserSessionId());
                }
            }
        }

        // Verify if we already have UserSession with this ID. If yes, don't create another one
        OfflineUserSessionModel userSessionRep = kcSession.users().getOfflineUserSession(realm, user, userSession.getId());
        if (userSessionRep == null) {
            createOfflineUserSession(kcSession, realm, user, userSession);
        }

        // Create clientRep and save to DB.
        createOfflineClientSession(kcSession, realm, user, clientSession, userSession);
    }

    // userSessionId is provided from offline token. It's used just to verify if it match the ID from clientSession representation
    public static ClientSessionModel findOfflineClientSession(KeycloakSession kcSession, RealmModel realm, UserModel user, String clientSessionId, String userSessionId) {
        OfflineClientSessionModel clientSession = kcSession.users().getOfflineClientSession(realm, user, clientSessionId);
        if (clientSession == null) {
            return null;
        }

        if (!userSessionId.equals(clientSession.getUserSessionId())) {
            throw new ModelException("User session don't match. Offline client session " + clientSession.getClientSessionId() + ", It's user session " + clientSession.getUserSessionId() +
                    "  Wanted user session: " + userSessionId);
        }

        OfflineUserSessionModel userSession = kcSession.users().getOfflineUserSession(realm, user, userSessionId);
        if (userSession == null) {
            throw new ModelException("Found clientSession " + clientSessionId + " but not userSession " + userSessionId);
        }

        OfflineUserSessionAdapter userSessionAdapter = new OfflineUserSessionAdapter(userSession, user);

        ClientModel client = realm.getClientById(clientSession.getClientId());
        return new OfflineClientSessionAdapter(clientSession, realm, client, userSessionAdapter);
    }

    public static Set<ClientModel> findClientsWithOfflineToken(KeycloakSession kcSession, RealmModel realm, UserModel user) {
        Collection<OfflineClientSessionModel> clientSessions = kcSession.users().getOfflineClientSessions(realm, user);
        Set<ClientModel> clients = new HashSet<>();
        for (OfflineClientSessionModel clientSession : clientSessions) {
            ClientModel client = realm.getClientById(clientSession.getClientId());
            clients.add(client);
        }
        return clients;
    }

    public static boolean revokeOfflineToken(KeycloakSession kcSession, RealmModel realm, UserModel user, ClientModel client) {
        Collection<OfflineClientSessionModel> clientSessions = kcSession.users().getOfflineClientSessions(realm, user);
        boolean anyRemoved = false;
        for (OfflineClientSessionModel clientSession : clientSessions) {
            if (clientSession.getClientId().equals(client.getId())) {
                if (logger.isTraceEnabled()) {
                    logger.tracef("Removing existing offline token for user '%s' and client '%s' . ClientSessionID was '%s' .",
                            user.getUsername(), client.getClientId(), clientSession.getClientSessionId());
                }

                kcSession.users().removeOfflineClientSession(realm, user, clientSession.getClientSessionId());
                checkUserSessionHasClientSessions(kcSession, realm, user, clientSession.getUserSessionId());
                anyRemoved = true;
            }
        }

        return anyRemoved;
    }

    public static boolean isOfflineTokenAllowed(RealmModel realm, ClientSessionModel clientSession) {
        RoleModel offlineAccessRole = realm.getRole(Constants.OFFLINE_ACCESS_ROLE);
        if (offlineAccessRole == null) {
            logger.warnf("Role '%s' not available in realm", Constants.OFFLINE_ACCESS_ROLE);
            return false;
        }

        return clientSession.getRoles().contains(offlineAccessRole.getId());
    }

    private static void createOfflineUserSession(KeycloakSession kcSession, RealmModel realm, UserModel user, UserSessionModel userSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Creating new offline user session. UserSessionID: '%s' , Username: '%s'", userSession.getId(), user.getUsername());
        }
        OfflineUserSessionAdapter.OfflineUserSessionData rep = new OfflineUserSessionAdapter.OfflineUserSessionData();
        rep.setBrokerUserId(userSession.getBrokerUserId());
        rep.setBrokerSessionId(userSession.getBrokerSessionId());
        rep.setIpAddress(userSession.getIpAddress());
        rep.setAuthMethod(userSession.getAuthMethod());
        rep.setRememberMe(userSession.isRememberMe());
        rep.setStarted(userSession.getStarted());
        rep.setNotes(userSession.getNotes());

        try {
            String stringRep = JsonSerialization.writeValueAsString(rep);
            OfflineUserSessionModel sessionModel = new OfflineUserSessionModel();
            sessionModel.setUserSessionId(userSession.getId());
            sessionModel.setData(stringRep);
            kcSession.users().addOfflineUserSession(realm, user, sessionModel);
        } catch (IOException ioe) {
            throw new ModelException(ioe);
        }
    }

    private static void createOfflineClientSession(KeycloakSession kcSession, RealmModel realm, UserModel user, ClientSessionModel clientSession, UserSessionModel userSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Creating new offline token client session. ClientSessionId: '%s', UserSessionID: '%s' , Username: '%s', Client: '%s'" ,
                    clientSession.getId(), userSession.getId(), user.getUsername(), clientSession.getClient().getClientId());
        }
        OfflineClientSessionAdapter.OfflineClientSessionData rep = new OfflineClientSessionAdapter.OfflineClientSessionData();
        rep.setAuthMethod(clientSession.getAuthMethod());
        rep.setRedirectUri(clientSession.getRedirectUri());
        rep.setProtocolMappers(clientSession.getProtocolMappers());
        rep.setRoles(clientSession.getRoles());
        rep.setNotes(clientSession.getNotes());
        rep.setAuthenticatorStatus(clientSession.getExecutionStatus());
        rep.setTimestamp(Time.currentTime());

        try {
            String stringRep = JsonSerialization.writeValueAsString(rep);
            OfflineClientSessionModel clsModel = new OfflineClientSessionModel();
            clsModel.setClientSessionId(clientSession.getId());
            clsModel.setClientId(clientSession.getClient().getId());
            clsModel.setUserId(user.getId());
            clsModel.setUserSessionId(userSession.getId());
            clsModel.setData(stringRep);
            kcSession.users().addOfflineClientSession(realm, clsModel);
        } catch (IOException ioe) {
            throw new ModelException(ioe);
        }
    }

    // Check if userSession has any offline clientSessions attached to it. Remove userSession if not
    private static void checkUserSessionHasClientSessions(KeycloakSession kcSession, RealmModel realm, UserModel user, String userSessionId) {
        Collection<OfflineClientSessionModel> clientSessions = kcSession.users().getOfflineClientSessions(realm, user);

        for (OfflineClientSessionModel clientSession : clientSessions) {
            if (clientSession.getUserSessionId().equals(userSessionId)) {
                return;
            }
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Removing offline userSession for user %s as it doesn't have any client sessions attached. UserSessionID: %s", user.getUsername(), userSessionId);
        }
        kcSession.users().removeOfflineUserSession(realm, user, userSessionId);
    }
}
