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

package org.keycloak.federation.kerberos;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.services.managers.UserManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosFederationProvider implements UserFederationProvider {

    private static final Logger logger = Logger.getLogger(KerberosFederationProvider.class);
    public static final String KERBEROS_PRINCIPAL = "KERBEROS_PRINCIPAL";

    protected KeycloakSession session;
    protected UserFederationProviderModel model;
    protected KerberosConfig kerberosConfig;
    protected KerberosFederationProviderFactory factory;

    public KerberosFederationProvider(KeycloakSession session,UserFederationProviderModel model, KerberosFederationProviderFactory factory) {
        this.session = session;
        this.model = model;
        this.kerberosConfig = new KerberosConfig(model);
        this.factory = factory;
    }

    @Override
    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        if (!isValid(realm, local)) {
            return null;
        }

        if (kerberosConfig.getEditMode() == EditMode.READ_ONLY) {
            return new ReadOnlyKerberosUserModelDelegate(local, this);
        } else {
            return local;
        }
    }

    @Override
    public boolean synchronizeRegistrations() {
        return false;
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        KerberosUsernamePasswordAuthenticator authenticator = factory.createKerberosUsernamePasswordAuthenticator(kerberosConfig);
        if (authenticator.isUserAvailable(username)) {
            // Case when method was called with username including kerberos realm like john@REALM.ORG . Authenticator already checked that kerberos realm was correct
            if (username.contains("@")) {
                username = username.split("@")[0];
            }

            return findOrCreateAuthenticatedUser(realm, username);
        } else {
            return null;
        }
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public boolean isValid(RealmModel realm, UserModel local) {
        // KerberosUsernamePasswordAuthenticator.isUserAvailable is an overhead, so avoid it for now

        String kerberosPrincipal = local.getUsername() + "@" + kerberosConfig.getKerberosRealm();
        return kerberosPrincipal.equalsIgnoreCase(local.getFirstAttribute(KERBEROS_PRINCIPAL));
    }

    @Override
    public Set<String> getSupportedCredentialTypes(UserModel local) {
        Set<String> supportedCredTypes = new HashSet<String>();
        supportedCredTypes.add(UserCredentialModel.KERBEROS);

        if (kerberosConfig.isAllowPasswordAuthentication()) {
            boolean passwordSupported = true;
            if (kerberosConfig.getEditMode() == EditMode.UNSYNCED ) {

                // Password from KC database has preference over kerberos password
                for (UserCredentialValueModel cred : local.getCredentialsDirectly()) {
                    if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                        passwordSupported = false;
                    }
                }
            }

            if (passwordSupported) {
                supportedCredTypes.add(UserCredentialModel.PASSWORD);
            }
        }

        return supportedCredTypes;
    }

    @Override
    public Set<String> getSupportedCredentialTypes() {
        Set<String> supportedCredTypes = new HashSet<String>();
        supportedCredTypes.add(UserCredentialModel.KERBEROS);
        return supportedCredTypes;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return validPassword(user.getUsername(), cred.getValue());
            } else {
                return false; // invalid cred type
            }
        }
        return true;
    }

    protected boolean validPassword(String username, String password) {
        if (kerberosConfig.isAllowPasswordAuthentication()) {
            KerberosUsernamePasswordAuthenticator authenticator = factory.createKerberosUsernamePasswordAuthenticator(kerberosConfig);
            return authenticator.validUser(username, password);
        } else {
            return false;
        }
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return validCredentials(realm, user, Arrays.asList(input));
    }

    @Override
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential) {
        if (credential.getType().equals(UserCredentialModel.KERBEROS)) {
            String spnegoToken = credential.getValue();
            SPNEGOAuthenticator spnegoAuthenticator = factory.createSPNEGOAuthenticator(spnegoToken, kerberosConfig);

            spnegoAuthenticator.authenticate();

            Map<String, String> state = new HashMap<String, String>();
            if (spnegoAuthenticator.isAuthenticated()) {
                String username = spnegoAuthenticator.getAuthenticatedUsername();
                UserModel user = findOrCreateAuthenticatedUser(realm, username);
                if (user == null) {
                    return CredentialValidationOutput.failed();
                } else {
                    String delegationCredential = spnegoAuthenticator.getSerializedDelegationCredential();
                    if (delegationCredential != null) {
                        state.put(KerberosConstants.GSS_DELEGATION_CREDENTIAL, delegationCredential);
                    }

                    return new CredentialValidationOutput(user, CredentialValidationOutput.Status.AUTHENTICATED, state);
                }
            }  else {
                state.put(KerberosConstants.RESPONSE_TOKEN, spnegoAuthenticator.getResponseToken());
                return new CredentialValidationOutput(null, CredentialValidationOutput.Status.CONTINUE, state);
            }

        } else {
            return CredentialValidationOutput.failed();
        }
    }

    @Override
    public void close() {

    }

    /**
     * Called after successful authentication
     *
     * @param realm realm
     * @param username username without realm prefix
     * @return user if found or successfully created. Null if user with same username already exists, but is not linked to this provider
     */
    protected UserModel findOrCreateAuthenticatedUser(RealmModel realm, String username) {
        UserModel user = session.userStorage().getUserByUsername(username, realm);
        if (user != null) {
            logger.debug("Kerberos authenticated user " + username + " found in Keycloak storage");

            if (!model.getId().equals(user.getFederationLink())) {
                logger.warn("User with username " + username + " already exists, but is not linked to provider [" + model.getDisplayName() + "]");
                return null;
            } else {
                UserModel proxied = validateAndProxy(realm, user);
                if (proxied != null) {
                    return proxied;
                } else {
                    logger.warn("User with username " + username + " already exists and is linked to provider [" + model.getDisplayName() +
                            "] but kerberos principal is not correct. Kerberos principal on user is: " + user.getFirstAttribute(KERBEROS_PRINCIPAL));
                    logger.warn("Will re-create user");
                    new UserManager(session).removeUser(realm, user, session.userStorage());
                }
            }
        }

        logger.debug("Kerberos authenticated user " + username + " not in Keycloak storage. Creating him");
        return importUserToKeycloak(realm, username);
    }

    protected UserModel importUserToKeycloak(RealmModel realm, String username) {
        // Just guessing email from kerberos realm
        String email = username + "@" + kerberosConfig.getKerberosRealm().toLowerCase();

        logger.debugf("Creating kerberos user: %s, email: %s to local Keycloak storage", username, email);
        UserModel user = session.userStorage().addUser(realm, username);
        user.setEnabled(true);
        user.setEmail(email);
        user.setFederationLink(model.getId());
        user.setSingleAttribute(KERBEROS_PRINCIPAL, username + "@" + kerberosConfig.getKerberosRealm());

        if (kerberosConfig.isUpdateProfileFirstLogin()) {
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
        }

        return validateAndProxy(realm, user);
    }
}
