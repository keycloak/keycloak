package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.enums.SslRequired;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OpenIDConnect;
import org.keycloak.representations.idm.ApplicationRepresentation;
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
        setupAdminConsole(realm);

        return realm;
    }

    protected void setupAdminConsole(RealmModel realm) {
        ApplicationModel adminConsole = realm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
        if (adminConsole == null) adminConsole = new ApplicationManager(this).createApplication(realm, Constants.ADMIN_CONSOLE_APPLICATION);
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
            String realmAdminApplicationName = getRealmAdminApplicationName(realm);
            ApplicationModel realmAdminApp = realm.getApplicationByName(realmAdminApplicationName);
            adminRole = realmAdminApp.getRole(AdminRoles.REALM_ADMIN);
        }
        adminConsole.addScopeMapping(adminRole);
    }

    public String getRealmAdminApplicationName(RealmModel realm) {
        return "realm-management";
    }

    public String getRealmAdminApplicationName(RealmRepresentation realm) {
        return "realm-management";
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
    }

    public boolean removeRealm(RealmModel realm) {
        List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();

        boolean removed = model.removeRealm(realm.getId());
        if (removed) {
            new ApplicationManager(this).removeApplication(getKeycloakAdminstrationRealm(), realm.getMasterAdminApp());

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
    }

    // Should be RealmManager moved to model/api instead of referencing methods this way?
    private void setupMasterAdminManagement(RealmModel realm) {
        ImportUtils.setupMasterAdminManagement(model, realm);
    }

    private void setupRealmAdminManagement(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) { return; } // don't need to do this for master realm

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));

        String realmAdminApplicationName = getRealmAdminApplicationName(realm);
        ApplicationModel realmAdminApp = realm.getApplicationByName(realmAdminApplicationName);
        if (realmAdminApp == null) {
            realmAdminApp = applicationManager.createApplication(realm, realmAdminApplicationName);
        }
        RoleModel adminRole = realmAdminApp.addRole(AdminRoles.REALM_ADMIN);
        realmAdminApp.setBearerOnly(true);
        realmAdminApp.setFullScopeAllowed(false);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.addRole(r);
            adminRole.addCompositeRole(role);
        }
    }


    private void setupAccountManagement(RealmModel realm) {
        ApplicationModel application = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APP);
        if (application == null) {
            application = new ApplicationManager(this).createApplication(realm, Constants.ACCOUNT_MANAGEMENT_APP);
            application.setEnabled(true);
            application.setFullScopeAllowed(false);
            String base = contextPath + "/realms/" + realm.getName() + "/account";
            String redirectUri = base + "/*";
            application.addRedirectUri(redirectUri);
            application.setBaseUrl(base);

            for (String role : AccountRoles.ALL) {
                application.addDefaultRole(role);
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
        if (!hasRealmAdminManagementApp(rep)) setupRealmAdminManagement(realm);
        if (!hasAccountManagementApp(rep)) setupAccountManagement(realm);
        if (!hasAdminConsoleApp(rep)) setupAdminConsole(realm);

        RepresentationToModel.importRealm(session, rep, realm);

        // Refresh periodic sync tasks for configured federationProviders
        List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();
        UsersSyncManager usersSyncManager = new UsersSyncManager();
        for (final UserFederationProviderModel fedProvider : federationProviders) {
            usersSyncManager.refreshPeriodicSyncForProvider(session.getKeycloakSessionFactory(), session.getProvider(TimerProvider.class), fedProvider, realm.getId());
        }
        return realm;
    }

    private boolean hasRealmAdminManagementApp(RealmRepresentation rep) {
        if (rep.getApplications() == null) return false;
        for (ApplicationRepresentation app : rep.getApplications()) {
            if (app.getName().equals(getRealmAdminApplicationName(rep))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAccountManagementApp(RealmRepresentation rep) {
        if (rep.getApplications() == null) return false;
        for (ApplicationRepresentation app : rep.getApplications()) {
            if (app.getName().equals(Constants.ACCOUNT_MANAGEMENT_APP)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdminConsoleApp(RealmRepresentation rep) {
        if (rep.getApplications() == null) return false;
        for (ApplicationRepresentation app : rep.getApplications()) {
            if (app.getName().equals(Constants.ADMIN_CONSOLE_APPLICATION)) {
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
