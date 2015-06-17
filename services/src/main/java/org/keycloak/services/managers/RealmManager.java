package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.enums.SslRequired;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.DefaultRequiredActions;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.timer.TimerProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Per request object
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager {
    protected static final Logger logger = Logger.getLogger(RealmManager.class);

    protected KeycloakSession session;
    protected RealmProvider model;
    protected String contextPath = "";

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public RealmManager(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmModel getKeycloakAdminstrationRealm() {
        return getRealm(Config.getAdminRealm());
    }

    public RealmModel getRealm(String id) {
        return model.getRealm(id);
    }

    public RealmModel getRealmByName(String name) {
        return model.getRealmByName(name);
    }

    public RealmModel createRealm(String name) {
        return createRealm(name, name);
    }

    public RealmModel createRealm(String id, String name) {
        if (id == null) id = KeycloakModelUtils.generateId();
        RealmModel realm = model.createRealm(id, name);
        realm.setName(name);

        // setup defaults
        setupRealmDefaults(realm);

        setupMasterAdminManagement(realm);
        setupRealmAdminManagement(realm);
        setupAccountManagement(realm);
        setupBrokerService(realm);
        setupAdminConsole(realm);
        setupAuthenticationFlows(realm);
        setupRequiredActions(realm);

        return realm;
    }

    protected void setupAuthenticationFlows(RealmModel realm) {
        if (realm.getAuthenticationFlows().size() == 0) DefaultAuthenticationFlows.addFlows(realm);
    }

    protected void setupRequiredActions(RealmModel realm) {
        if (realm.getRequiredActionProviders().size() == 0) DefaultRequiredActions.addActions(realm);
    }

    protected void setupAdminConsole(RealmModel realm) {
        ClientModel adminConsole = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (adminConsole == null) adminConsole = new ClientManager(this).createClient(realm, Constants.ADMIN_CONSOLE_CLIENT_ID);
        adminConsole.setName("${client_" + Constants.ADMIN_CONSOLE_CLIENT_ID + "}");
        String baseUrl = contextPath + "/admin/" + realm.getName() + "/console";
        adminConsole.setBaseUrl(baseUrl + "/index.html");
        adminConsole.setEnabled(true);
        adminConsole.setPublicClient(true);
        adminConsole.addRedirectUri(baseUrl + "/*");
        adminConsole.setFullScopeAllowed(false);

        RoleModel adminRole;
        if (realm.getName().equals(Config.getAdminRealm())) {
            adminRole = realm.getRole(AdminRoles.ADMIN);
        } else {
            String realmAdminApplicationClientId = getRealmAdminClientId(realm);
            ClientModel realmAdminApp = realm.getClientByClientId(realmAdminApplicationClientId);
            adminRole = realmAdminApp.getRole(AdminRoles.REALM_ADMIN);
        }
        adminConsole.addScopeMapping(adminRole);
    }

    public String getRealmAdminClientId(RealmModel realm) {
        return Constants.REALM_MANAGEMENT_CLIENT_ID;
    }

    public String getRealmAdminClientId(RealmRepresentation realm) {
        return Constants.REALM_MANAGEMENT_CLIENT_ID;
    }



    protected void setupRealmDefaults(RealmModel realm) {
        realm.setBrowserSecurityHeaders(BrowserSecurityHeaders.defaultHeaders);

        // brute force
        realm.setBruteForceProtected(false); // default settings off for now todo set it on
        realm.setMaxFailureWaitSeconds(900);
        realm.setMinimumQuickLoginWaitSeconds(60);
        realm.setWaitIncrementSeconds(60);
        realm.setQuickLoginCheckMilliSeconds(1000);
        realm.setMaxDeltaTimeSeconds(60 * 60 * 12); // 12 hours
        realm.setFailureFactor(30);
        realm.setSslRequired(SslRequired.EXTERNAL);

        realm.setEventsListeners(Collections.singleton("jboss-logging"));
    }

    public boolean removeRealm(RealmModel realm) {
        List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();

        boolean removed = model.removeRealm(realm.getId());
        if (removed) {
            new ClientManager(this).removeClient(getKeycloakAdminstrationRealm(), realm.getMasterAdminClient());

            UserSessionProvider sessions = session.sessions();
            if (sessions != null) {
                sessions.onRealmRemoved(realm);
            }

            // Remove all periodic syncs for configured federation providers
            UsersSyncManager usersSyncManager = new UsersSyncManager();
            for (final UserFederationProviderModel fedProvider : federationProviders) {
                usersSyncManager.removePeriodicSyncForProvider(session.getProvider(TimerProvider.class), fedProvider);
            }
        }
        return removed;
    }

    public void updateRealmEventsConfig(RealmEventsConfigRepresentation rep, RealmModel realm) {
        realm.setEventsEnabled(rep.isEventsEnabled());
        realm.setEventsExpiration(rep.getEventsExpiration() != null ? rep.getEventsExpiration() : 0);
        if (rep.getEventsListeners() != null) {
            realm.setEventsListeners(new HashSet<String>(rep.getEventsListeners()));
        }
        if(rep.getEnabledEventTypes() != null) {
            realm.setEnabledEventTypes(new HashSet<String>(rep.getEnabledEventTypes()));
        }
        
        realm.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        realm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());
    }

    // Should be RealmManager moved to model/api instead of referencing methods this way?
    private void setupMasterAdminManagement(RealmModel realm) {
        ImportUtils.setupMasterAdminManagement(model, realm);
    }

    private void setupRealmAdminManagement(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) { return; } // don't need to do this for master realm

        ClientManager clientManager = new ClientManager(new RealmManager(session));

        String realmAdminClientId = getRealmAdminClientId(realm);
        ClientModel realmAdminClient = realm.getClientByClientId(realmAdminClientId);
        if (realmAdminClient == null) {
            realmAdminClient = clientManager.createClient(realm, realmAdminClientId);
            realmAdminClient.setName("${client_" + realmAdminClientId + "}");
        }
        RoleModel adminRole = realmAdminClient.addRole(AdminRoles.REALM_ADMIN);
        adminRole.setDescription("${role_" + AdminRoles.REALM_ADMIN + "}");
        realmAdminClient.setBearerOnly(true);
        realmAdminClient.setFullScopeAllowed(false);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminClient.addRole(r);
            role.setDescription("${role_"+r+"}");
            adminRole.addCompositeRole(role);
        }
    }


    private void setupAccountManagement(RealmModel realm) {
        ClientModel client = realm.getClientNameMap().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null) {
            client = new ClientManager(this).createClient(realm, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            client.setName("${client_" + Constants.ACCOUNT_MANAGEMENT_CLIENT_ID + "}");
            client.setEnabled(true);
            client.setFullScopeAllowed(false);
            String base = contextPath + "/realms/" + realm.getName() + "/account";
            String redirectUri = base + "/*";
            client.addRedirectUri(redirectUri);
            client.setBaseUrl(base);

            for (String role : AccountRoles.ALL) {
                client.addDefaultRole(role);
                client.getRole(role).setDescription("${role_"+role+"}");
            }
        }
    }

    public void setupBrokerService(RealmModel realm) {
        ClientModel client = realm.getClientNameMap().get(Constants.BROKER_SERVICE_CLIENT_ID);
        if (client == null) {
            client = new ClientManager(this).createClient(realm, Constants.BROKER_SERVICE_CLIENT_ID);
            client.setEnabled(true);
            client.setName("${client_" + Constants.BROKER_SERVICE_CLIENT_ID + "}");
            client.setFullScopeAllowed(false);

            for (String role : Constants.BROKER_SERVICE_ROLES) {
                client.addRole(role).setDescription("${role_"+ role.toLowerCase().replaceAll("_", "-") +"}");
            }
        }
    }

    public RealmModel importRealm(RealmRepresentation rep) {
        String id = rep.getId();
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }
        RealmModel realm = model.createRealm(id, rep.getRealm());
        realm.setName(rep.getRealm());

        // setup defaults

        setupRealmDefaults(realm);
        setupMasterAdminManagement(realm);
        if (!hasRealmAdminManagementClient(rep)) setupRealmAdminManagement(realm);
        if (!hasAccountManagementClient(rep)) setupAccountManagement(realm);
        if (!hasBrokerClient(rep)) setupBrokerService(realm);
        if (!hasAdminConsoleClient(rep)) setupAdminConsole(realm);

        RepresentationToModel.importRealm(session, rep, realm);

        setupAuthenticationFlows(realm);
        setupRequiredActions(realm);

        // Refresh periodic sync tasks for configured federationProviders
        List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();
        UsersSyncManager usersSyncManager = new UsersSyncManager();
        for (final UserFederationProviderModel fedProvider : federationProviders) {
            usersSyncManager.refreshPeriodicSyncForProvider(session.getKeycloakSessionFactory(), session.getProvider(TimerProvider.class), fedProvider, realm.getId());
        }
        return realm;
    }

    private boolean hasRealmAdminManagementClient(RealmRepresentation rep) {
        if (rep.getClients() == null) return false;
        for (ClientRepresentation clientRep : rep.getClients()) {
            if (clientRep.getClientId().equals(getRealmAdminClientId(rep))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAccountManagementClient(RealmRepresentation rep) {
        if (rep.getClients() == null) return false;
        for (ClientRepresentation clientRep : rep.getClients()) {
            if (clientRep.getClientId().equals(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)) {
                return true;
            }
        }
        return false;
    }
    private boolean hasBrokerClient(RealmRepresentation rep) {
        if (rep.getClients() == null) return false;
        for (ClientRepresentation clientRep : rep.getClients()) {
            if (clientRep.getClientId().equals(Constants.BROKER_SERVICE_CLIENT_ID)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdminConsoleClient(RealmRepresentation rep) {
        if (rep.getClients() == null) return false;
        for (ClientRepresentation clientRep : rep.getClients()) {
            if (clientRep.getClientId().equals(Constants.ADMIN_CONSOLE_CLIENT_ID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Query users based on a search string:
     * <p/>
     * "Bill Burke" first and last name
     * "bburke@redhat.com" email
     * "Burke" lastname or username
     *
     * @param searchString
     * @param realmModel
     * @return
     */
    public List<UserModel> searchUsers(String searchString, RealmModel realmModel) {
        if (searchString == null) {
            return Collections.emptyList();
        }
        return session.users().searchForUser(searchString.trim(), realmModel);
    }

}
