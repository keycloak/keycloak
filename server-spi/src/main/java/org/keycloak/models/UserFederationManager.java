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

package org.keycloak.models;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.services.managers.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserFederationManager implements UserProvider {

    private static final Logger logger = Logger.getLogger(UserFederationManager.class);

    protected KeycloakSession session;

    // Set of already validated/proxied federation users during this session. Key is user ID
    private Map<String, UserModel> managedUsers = new HashMap<>();

    public UserFederationManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        UserModel user = session.userStorage().addUser(realm, id, username.toLowerCase(), addDefaultRoles, addDefaultRequiredActions);
        return registerWithFederation(realm, user);
    }

    protected UserFederationProvider getFederationProvider(UserFederationProviderModel model) {
        return KeycloakModelUtils.getFederationProviderInstance(session, model);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserModel user = session.userStorage().addUser(realm, username.toLowerCase());
        return registerWithFederation(realm, user);
    }

    protected UserModel registerWithFederation(RealmModel realm, UserModel user) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            if (fed.synchronizeRegistrations()) {
                user.setFederationLink(federation.getId());
                UserModel registered = fed.register(realm, user);
                managedUsers.put(registered.getId(), registered);
                return registered;
            }
        }
        return user;
    }

    protected UserFederationProvider getFederationLink(RealmModel realm, UserModel user) {
        if (user.getFederationLink() == null) return null;
        for (UserFederationProviderModel fed : realm.getUserFederationProviders()) {
            if (fed.getId().equals(user.getFederationLink())) {
                return getFederationProvider(fed);
            }
        }
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        UserFederationProvider link = getFederationLink(realm, user);
        if (link != null) {
            boolean fedRemoved = link.removeUser(realm, user);
            if (fedRemoved) {
                boolean localRemoved = session.userStorage().removeUser(realm, user);
                managedUsers.remove(user.getId());
                if (!localRemoved) {
                    logger.warn("User possibly removed from federation provider, but failed to remove him from keycloak model");
                }
                return localRemoved;
            } else {
                logger.warn("Failed to remove user from federation provider");
                return false;
            }
        }
        return session.userStorage().removeUser(realm, user);

    }

    protected void validateUser(RealmModel realm, UserModel user) {
        if (managedUsers.containsKey(user.getId())) {
            return;
        }

        UserFederationProvider link = getFederationLink(realm, user);
        if (link != null  && !link.isValid(realm, user)) {
            deleteInvalidUser(realm, user);
            throw new IllegalStateException("Federated user no longer valid");
        }

    }

    protected void deleteInvalidUser(final RealmModel realm, final UserModel user) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                RealmModel realmModel = session.realms().getRealm(realm.getId());
                if (realmModel == null) return;
                UserModel deletedUser = session.userStorage().getUserById(user.getId(), realmModel);
                new UserManager(session).removeUser(realmModel, deletedUser, session.userStorage());
                logger.debugf("Removed invalid user '%s'", user.getUsername());
            }

        });
    }


    protected UserModel validateAndProxyUser(RealmModel realm, UserModel user) {
        UserModel managed = managedUsers.get(user.getId());
        if (managed != null) {
            return managed;
        }

        UserFederationProvider link = getFederationLink(realm, user);
        if (link != null) {
            UserModel validatedProxyUser = link.validateAndProxy(realm, user);
            if (validatedProxyUser != null) {
                managedUsers.put(user.getId(), validatedProxyUser);
                return validatedProxyUser;
            } else {
                deleteInvalidUser(realm, user);
                return null;
            }
        }
        return user;
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        validateUser(realm, user);
        session.userStorage().addFederatedIdentity(realm, user, socialLink);
    }

    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        session.userStorage().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        validateUser(realm, user);
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        return session.userStorage().removeFederatedIdentity(realm, user, socialProvider);
    }

    @Override
    public void addConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        validateUser(realm, user);
        session.userStorage().addConsent(realm, user, consent);

    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, UserModel user, String clientInternalId) {
        validateUser(realm, user);
        return session.userStorage().getConsentByClient(realm, user, clientInternalId);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, UserModel user) {
        validateUser(realm, user);
        return session.userStorage().getConsents(realm, user);
    }

    @Override
    public void updateConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        validateUser(realm, user);
        session.userStorage().updateConsent(realm, user, consent);

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, UserModel user, String clientInternalId) {
        validateUser(realm, user);
        return session.userStorage().revokeConsentForClient(realm, user, clientInternalId);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        UserModel user = session.userStorage().getUserById(id, realm);
        if (user != null) {
            user = validateAndProxyUser(realm, user);
        }
        return user;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, final GroupModel group, int firstResult, int maxResults) {
        // Not very effective. For the page X, it is loading also all previous pages 0..X-1 . Improve if needed...
        int maxTotal = firstResult + maxResults;
        List<UserModel> localMembers = query(new PaginatedQuery() {

            @Override
            public List<UserModel> query(RealmModel realm, int first, int max) {
                return session.userStorage().getGroupMembers(realm, group, first, max);
            }

        }, realm, 0, maxTotal);

        Set<UserModel> result = new LinkedHashSet<>(localMembers);

        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            if (result.size() >= maxTotal) {
                break;
            }

            int max = maxTotal - result.size();

            UserFederationProvider fed = getFederationProvider(federation);
            List<UserModel> current = fed.getGroupMembers(realm, group, 0, max);
            if (current != null) {
                result.addAll(current);
            }
        }

        if (result.size() <= firstResult) {
            return Collections.emptyList();
        }

        int max = Math.min(maxTotal, result.size());
        return new ArrayList<>(result).subList(firstResult, max);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, -1, -1);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel user = session.userStorage().getUserByUsername(username.toLowerCase(), realm);
        if (user != null) {
            user = validateAndProxyUser(realm, user);
            if (user != null) return user;
        }
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            user = fed.getUserByUsername(realm, username);
            if (user != null) return user;
        }
        return user;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        UserModel user = session.userStorage().getUserByEmail(email.toLowerCase(), realm);
        if (user != null) {
            user = validateAndProxyUser(realm, user);
            if (user != null) return user;
        }
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            user = fed.getUserByEmail(realm, email);
            if (user != null) return user;
        }
        return user;
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        UserModel user = session.userStorage().getUserByFederatedIdentity(socialLink, realm);
        if (user != null) {
            user = validateAndProxyUser(realm, user);
        }
        return user;
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        UserModel user = session.userStorage().getServiceAccount(client);
        if (user != null) {
            user = validateAndProxyUser(client.getRealm(), user);
        }
        return user;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return getUsers(realm, 0, Integer.MAX_VALUE - 1, includeServiceAccounts);

    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, false);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return getUsers(realm, firstResult, maxResults, false);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return session.userStorage().getUsersCount(realm);
    }

    interface PaginatedQuery {
        List<UserModel> query(RealmModel realm, int first, int max);
    }

    protected List<UserModel> query(PaginatedQuery pagedQuery, RealmModel realm, int firstResult, int maxResults) {
        List<UserModel> results = new LinkedList<UserModel>();
        if (maxResults == 0) return results;
        int first = firstResult;
        int max = maxResults;
        do {
            List<UserModel> query = pagedQuery.query(realm, first, max);
            if (query == null || query.size() == 0) return results;
            int added = 0;
            for (UserModel user : query) {
                user = validateAndProxyUser(realm, user);
                if (user == null) continue;
                results.add(user);
                added++;
            }
            if (results.size() == maxResults) return results;
            if (query.size() < max) return results;
            first = query.size();
            max -= added;
            if (max <= 0) return results;
        } while (true);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, final boolean includeServiceAccounts) {
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(RealmModel realm, int first, int max) {
                return session.userStorage().getUsers(realm, first, max, includeServiceAccounts);
            }
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE - 1);
    }

    void federationLoad(RealmModel realm, Map<String, String> attributes) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            fed.searchByAttributes(attributes, realm, 30);
        }
    }

    @Override
    public List<UserModel> searchForUser(final String search, RealmModel realm, int firstResult, int maxResults) {
        Map<String, String> attributes = new HashMap<String, String>();
        int spaceIndex = search.lastIndexOf(' ');
        if (spaceIndex > -1) {
            String firstName = search.substring(0, spaceIndex).trim();
            String lastName = search.substring(spaceIndex).trim();
            attributes.put(UserModel.FIRST_NAME, firstName);
            attributes.put(UserModel.LAST_NAME, lastName);
        } else if (search.indexOf('@') > -1) {
            attributes.put(UserModel.USERNAME, search.trim().toLowerCase());
            attributes.put(UserModel.EMAIL, search.trim().toLowerCase());
        } else {
            attributes.put(UserModel.LAST_NAME, search.trim());
            attributes.put(UserModel.USERNAME, search.trim().toLowerCase());
        }
        federationLoad(realm, attributes);
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(RealmModel realm, int first, int max) {
                return session.userStorage().searchForUser(search, realm, first, max);
            }
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return searchForUser(attributes, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(final Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        federationLoad(realm, attributes);
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(RealmModel realm, int first, int max) {
                return session.userStorage().searchForUser(attributes, realm, first, max);
            }
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return session.userStorage().searchForUserByUserAttribute(attrName, attrValue, realm);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        validateUser(realm, user);
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        return session.userStorage().getFederatedIdentities(user, realm);
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        validateUser(realm, user);
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        return session.userStorage().getFederatedIdentity(user, socialProvider, realm);
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        // not federation-aware for now
        session.userStorage().grantToAllUsers(realm, role);
    }

    @Override
    public void preRemove(RealmModel realm) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            fed.preRemove(realm);
        }
        session.userStorage().preRemove(realm);
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel model) {
        session.userStorage().preRemove(realm, model);
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            fed.preRemove(realm, group);
        }
        session.userStorage().preRemove(realm, group);

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            fed.preRemove(realm, role);
        }
        session.userStorage().preRemove(realm, role);
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        session.userStorage().preRemove(realm, client);
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        session.userStorage().preRemove(protocolMapper);
    }

    public void updateCredential(RealmModel realm, UserModel user, UserCredentialModel credential) {
        if (credential.getType().equals(UserCredentialModel.PASSWORD)) {
            if (realm.getPasswordPolicy() != null) {
                PolicyError error = session.getProvider(PasswordPolicyManagerProvider.class).validate(user, credential.getValue());
                if (error != null) throw new ModelException(error.getMessage(), error.getParameters());
            }
        }
        user.updateCredential(credential);
    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        UserFederationProvider link = getFederationLink(realm, user);
        if (link != null) {
            validateUser(realm, user);
            Set<String> supportedCredentialTypes = link.getSupportedCredentialTypes(user);
            if (supportedCredentialTypes.size() > 0) {
                List<UserCredentialModel> fedCreds = new ArrayList<UserCredentialModel>();
                List<UserCredentialModel> localCreds = new ArrayList<UserCredentialModel>();
                for (UserCredentialModel cred : input) {
                    if (supportedCredentialTypes.contains(cred.getType())) {
                        fedCreds.add(cred);
                    } else {
                        localCreds.add(cred);
                    }
                }
                if (!link.validCredentials(realm, user, fedCreds)) {
                    return false;
                }
                return session.userStorage().validCredentials(session, realm, user, localCreds);
            }
        }
        return session.userStorage().validCredentials(session, realm, user, input);
    }

    /**
     * Is the user configured to use this credential type
     *
     * @return
     */
    public boolean configuredForCredentialType(String type, RealmModel realm, UserModel user) {
        UserFederationProvider link = getFederationLink(realm, user);
        if (link != null) {
            Set<String> supportedCredentialTypes = link.getSupportedCredentialTypes(user);
            if (supportedCredentialTypes.contains(type)) return true;
        }
        if (UserCredentialModel.isOtp(type)) {
            if (!user.isOtpEnabled()) return false;
        }

        List<UserCredentialValueModel> creds = user.getCredentialsDirectly();
        for (UserCredentialValueModel cred : creds) {
            if (cred.getType().equals(type)) {
                if (UserCredentialModel.isOtp(type)) {
                    OTPPolicy otpPolicy = realm.getOTPPolicy();
                    if (!cred.getAlgorithm().equals(otpPolicy.getAlgorithm())
                        || cred.getDigits() != otpPolicy.getDigits()) {
                        return false;
                    }
                    if (type.equals(UserCredentialModel.TOTP) && cred.getPeriod() != otpPolicy.getPeriod()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }





    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, UserCredentialModel... input) {
        return validCredentials(session, realm, user, Arrays.asList(input));
    }

    @Override
    public CredentialValidationOutput validCredentials(KeycloakSession session, RealmModel realm, UserCredentialModel... input) {
        List<UserFederationProviderModel> fedProviderModels = realm.getUserFederationProviders();
        List<UserFederationProvider> fedProviders = new ArrayList<UserFederationProvider>();
        for (UserFederationProviderModel fedProviderModel : fedProviderModels) {
            fedProviders.add(getFederationProvider(fedProviderModel));
        }

        CredentialValidationOutput result = null;
        for (UserCredentialModel cred : input) {
            UserFederationProvider providerSupportingCreds = null;

            // Find first provider, which supports required credential type
            for (UserFederationProvider fedProvider : fedProviders) {
                if (fedProvider.getSupportedCredentialTypes().contains(cred.getType())) {
                    providerSupportingCreds = fedProvider;
                    break;
                }
            }

            if (providerSupportingCreds == null) {
                logger.warn("Don't have provider supporting credentials of type " + cred.getType());
                return CredentialValidationOutput.failed();
            }

            logger.debug("Found provider [" + providerSupportingCreds + "] supporting credentials of type " + cred.getType());
            CredentialValidationOutput currentResult = providerSupportingCreds.validCredentials(realm, cred);
            result = (result == null) ? currentResult : result.merge(currentResult);
        }

        // For now, validCredentials(realm, input) is not supported for local userProviders
        return (result != null) ? result : CredentialValidationOutput.failed();
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {

    }

    @Override
    public void close() {
    }
}
