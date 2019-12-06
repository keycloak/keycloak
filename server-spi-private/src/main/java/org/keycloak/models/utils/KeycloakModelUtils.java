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

package org.keycloak.models.utils;

import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.transaction.JtaTransactionManagerLookup;

import javax.crypto.spec.SecretKeySpec;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Set of helper methods, which are useful in various model implementations.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public final class KeycloakModelUtils {

    private KeycloakModelUtils() {
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static byte[] generateSecret() {
        return generateSecret(32);
    }

    public static byte[] generateSecret(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return buf;
    }

    public static PublicKey getPublicKey(String publicKeyPem) {
        if (publicKeyPem != null) {
            try {
                return PemUtils.decodePublicKey(publicKeyPem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    public static X509Certificate getCertificate(String cert) {
        if (cert != null) {
            try {
                return PemUtils.decodeCertificate(cert);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }


    public static PrivateKey getPrivateKey(String privateKeyPem) {
        if (privateKeyPem != null) {
            try {
                return PemUtils.decodePrivateKey(privateKeyPem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static Key getSecretKey(String secret) {
        return secret != null ? new SecretKeySpec(secret.getBytes(), "HmacSHA256") : null;
    }

    public static String getPemFromKey(Key key) {
        return PemUtils.encodeKey(key);
    }

    public static String getPemFromCertificate(X509Certificate certificate) {
        return PemUtils.encodeCertificate(certificate);
    }

    public static CertificateRepresentation generateKeyPairCertificate(String subject) {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject);

        String privateKeyPem = PemUtils.encodeKey(keyPair.getPrivate());
        String certPem = PemUtils.encodeCertificate(certificate);

        CertificateRepresentation rep = new CertificateRepresentation();
        rep.setPrivateKey(privateKeyPem);
        rep.setCertificate(certPem);
        return rep;
    }

    public static UserCredentialModel generateSecret(ClientModel client) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        client.setSecret(secret.getChallengeResponse());
        return secret;
    }

    public static String getDefaultClientAuthenticatorType() {
        return "client-secret";
    }

    public static String generateCodeSecret() {
        return UUID.randomUUID().toString();
    }

    public static ClientModel createClient(RealmModel realm, String name) {
        ClientModel app = realm.addClient(name);
        app.setClientAuthenticatorType(getDefaultClientAuthenticatorType());
        generateSecret(app);
        app.setFullScopeAllowed(true);

        return app;
    }

    /**
     * Deep search if given role is descendant of composite role
     *
     * @param role      role to check
     * @param composite composite role
     * @param visited   set of already visited roles (used for recursion)
     * @return true if "role" is descendant of "composite"
     */
    public static boolean searchFor(RoleModel role, RoleModel composite, Set<String> visited) {
        if (visited.contains(composite.getId())) {
            return false;
        }

        visited.add(composite.getId());

        if (!composite.isComposite()) {
            return false;
        }

        Set<RoleModel> compositeRoles = composite.getComposites();
        return compositeRoles.contains(role) ||
                        compositeRoles.stream()
                                .filter(x -> x.isComposite() && searchFor(role, x, visited))
                                .findFirst()
                                .isPresent();
    }

    /**
     * Try to find user by username or email for authentication
     *
     * @param realm    realm
     * @param username username or email of user
     * @return found user
     */
    public static UserModel findUserByNameOrEmail(KeycloakSession session, RealmModel realm, String username) {
        if (realm.isLoginWithEmailAllowed() && username.indexOf('@') != -1) {
            UserModel user = session.users().getUserByEmail(username, realm);
            if (user != null) {
                return user;
            }
        }

        return session.users().getUserByUsername(username, realm);
    }

    /**
     * Wrap given runnable job into KeycloakTransaction.
     *
     * @param factory
     * @param task
     */
    public static void runJobInTransaction(KeycloakSessionFactory factory, KeycloakSessionTask task) {
        KeycloakSession session = factory.create();
        KeycloakTransaction tx = session.getTransactionManager();
        try {
            tx.begin();
            task.run(session);

            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
        } catch (RuntimeException re) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            session.close();
        }
    }


    /**
     * Wrap given runnable job into KeycloakTransaction. Set custom timeout for the JTA transaction (in case we're in the environment with JTA enabled)
     *
     * @param factory
     * @param task
     * @param timeoutInSeconds
     */
    public static void runJobInTransactionWithTimeout(KeycloakSessionFactory factory, KeycloakSessionTask task, int timeoutInSeconds) {
        JtaTransactionManagerLookup lookup = (JtaTransactionManagerLookup)factory.getProviderFactory(JtaTransactionManagerLookup.class);
        try {
            if (lookup != null) {
                if (lookup.getTransactionManager() != null) {
                    try {
                        lookup.getTransactionManager().setTransactionTimeout(timeoutInSeconds);
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            runJobInTransaction(factory, task);

        } finally {
            if (lookup != null) {
                if (lookup.getTransactionManager() != null) {
                    try {
                        // Reset to default transaction timeout
                        lookup.getTransactionManager().setTransactionTimeout(0);
                    } catch (SystemException e) {
                        // Shouldn't happen for Wildfly transaction manager
                        throw new RuntimeException(e);
                    }
                }
            }
        }

    }


    public static String getMasterRealmAdminApplicationClientId(String realmName) {
        return realmName + "-realm";
    }

    // USER FEDERATION RELATED STUFF


    public static UserStorageProviderModel findUserStorageProviderByName(String displayName, RealmModel realm) {
        if (displayName == null) {
            return null;
        }

        for (UserStorageProviderModel fedProvider : realm.getUserStorageProviders()) {
            if (displayName.equals(fedProvider.getName())) {
                return fedProvider;
            }
        }
        return null;
    }

    public static UserStorageProviderModel findUserStorageProviderById(String fedProviderId, RealmModel realm) {
        for (UserStorageProviderModel fedProvider : realm.getUserStorageProviders()) {
            if (fedProviderId.equals(fedProvider.getId())) {
                return fedProvider;
            }
        }
        return null;
    }

    public static ComponentModel createComponentModel(String name, String parentId, String providerId, String providerType, String... config) {
        ComponentModel mapperModel = new ComponentModel();
        mapperModel.setParentId(parentId);
        mapperModel.setName(name);
        mapperModel.setProviderId(providerId);
        mapperModel.setProviderType(providerType);

        String key = null;
        for (String configEntry : config) {
            if (key == null) {
                key = configEntry;
            } else {
                mapperModel.getConfig().add(key, configEntry);
                key = null;
            }
        }
        if (key != null) {
            throw new IllegalStateException("Invalid count of arguments for config. Maybe mistake?");
        }

        return mapperModel;
    }


    // END USER FEDERATION RELATED STUFF

    public static String toLowerCaseSafe(String str) {
        return str==null ? null : str.toLowerCase();
    }

    public static RoleModel setupOfflineRole(RealmModel realm) {
        RoleModel offlineRole = realm.getRole(Constants.OFFLINE_ACCESS_ROLE);

        if (offlineRole == null) {
            offlineRole = realm.addRole(Constants.OFFLINE_ACCESS_ROLE);
            offlineRole.setDescription("${role_offline-access}");
            realm.addDefaultRole(Constants.OFFLINE_ACCESS_ROLE);
        }

        return offlineRole;
    }


    /**
     * Recursively find all AuthenticationExecutionModel from specified flow or all it's subflows
     *
     * @param realm
     * @param flow
     * @param result input should be empty list. At the end will be all executions added to this list
     */
    public static void deepFindAuthenticationExecutions(RealmModel realm, AuthenticationFlowModel flow, List<AuthenticationExecutionModel> result) {
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(flow.getId());
        for (AuthenticationExecutionModel execution : executions) {
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                deepFindAuthenticationExecutions(realm, subFlow, result);
            } else {
                result.add(execution);
            }
        }
    }

    public static String resolveFirstAttribute(GroupModel group, String name) {
        String value = group.getFirstAttribute(name);
        if (value != null) return value;
        if (group.getParentId() == null) return null;
        return resolveFirstAttribute(group.getParent(), name);

    }

    /**
     *
     *
     * @param user
     * @param name
     * @return
     */
    public static String resolveFirstAttribute(UserModel user, String name) {
        String value = user.getFirstAttribute(name);
        if (value != null) return value;
        for (GroupModel group : user.getGroups()) {
            value = resolveFirstAttribute(group, name);
            if (value != null) return value;
        }
        return null;

    }

    public static List<String>  resolveAttribute(GroupModel group, String name) {
        List<String> values = group.getAttribute(name);
        if (values != null && !values.isEmpty()) return values;
        if (group.getParentId() == null) return null;
        return resolveAttribute(group.getParent(), name);

    }


    public static Collection<String> resolveAttribute(UserModel user, String name, boolean aggregateAttrs) {
        List<String> values = user.getAttribute(name);
        Set<String> aggrValues = new HashSet<String>();
        if (!values.isEmpty()) {
            if (!aggregateAttrs) {
                return values;
            }
            aggrValues.addAll(values);
        }
        for (GroupModel group : user.getGroups()) {
            values = resolveAttribute(group, name);
            if (values != null && !values.isEmpty()) {
                if (!aggregateAttrs) {
                    return values;
                }
                aggrValues.addAll(values);
            }
        }
        return aggrValues;
    }


    private static GroupModel findSubGroup(String[] segments, int index, GroupModel parent) {
        for (GroupModel group : parent.getSubGroups()) {
            String groupName = group.getName();
            String[] pathSegments = formatPathSegments(segments, index, groupName);

            if (groupName.equals(pathSegments[index])) {
                if (pathSegments.length == index + 1) {
                    return group;
                }
                else {
                    if (index + 1 < pathSegments.length) {
                        GroupModel found = findSubGroup(pathSegments, index + 1, group);
                        if (found != null) return found;
                    } else {
                        return null;
                    }
                }

            }
        }
        return null;
    }

    /**
     * Given the {@code pathParts} of a group with the given {@code groupName}, format the {@pathParts} in order to ignore
     * group names containing a {@code /} character.
     *
     * @param segments the path segments
     * @param index the index pointing to the position to start looking for the group name
     * @param groupName the groupName
     * @return a new array of strings with the correct segments in case the group has a name containing slashes
     */
    private static String[] formatPathSegments(String[] segments, int index, String groupName) {
        String[] nameSegments = groupName.split("/");

        if (nameSegments.length > 1 && segments.length >= nameSegments.length) {
            for (int i = 0; i < nameSegments.length; i++) {
                if (!nameSegments[i].equals(segments[index + i])) {
                    return segments;
                }
            }

            int numMergedIndexes = nameSegments.length - 1;
            String[] newPath = new String[segments.length - numMergedIndexes];

            for (int i = 0; i < newPath.length; i++) {
                if (i == index) {
                    newPath[i] = groupName;
                } else if (i > index) {
                    newPath[i] = segments[i + numMergedIndexes];
                } else {
                    newPath[i] = segments[i];
                }
            }

            return newPath;
        }

        return segments;
    }

    public static GroupModel findGroupByPath(RealmModel realm, String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String[] split = path.split("/");
        if (split.length == 0) return null;
        GroupModel found = null;
        for (GroupModel group : realm.getTopLevelGroups()) {
            String groupName = group.getName();
            String[] pathSegments = formatPathSegments(split, 0, groupName);

            if (groupName.equals(pathSegments[0])) {
                if (pathSegments.length == 1) {
                    found = group;
                    break;
                }
                else {
                    if (pathSegments.length > 1) {
                        found = findSubGroup(pathSegments, 1, group);
                        if (found != null) break;
                    }
                }

            }
        }
        return found;
    }

    public static Set<RoleModel> getClientScopeMappings(ClientModel client, ScopeContainerModel container) {
        Set<RoleModel> mappings = container.getScopeMappings();
        Set<RoleModel> result = new HashSet<>();
        for (RoleModel role : mappings) {
            RoleContainerModel roleContainer = role.getContainer();
            if (roleContainer instanceof ClientModel) {
                if (client.getId().equals(((ClientModel)roleContainer).getId())) {
                    result.add(role);
                }

            }
        }
        return result;
    }

    // Used in various role mappers
    public static RoleModel getRoleFromString(RealmModel realm, String roleName) {
        // Check client roles for all possible splits by dot
        int scopeIndex = roleName.lastIndexOf('.');
        while (scopeIndex >= 0) {
            String appName = roleName.substring(0, scopeIndex);
            ClientModel client = realm.getClientByClientId(appName);
            if (client != null) {
                String role = roleName.substring(scopeIndex + 1);
                return client.getRole(role);
            }

            scopeIndex = roleName.lastIndexOf('.', scopeIndex - 1);
        }

        // determine if roleName is a realm role
        return realm.getRole(roleName);
    }

    // Used for hardcoded role mappers
    public static String[] parseRole(String role) {
        int scopeIndex = role.lastIndexOf('.');
        if (scopeIndex > -1) {
            String appName = role.substring(0, scopeIndex);
            role = role.substring(scopeIndex + 1);
            String[] rtn = {appName, role};
            return rtn;
        } else {
            String[] rtn = {null, role};
            return rtn;

        }
    }

    /**
     * Check to see if a flow is currently in use
     *
     * @param realm
     * @param model
     * @return
     */
    public static boolean isFlowUsed(RealmModel realm, AuthenticationFlowModel model) {
        AuthenticationFlowModel realmFlow = null;

        if ((realmFlow = realm.getBrowserFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getRegistrationFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getClientAuthenticationFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getDirectGrantFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getResetCredentialsFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getDockerAuthenticationFlow()) != null && realmFlow.getId().equals(model.getId())) return true;

        for (IdentityProviderModel idp : realm.getIdentityProviders()) {
            if (model.getId().equals(idp.getFirstBrokerLoginFlowId())) return true;
            if (model.getId().equals(idp.getPostBrokerLoginFlowId())) return true;
        }

        return false;

    }

    public static boolean isClientScopeUsed(RealmModel realm, ClientScopeModel clientScope) {
        for (ClientModel client : realm.getClients()) {
            if ((client.getClientScopes(true, false).containsKey(clientScope.getName())) ||
                    (client.getClientScopes(false, false).containsKey(clientScope.getName()))) {
                return true;
            }
        }
        return false;
    }

    public static ClientScopeModel getClientScopeByName(RealmModel realm, String clientScopeName) {
        for (ClientScopeModel clientScope : realm.getClientScopes()) {
            if (clientScopeName.equals(clientScope.getName())) {
                return clientScope;
            }
        }
        // check if we are referencing a client instead of a scope
        if (realm.getClients() != null) {
            for (ClientModel client : realm.getClients()) {
                if (clientScopeName.equals(client.getClientId())) {
                    return client;
                }
            }
        }
        return null;
    }

    /**
     * Lookup clientScope OR client by id. Method is useful if you know just ID, but you don't know
     * if underlying model is clientScope or client
     */
    public static ClientScopeModel findClientScopeById(RealmModel realm, ClientModel client, String clientScopeId) {
        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);

        if (clientScope ==  null) {
            // as fallback we try to resolve dynamic scopes
            clientScope = client.getDynamicClientScope(clientScopeId);
        }

        if (clientScope != null) {
            return clientScope;
        } else {
            return realm.getClientById(clientScopeId);
        }
    }

    /** Replace spaces in the name with underscore, so that scope name can be used as value of scope parameter **/
    public static String convertClientScopeName(String previousName) {
        if (previousName.contains(" ")) {
            return previousName.replaceAll(" ", "_");
        } else {
            return previousName;
        }
    }

    public static void setupAuthorizationServices(RealmModel realm) {
        for (String roleName : Constants.AUTHZ_DEFAULT_AUTHORIZATION_ROLES) {
            if (realm.getRole(roleName) == null) {
                RoleModel role = realm.addRole(roleName);
                role.setDescription("${role_" + roleName + "}");
                realm.addDefaultRole(roleName);
            }
        }
    }

    public static void suspendJtaTransaction(KeycloakSessionFactory factory, Runnable runnable) {
        JtaTransactionManagerLookup lookup = (JtaTransactionManagerLookup)factory.getProviderFactory(JtaTransactionManagerLookup.class);
        Transaction suspended = null;
        try {
            if (lookup != null) {
                if (lookup.getTransactionManager() != null) {
                    try {
                        suspended = lookup.getTransactionManager().suspend();
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            runnable.run();
        } finally {
            if (suspended != null) {
                try {
                    lookup.getTransactionManager().resume(suspended);
                } catch (InvalidTransactionException | SystemException e) {
                    throw new RuntimeException(e);
                }
            }

        }

    }

    public static String getIdentityProviderDisplayName(KeycloakSession session, IdentityProviderModel provider) {
        String displayName = provider.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }

        SocialIdentityProviderFactory providerFactory = (SocialIdentityProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(SocialIdentityProvider.class, provider.getProviderId());
        if (providerFactory != null) {
            return providerFactory.getName();
        } else {
            return provider.getAlias();
        }
    }

}
