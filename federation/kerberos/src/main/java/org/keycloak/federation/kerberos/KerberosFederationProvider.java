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

import org.jboss.logging.Logger;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.credential.CredentialAuthentication;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosFederationProvider implements UserStorageProvider,
        UserLookupProvider.Streams,
        CredentialInputValidator,
        CredentialInputUpdater.Streams,
        CredentialAuthentication,
        ImportedUserValidation {

    private static final Logger logger = Logger.getLogger(KerberosFederationProvider.class);
    public static final String KERBEROS_PRINCIPAL = "KERBEROS_PRINCIPAL";

    protected KeycloakSession session;
    protected UserStorageProviderModel model;
    protected KerberosConfig kerberosConfig;
    protected KerberosFederationProviderFactory factory;

    public KerberosFederationProvider(KeycloakSession session, UserStorageProviderModel model, KerberosFederationProviderFactory factory) {
        this.session = session;
        this.model = model;
        this.kerberosConfig = new KerberosConfig(model);
        this.factory = factory;
    }

    @Override
    public UserModel validate(RealmModel realm, UserModel user) {
        if (!isValid(realm, user)) {
            return null;
        }

        if (kerberosConfig.getEditMode() == EditMode.READ_ONLY) {
            return new ReadOnlyKerberosUserModelDelegate(user, this);
        } else {
            return user;
        }
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
    public UserModel getUserById(RealmModel realm, String id) {
        return null;
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

    public boolean isValid(RealmModel realm, UserModel local) {
        // KerberosUsernamePasswordAuthenticator.isUserAvailable is an overhead, so avoid it for now

        String kerberosPrincipal = local.getUsername() + "@" + kerberosConfig.getKerberosRealm();
        return kerberosPrincipal.equalsIgnoreCase(local.getFirstAttribute(KERBEROS_PRINCIPAL));
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel) || !PasswordCredentialModel.TYPE.equals(input.getType())) return false;
        if (kerberosConfig.getEditMode() == EditMode.READ_ONLY) {
            throw new ReadOnlyException("Can't change password in Keycloak database. Change password with your Kerberos server");
        }
        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(UserCredentialModel.KERBEROS) || (kerberosConfig.isAllowPasswordAuthentication() && credentialType.equals(PasswordCredentialModel.TYPE));
    }

    @Override
    public boolean supportsCredentialAuthenticationFor(String type) {
        return UserCredentialModel.KERBEROS.equals(type);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return false;
        if (input.getType().equals(PasswordCredentialModel.TYPE) && !session.userCredentialManager().isConfiguredLocally(realm, user, PasswordCredentialModel.TYPE)) {
            return validPassword(user.getUsername(), input.getChallengeResponse());
        } else {
            return false; // invalid cred type
        }
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
    public CredentialValidationOutput authenticate(RealmModel realm, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return null;
        UserCredentialModel credential = (UserCredentialModel)input;
        if (credential.getType().equals(UserCredentialModel.KERBEROS)) {
            String spnegoToken = credential.getChallengeResponse();
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
            }  else if (spnegoAuthenticator.getResponseToken() != null) {
                // Case when SPNEGO handshake requires multiple steps
                logger.tracef("SPNEGO Handshake will continue");
                state.put(KerberosConstants.RESPONSE_TOKEN, spnegoAuthenticator.getResponseToken());
                return new CredentialValidationOutput(null, CredentialValidationOutput.Status.CONTINUE, state);
            } else {
                logger.tracef("SPNEGO Handshake not successful");
                return CredentialValidationOutput.failed();
            }

        } else {
            return null;
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
        UserModel user = session.userLocalStorage().getUserByUsername(realm, username);
        if (user != null) {
            user = session.users().getUserById(realm, user.getId());  // make sure we get a cached instance
            logger.debug("Kerberos authenticated user " + username + " found in Keycloak storage");

            if (!model.getId().equals(user.getFederationLink())) {
                logger.warn("User with username " + username + " already exists, but is not linked to provider [" + model.getName() + "]");
                return null;
            } else {
                UserModel proxied = validate(realm, user);
                if (proxied != null) {
                    return proxied;
                } else {
                    logger.warn("User with username " + username + " already exists and is linked to provider [" + model.getName() +
                            "] but kerberos principal is not correct. Kerberos principal on user is: " + user.getFirstAttribute(KERBEROS_PRINCIPAL));
                    logger.warn("Will re-create user");
                    new UserManager(session).removeUser(realm, user, session.userLocalStorage());
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
        UserModel user = session.userLocalStorage().addUser(realm, username);
        user.setEnabled(true);
        user.setEmail(email);
        user.setFederationLink(model.getId());
        user.setSingleAttribute(KERBEROS_PRINCIPAL, username + "@" + kerberosConfig.getKerberosRealm());

        if (kerberosConfig.isUpdateProfileFirstLogin()) {
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
        }

        return validate(realm, user);
    }
}
