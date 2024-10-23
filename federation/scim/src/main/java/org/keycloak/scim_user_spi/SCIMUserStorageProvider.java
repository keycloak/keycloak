/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.scim_user_spi;

import org.jboss.logging.Logger;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialAuthentication;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.broker.provider.util.SimpleHttp;

import org.keycloak.scim_user_spi.authenticator.SCIMAuthenticator;
import org.keycloak.scim_user_spi.schemas.SCIMError;
import org.keycloak.scim_user_spi.schemas.SCIMUser;

import org.keycloak.storage.user.UserQueryProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;

/**
 * @author <a href="mailto:jstephen@redhat.com">Justin Stephenson</a>
 * @version $Revision: 1 $
 */
public class SCIMUserStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator,
        CredentialAuthentication, UserRegistrationProvider, UserQueryProvider, ImportedUserValidation {
    protected KeycloakSession session;
    protected ComponentModel model;
    protected Scim scim;
    private static final Logger logger = Logger.getLogger(SCIMUserStorageProvider.class);
    protected final Set<String> supportedCredentialTypes = new HashSet<>();
    protected SCIMUserStorageProviderFactory factory;

    public SCIMUserStorageProvider(KeycloakSession session, ComponentModel model, Scim scim,
            SCIMUserStorageProviderFactory factory) {
        this.session = session;
        this.model = model;
        this.scim = scim;
        this.factory = factory;

        supportedCredentialTypes.add(PasswordCredentialModel.TYPE);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(realm, username);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        /*
         * Remove @realm, this is needed as GSSAPI auth users reach here as user@realm
         */
        int idx = username.indexOf("@");
        if (idx != -1) {
            username = username.substring(0, idx);
        }

        UserModel user = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, username);
        if (user != null) {
            logger.info("User already exists in keycloak");
            return user;
        } else {
            return createUserInKeycloak(realm, username);
        }
    }

    protected UserModel createUserInKeycloak(RealmModel realm, String username) {
        SCIMUser scimuser = scim.getUserByUsername(username);
        if (scimuser.getTotalResults() == 0) {
            return null;
        }
        UserModel user = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, username);
        user.setEmail(scim.getEmail(scimuser));
        user.setFirstName(scim.getFirstName(scimuser));
        user.setLastName(scim.getLastName(scimuser));
        user.setFederationLink(model.getId());
        user.setEnabled(scim.getActive(scimuser));

        for (String name : scim.getGroupsList(scimuser)) {
            Stream<GroupModel> groupsStream = session.groups().searchForGroupByNameStream(realm, name, false, null, null);
            GroupModel group = groupsStream.findFirst().orElse(null);

            if (group == null) {
                logger.infov("No group found, creating group: {0}", name);
                group = session.groups().createGroup(realm, name);
            }
            user.joinGroup(group);
        }

        logger.infov("Creating SCIM user {0} in keycloak", username);
        return new SCIMUserModelDelegate(scim, user, model);
    }

    @Override
    public void close() {

    }

    public Set<String> getSupportedCredentialTypes() {
        return new HashSet<String>(this.supportedCredentialTypes);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return getSupportedCredentialTypes().contains(credentialType);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getSupportedCredentialTypes().contains(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel))
            return false;

        /*
         * The password can either be validated locally in keycloak (tried first) or in the SCIM server
         */
        if (((UserCredentialManager) user.credentialManager()).isConfiguredLocally(input.getType())) {
            logger.infov("Local password validation for {0}", user.getUsername());
            /* return false in order to fallback to the next validator */
            return false;
        } else {
            logger.infov("Delegated password validation for {0}", user.getUsername());
            Scim scim = this.scim;
            return scim.isValid(user.getUsername(), input.getChallengeResponse());
        }
    }

    @Override
    public UserModel validate(RealmModel realm, UserModel local) {
        Scim scim = this.scim;

        SCIMUser scimuser = scim.getUserByUsername(local.getUsername());
        String fname = scim.getFirstName(scimuser);
        String lname = scim.getLastName(scimuser);
        String email = scim.getEmail(scimuser);

        if (!local.getFirstName().equals(fname)) {
            local.setFirstName(fname);
        }
        if (!local.getLastName().equals(lname)) {
            local.setLastName(lname);
        }
        if (!local.getEmail().equals(email)) {
            local.setEmail(email);
        }

        return new SCIMUserModelDelegate(this.scim, local, model);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        Scim scim = this.scim;

        SimpleHttp.Response resp = scim.createUser(username);

        try {
            if (resp.getStatus() != HttpStatus.SC_CREATED) {
                logger.warn("Unexpected create status code returned");
                SCIMError error = resp.asJson(SCIMError.class);
                logger.warn(error.getDetail());
                resp.close();
                return null;
            }
            resp.close();
        } catch (IOException e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }

        return createUserInKeycloak(realm, username);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        logger.infov("Removing user: {0}", user.getUsername());
        Scim scim = this.scim;

        SimpleHttp.Response resp = scim.deleteUser(user.getUsername());
        Boolean status = false;
        try {
            status = resp.getStatus() == HttpStatus.SC_NO_CONTENT;
            resp.close();
        } catch (IOException e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }
        return status;
    }

    private Stream<UserModel> performSearch(RealmModel realm, String search) {
        List<UserModel> users = new LinkedList<>();
        Scim scim = this.scim;

        SCIMUser scimuser = scim.getUserByUsername(search);
        if (scimuser.getTotalResults() > 0) {
            logger.info("User found by username!");
            if (UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, search) == null) {
                UserModel user = getUserByUsername(realm, scim.getUserName(scimuser));
                users.add(user);
            } else {
                logger.info("User exists!");
            }

            return users.stream();
        }

        return users.stream();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel arg0, GroupModel arg1, Integer arg2, Integer arg3) {
        return Stream.empty();
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        Scim scim = this.scim;

        SCIMUser user = null;
        SimpleHttp.Response response;
        try {
            response = scim.clientRequest("/Users", "GET", null);
            user = response.asJson(SCIMUser.class);
            response.close();
        } catch (Exception e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }

        return user.getTotalResults();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult,
            Integer maxResults) {
        String search = params.get(UserModel.SEARCH);
        /* only supports searching by username */
        if (search == null)
            return Stream.empty();
        return performSearch(realm, search);
    }

    @Override
    public boolean supportsCredentialAuthenticationFor(String type) {
        return UserCredentialModel.KERBEROS.equals(type);
    }

    @Override
    public CredentialValidationOutput authenticate(RealmModel realm, CredentialInput input) {
        Map<String, String> state = new HashMap<>();
        String username = null;
        SCIMAuthenticator scimAuthenticator = factory.createSCIMAuthenticator();

        String token = scimAuthenticator.getToken(session);
        if (token != null) {
            username = scim.gssAuth(token);

            /* Remove realm */
            int idx = username.indexOf("@");
            if (idx != -1) {
                username = username.substring(0, idx);
            }
            logger.info("GSSAPI authenticating with user " + username);
        }

        UserModel user = getUserByUsername(realm, username);
        if (user == null) {
            logger.info("CredentialValidationOutput failed");
            return CredentialValidationOutput.failed();
        }
        logger.info("CredentialValidationOutput success!");
        return new CredentialValidationOutput(user, CredentialValidationOutput.Status.AUTHENTICATED, state);
    }
}
