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

package org.keycloak.federation.sssd;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.federation.sssd.api.Sssd;
import org.keycloak.federation.sssd.api.Sssd.User;
import org.keycloak.federation.sssd.impl.PAMAuthenticator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import sun.security.util.Password;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SPI provider implementation to retrieve data from SSSD and authenticate
 * against PAM
 *
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class SSSDFederationProvider implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        ImportedUserValidation {

    private static final Logger logger = Logger.getLogger(SSSDFederationProvider.class);

    protected static final Set<String> supportedCredentialTypes = new HashSet<>();
    private final SSSDFederationProviderFactory factory;
    protected KeycloakSession session;
    protected UserStorageProviderModel model;

    public SSSDFederationProvider(KeycloakSession session, UserStorageProviderModel model, SSSDFederationProviderFactory sssdFederationProviderFactory) {
        this.session = session;
        this.model = model;
        this.factory = sssdFederationProviderFactory;
    }

    static {
        supportedCredentialTypes.add(PasswordCredentialModel.TYPE);
    }


    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return findOrCreateAuthenticatedUser(realm, username);
    }

    @Override
    public UserModel validate(RealmModel realm, UserModel user) {
        return validateAndProxy(realm, user);
    }

        /**
         * Called after successful authentication
         *
         * @param realm    realm
         * @param username username without realm prefix
         * @return user if found or successfully created. Null if user with same username already exists, but is not linked to this provider
         */
    protected UserModel findOrCreateAuthenticatedUser(RealmModel realm, String username) {
        UserModel user = session.userLocalStorage().getUserByUsername(username, realm);
        if (user != null) {
            logger.debug("SSSD authenticated user " + username + " found in Keycloak storage");

            if (!model.getId().equals(user.getFederationLink())) {
                logger.warn("User with username " + username + " already exists, but is not linked to provider [" + model.getName() + "]");
                return null;
            } else {
                UserModel proxied = validateAndProxy(realm, user);
                if (proxied != null) {
                    return proxied;
                } else {
                    logger.warn("User with username " + username + " already exists and is linked to provider [" + model.getName() +
                            "] but principal is not correct.");
                    logger.warn("Will re-create user");
                    new UserManager(session).removeUser(realm, user, session.userLocalStorage());
                }
            }
        }

        logger.debug("SSSD authenticated user " + username + " not in Keycloak storage. Creating...");
        return importUserToKeycloak(realm, username);
    }

    protected UserModel importUserToKeycloak(RealmModel realm, String username) {
        Sssd sssd = new Sssd(username);
        User sssdUser = sssd.getUser();
        logger.debugf("Creating SSSD user: %s to local Keycloak storage", username);
        UserModel user = session.userLocalStorage().addUser(realm, username);
        user.setEnabled(true);
        user.setEmail(sssdUser.getEmail());
        user.setFirstName(sssdUser.getFirstName());
        user.setLastName(sssdUser.getLastName());
        for (String s : sssd.getGroups()) {
            GroupModel group = KeycloakModelUtils.findGroupByPath(realm, "/" + s);
            if (group == null) {
                group = session.realms().createGroup(realm, s);
            }
            user.joinGroup(group);
        }
        user.setFederationLink(model.getId());
        return validateAndProxy(realm, user);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public void preRemove(RealmModel realm) {
        // complete  We don't care about the realm being removed
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // complete we dont'care if a role is removed

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        // complete we dont'care if a role is removed

    }

    public boolean isValid(RealmModel realm, UserModel local) {
        User user = new Sssd(local.getUsername()).getUser();
        return user.equals(local);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;

        UserCredentialModel cred = (UserCredentialModel)input;
        PAMAuthenticator pam = factory.createPAMAuthenticator(user.getUsername(), cred.getChallengeResponse());
        return (pam.authenticate() != null);
    }

    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        if (isValid(realm, local)) {
            return new ReadonlySSSDUserModelDelegate(local, this);
        } else {
            return null;
        }
    }

    @Override
    public void close() {
        Sssd.disconnect();
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        throw new IllegalStateException("You can't update your password as your account is read only.");
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.EMPTY_SET;
    }
}
