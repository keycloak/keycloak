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

import org.keycloak.Config;
import org.keycloak.common.Version;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.config.BootstrapAdminOptions;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.ServicesLogger;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.StringUtil;

import static org.keycloak.models.UserModel.IS_TEMP_ADMIN_ATTR_NAME;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplianceBootstrap {

    private final KeycloakSession session;

    public ApplianceBootstrap(KeycloakSession session) {
        this.session = session;
    }

    public boolean isNewInstall() {
        if (session.realms().getRealmByName(Config.getAdminRealm()) != null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isNoMasterUser() {
        RealmModel realm = session.realms().getRealmByName(Config.getAdminRealm());
        session.getContext().setRealm(realm);
        return session.users().getUsersCount(realm, true) == 0;
    }

    public boolean createMasterRealm() {
        if (!isNewInstall()) {
            throw new IllegalStateException("Can't create default realm as realms already exists");
        }

        String adminRealmName = Config.getAdminRealm();
        ServicesLogger.LOGGER.initializingAdminRealm(adminRealmName);

        RealmManager manager = new RealmManager(session);
        RealmModel realm = manager.createRealm(adminRealmName);
        realm.setName(adminRealmName);
        realm.setDisplayName(Version.NAME);
        realm.setDisplayNameHtml(Version.NAME_HTML);
        realm.setEnabled(true);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        realm.setDefaultSignatureAlgorithm(Constants.DEFAULT_SIGNATURE_ALGORITHM);
        realm.setSsoSessionIdleTimeout(1800);
        realm.setAccessTokenLifespan(60);
        realm.setAccessTokenLifespanForImplicitFlow(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT);
        realm.setSsoSessionMaxLifespan(36000);
        realm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);
        // KEYCLOAK-7688 Offline Session Max for Offline Token
        realm.setOfflineSessionMaxLifespanEnabled(false);
        realm.setOfflineSessionMaxLifespan(Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN);
        realm.setAccessCodeLifespan(60);
        realm.setAccessCodeLifespanUserAction(300);
        realm.setAccessCodeLifespanLogin(1800);
        realm.setSslRequired(SslRequired.EXTERNAL);
        realm.setRegistrationAllowed(false);
        realm.setRegistrationEmailAsUsername(false);

        session.getContext().setRealm(realm);
        DefaultKeyProviders.createProviders(realm);

        // In master realm the UP config is more relaxed
        // firstName, lastName and email are not required (all attributes except username)
        UserProfileProvider UserProfileProvider = session.getProvider(UserProfileProvider.class);
        UPConfig upConfig = UserProfileProvider.getConfiguration();
        for (UPAttribute attr : upConfig.getAttributes()) {
            if (!UserModel.USERNAME.equals(attr.getName())) {
                attr.setRequired(null);
            }
        }
        UserProfileProvider.setConfiguration(upConfig);

        return true;
    }

    /**
     * Create a temporary admin user
     * @param username
     * @param password
     * @param initialUser if true only create the user if no other users exist
     * @return false if the user could not be created
     */
    public boolean createMasterRealmAdminUser(String username, String password, boolean isTemporary, /*Integer expriationMinutes,*/ boolean initialUser) {
        RealmModel realm = session.realms().getRealmByName(Config.getAdminRealm());
        session.getContext().setRealm(realm);

        username = StringUtil.isBlank(username) ? BootstrapAdminOptions.DEFAULT_TEMP_ADMIN_USERNAME : username;
        //expriationMinutes = expriationMinutes == null ? DEFAULT_TEMP_ADMIN_EXPIRATION : expriationMinutes;

        if (initialUser && session.users().getUsersCount(realm, true) > 0) {
            ServicesLogger.LOGGER.addAdminUserFailedUsersExist(Config.getAdminRealm());
            return false;
        }

        try {
            UserModel adminUser = session.users().addUser(realm, username);
            adminUser.setEnabled(true);
            if (isTemporary) {
                adminUser.setSingleAttribute(IS_TEMP_ADMIN_ATTR_NAME, Boolean.TRUE.toString());
                // also set the expiration - could be relative to a creation timestamp, or computed
            }

            UserCredentialModel usrCredModel = UserCredentialModel.password(password);
            adminUser.credentialManager().updateCredential(usrCredModel);

            RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);
            adminUser.grantRole(adminRole);

            if (isTemporary)
                ServicesLogger.LOGGER.createdTemporaryAdminUser(username);
            else
                ServicesLogger.LOGGER.createdInitialAdminUser(username);
        } catch (ModelDuplicateException e) {
            ServicesLogger.LOGGER.addUserFailedUserExists(username, Config.getAdminRealm());
            return false;
        }
        return true;
    }

    /**
     * Create a temporary admin service account
     * @param clientId     the client ID
     * @param clientSecret the client secret
     * @return false if the service account could not be created
     */
    public boolean createTemporaryMasterRealmAdminService(String clientId, String clientSecret /*, Integer expriationMinutes*/) {
        RealmModel realm = session.realms().getRealmByName(Config.getAdminRealm());
        session.getContext().setRealm(realm);

        clientId = StringUtil.isBlank(clientId) ? BootstrapAdminOptions.DEFAULT_TEMP_ADMIN_SERVICE : clientId;
        //expriationMinutes = expriationMinutes == null ? DEFAULT_TEMP_ADMIN_EXPIRATION : expriationMinutes;

        ClientRepresentation adminClient = new ClientRepresentation();
        adminClient.setClientId(clientId);
        adminClient.setEnabled(true);
        adminClient.setServiceAccountsEnabled(true);
        adminClient.setStandardFlowEnabled(false);
        adminClient.setPublicClient(false);
        adminClient.setSecret(clientSecret);

        try {
            ClientModel adminClientModel = ClientManager.createClient(session, realm, adminClient);

            new ClientManager(new RealmManager(session)).enableServiceAccount(adminClientModel);
            UserModel serviceAccount = session.users().getServiceAccount(adminClientModel);
            RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);
            serviceAccount.grantRole(adminRole);

            adminClientModel.setAttribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString());
            adminClientModel.setAttribute(IS_TEMP_ADMIN_ATTR_NAME, Boolean.TRUE.toString());
            // also set the expiration - could be relative to a creation timestamp, or computed

            ServicesLogger.LOGGER.createdTemporaryAdminService(clientId);
        } catch (ModelDuplicateException e) {
            ServicesLogger.LOGGER.addClientFailedClientExists(clientId, Config.getAdminRealm());
            return false;
        }
        return true;
    }

    public void createMasterRealmUser(String username, String password, boolean isTemporary) {
        createMasterRealmAdminUser(username, password, isTemporary, true);
    }

}
