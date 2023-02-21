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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.KeycloakSessionTaskWithResult;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.transaction.JtaTransactionManagerLookup;

import javax.crypto.spec.SecretKeySpec;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.AccountRoles;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.keycloak.models.Constants.REALM_ATTR_USERNAME_CASE_SENSITIVE;
import static org.keycloak.models.Constants.REALM_ATTR_USERNAME_CASE_SENSITIVE_DEFAULT;

/**
 * Set of helper methods, which are useful in various model implementations.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public final class KeycloakModelUtils {

    private static final Logger logger = Logger.getLogger(KeycloakModelUtils.class);

    public static final String AUTH_TYPE_CLIENT_SECRET = "client-secret";
    public static final String AUTH_TYPE_CLIENT_SECRET_JWT = "client-secret-jwt";

    public static final String GROUP_PATH_SEPARATOR = "/";
    private static final char CLIENT_ROLE_SEPARATOR = '.';

    private KeycloakModelUtils() {
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
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

    public static String generateSecret(ClientModel client) {
        int secretLength = getSecretLengthByAuthenticationType(client.getClientAuthenticatorType(), client.getAttribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG));
        String secret = SecretGenerator.getInstance().randomString(secretLength);
        client.setSecret(secret);
        client.setAttribute(ClientSecretConstants.CLIENT_SECRET_CREATION_TIME, String.valueOf(Time.currentTime()));
        return secret;
    }

    public static String getDefaultClientAuthenticatorType() {
        return AUTH_TYPE_CLIENT_SECRET;
    }

    public static String generateCodeSecret() {
        return UUID.randomUUID().toString();
    }

    public static ClientModel createManagementClient(RealmModel realm, String name) {
        ClientModel client = createClient(realm, name);

        client.setBearerOnly(true);

        return client;
    }

    public static ClientModel createPublicClient(RealmModel realm, String name) {
        ClientModel client = createClient(realm, name);

        client.setPublicClient(true);

        return client;
    }

    private static ClientModel createClient(RealmModel realm, String name) {
        ClientModel client = realm.addClient(name);

        client.setClientAuthenticatorType(getDefaultClientAuthenticatorType());

        return client;
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

        Set<RoleModel> compositeRoles = composite.getCompositesStream().collect(Collectors.toSet());
        return compositeRoles.contains(role) ||
                compositeRoles.stream().anyMatch(x -> x.isComposite() && searchFor(role, x, visited));
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
            UserModel user = session.users().getUserByEmail(realm, username);
            if (user != null) {
                return user;
            }
        }

        return session.users().getUserByUsername(realm, username);
    }

    /**
     * Wrap given runnable job into KeycloakTransaction.
     */
    public static void runJobInTransaction(KeycloakSessionFactory factory, KeycloakSessionTask task) {
        runJobInTransactionWithResult(factory, session -> {
            task.run(session);
            return null;
        });
    }

    /**
     * Wrap a given callable job into a KeycloakTransaction.
     */
    public static <V> V runJobInTransactionWithResult(KeycloakSessionFactory factory, final KeycloakSessionTaskWithResult<V> callable) {
        V result;
        try (KeycloakSession session = factory.create()) {
            session.getTransactionManager().begin();
            try {
                result = callable.run(session);
            } catch (Throwable t) {
                session.getTransactionManager().setRollbackOnly();
                throw t;
            }
        }
        return result;
    }

    /**
     * Creates a new {@link KeycloakSession} and runs the specified callable in a new transaction. If the transaction fails
     * with a SQL retriable error, the method re-executes the specified callable until it either succeeds or the maximum number
     * of attempts is reached, leaving some increasing random delay milliseconds between the invocations. It uses the exponential
     * backoff + jitter algorithm to compute the delay, which is limited to {@code attemptsCount * retryIntervalMillis}.
     * More details https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
     *
     * @param factory a reference to the {@link KeycloakSessionFactory}.
     * @param callable a reference to the {@link KeycloakSessionTaskWithResult} that will be executed in a retriable way.
     * @param attemptsCount the maximum number of attempts to execute the callable.
     * @param retryIntervalMillis the base interval value in millis used to compute the delay.
     * @param <V> the type returned by the callable.
     * @return the value computed by the callable.
     */
    public static <V> V runJobInRetriableTransaction(final KeycloakSessionFactory factory, final KeycloakSessionTaskWithResult<V> callable,
                                                     final int attemptsCount, final int retryIntervalMillis) {
        int retryCount = 0;
        Random rand = new Random();
        while (true) {
            try (KeycloakSession session = factory.create()) {
                session.getTransactionManager().begin();
                return callable.run(session);
            } catch (RuntimeException re) {
                if (isExceptionRetriable(re) && ++retryCount < attemptsCount) {
                    int delay = Math.min(retryIntervalMillis * attemptsCount, (1 << retryCount) * retryIntervalMillis)
                            + rand.nextInt(retryIntervalMillis);
                    logger.debugf("Caught retriable exception, retrying request. Retry count = %s, retry delay = %s", retryCount, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        ie.addSuppressed(re);
                        throw new RuntimeException(ie);
                    }
                } else {
                    if (retryCount == attemptsCount) {
                        logger.debug("Exhausted all retry attempts for request.");
                        throw new RuntimeException("retries exceeded", re);
                    }
                    throw re;
                }
            }
        }
    }

    /**
     * Checks if the specified exception is retriable or not. A retriable exception must be an instance of {@code SQLException}
     * and must have a 40001 SQL retriable state. This is a standard SQL state as defined in SQL standard, and across the
     * implementations its meaning boils down to "deadlock" (applies to Postgres, MSSQL, Oracle, MySQL, and others).
     *
     * @param exception the exception to be checked.
     * @return {@code true} if the exception is retriable; {@code false} otherwise.
     */
    public static boolean isExceptionRetriable(final Throwable exception) {
        Objects.requireNonNull(exception);
        // first find the root cause and check if it is a SQLException
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        // JTA transaction handler might add multiple suppressed exceptions to the root cause, evaluate each of those
        for (Throwable suppressed : rootCause.getSuppressed()) {
            if (isExceptionRetriable(suppressed)) {
                return true;
            }
        };
        if (rootCause instanceof SQLException) {
            // check if the exception state is a recoverable one (40001)
            return "40001".equals(((SQLException) rootCause).getSQLState());
        }
        return false;
    }

    /**
     * Wrap given runnable job into KeycloakTransaction. Set custom timeout for the JTA transaction (in case we're in the environment with JTA enabled)
     *
     * @param factory
     * @param task
     * @param timeoutInSeconds
     */
    public static void runJobInTransactionWithTimeout(KeycloakSessionFactory factory, KeycloakSessionTask task, int timeoutInSeconds) {
        try {
            setTransactionLimit(factory, timeoutInSeconds);
            runJobInTransaction(factory, task);
        } finally {
            setTransactionLimit(factory, 0);
        }

    }

    public static void setTransactionLimit(KeycloakSessionFactory factory, int timeoutInSeconds) {
        JtaTransactionManagerLookup lookup = (JtaTransactionManagerLookup) factory.getProviderFactory(JtaTransactionManagerLookup.class);
        if (lookup != null) {
            if (lookup.getTransactionManager() != null) {
                try {
                    // If timeout is set to 0, reset to default transaction timeout
                    lookup.getTransactionManager().setTransactionTimeout(timeoutInSeconds);
                } catch (SystemException e) {
                    // Shouldn't happen for Wildfly transaction manager
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static Function<KeycloakSessionFactory, ComponentModel> componentModelGetter(String realmId, String componentId) {
        return factory -> getComponentModel(factory, realmId, componentId);
    }

    public static ComponentModel getComponentModel(KeycloakSessionFactory factory, String realmId, String componentId) {
        AtomicReference<ComponentModel> cm = new AtomicReference<>();
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            cm.set(realm == null ? null : realm.getComponent(componentId));
        });
        return cm.get();
    }

    public static <T extends Provider> ProviderFactory<T> getComponentFactory(KeycloakSessionFactory factory, Class<T> providerClass, Scope config, String spiName) {
        String realmId = config.get("realmId");
        String componentId = config.get("componentId");
        if (realmId == null || componentId == null) {
            realmId = "ROOT";
            ComponentModel cm = new ScopeComponentModel(providerClass, config, spiName, realmId);
            return factory.getProviderFactory(providerClass, realmId, cm.getId(), k -> cm);
        } else {
            return factory.getProviderFactory(providerClass, realmId, componentId, componentModelGetter(realmId, componentId));
        }
    }

    private static class ScopeComponentModel extends ComponentModel {

        private final String componentId;
        private final String providerId;
        private final String providerType;
        private final String realmId;
        private final Scope config;

        public ScopeComponentModel(Class<?> providerClass, Scope baseConfiguration, String spiName, String realmId) {
            final String pr = baseConfiguration.get("provider", Config.getProvider(spiName));

            this.providerId = pr == null ? "default" : pr;
            this.config = baseConfiguration.scope(this.providerId);
            this.componentId = spiName + "- " + (realmId == null ? "" : "f:" + realmId + ":") + this.providerId;
            this.realmId = realmId;
            this.providerType = providerClass.getName();
        }

        @Override
        public String getProviderType() {
            return providerType;
        }

        @Override
        public String getProviderId() {
            return providerId;
        }

        @Override
        public String getName() {
            return componentId + "-config";
        }

        @Override
        public String getId() {
            return componentId;
        }

        @Override
        public String getParentId() {
            return realmId;
        }

        @Override
        public boolean get(String key, boolean defaultValue) {
            return config.getBoolean(key, defaultValue);
        }

        @Override
        public long get(String key, long defaultValue) {
            return config.getLong(key, defaultValue);
        }

        @Override
        public int get(String key, int defaultValue) {
            return config.getInt(key, defaultValue);
        }

        @Override
        public String get(String key, String defaultValue) {
            return config.get(key, defaultValue);
        }

        @Override
        public String get(String key) {
            return get(key, null);
        }
    }

    public static String getMasterRealmAdminApplicationClientId(String realmName) {
        return realmName + "-realm";
    }

    // USER FEDERATION RELATED STUFF


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
        return str == null ? null : str.toLowerCase();
    }

    /**
     * Creates default role for particular realm with the given name.
     *
     * @param realm           Realm
     * @param defaultRoleName Name of the newly created defaultRole
     */
    public static void setupDefaultRole(RealmModel realm, String defaultRoleName) {
        RoleModel defaultRole = realm.addRole(defaultRoleName);
        defaultRole.setDescription("${role_default-roles}");
        realm.setDefaultRole(defaultRole);
    }

    public static RoleModel setupOfflineRole(RealmModel realm) {
        RoleModel offlineRole = realm.getRole(Constants.OFFLINE_ACCESS_ROLE);

        if (offlineRole == null) {
            offlineRole = realm.addRole(Constants.OFFLINE_ACCESS_ROLE);
            offlineRole.setDescription("${role_offline-access}");
            realm.addToDefaultRoles(offlineRole);
        }

        return offlineRole;
    }

    public static void setupDeleteAccount(ClientModel accountClient) {
        RoleModel deleteOwnAccount = accountClient.getRole(AccountRoles.DELETE_ACCOUNT);
        if (deleteOwnAccount == null) {
            deleteOwnAccount = accountClient.addRole(AccountRoles.DELETE_ACCOUNT);
        }
        deleteOwnAccount.setDescription("${role_" + AccountRoles.DELETE_ACCOUNT + "}");
    }

    /**
     * Recursively find all AuthenticationExecutionModel from specified flow or all it's subflows
     *
     * @param realm
     * @param flow
     * @param result input should be empty list. At the end will be all executions added to this list
     */
    public static void deepFindAuthenticationExecutions(RealmModel realm, AuthenticationFlowModel flow, List<AuthenticationExecutionModel> result) {
        realm.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                deepFindAuthenticationExecutions(realm, subFlow, result);
            } else {
                result.add(execution);
            }
        });
    }

    public static String resolveFirstAttribute(GroupModel group, String name) {
        String value = group.getFirstAttribute(name);
        if (value != null) return value;
        if (group.getParentId() == null) return null;
        return resolveFirstAttribute(group.getParent(), name);

    }

    public static Collection<String> resolveAttribute(GroupModel group, String name, boolean aggregateAttrs) {
        Set<String> values = group.getAttributeStream(name).collect(Collectors.toSet());
        if ((values.isEmpty() || aggregateAttrs) && group.getParentId() != null) {
            values.addAll(resolveAttribute(group.getParent(), name, aggregateAttrs));
        }
        return values;
    }

    public static Collection<String> resolveAttribute(UserModel user, String name, boolean aggregateAttrs) {
        List<String> values = user.getAttributeStream(name).collect(Collectors.toList());
        Set<String> aggrValues = new HashSet<String>();
        if (!values.isEmpty()) {
            if (!aggregateAttrs) {
                return values;
            }
            aggrValues.addAll(values);
        }
        Stream<Collection<String>> attributes = user.getGroupsStream()
                .map(group -> resolveAttribute(group, name, aggregateAttrs))
                .filter(Objects::nonNull)
                .filter(attr -> !attr.isEmpty());

        if (!aggregateAttrs) {
            Optional<Collection<String>> first = attributes.findFirst();
            if (first.isPresent()) return first.get();
        } else {
            aggrValues.addAll(attributes.flatMap(Collection::stream).collect(Collectors.toSet()));
        }

        return aggrValues;
    }


    private static GroupModel findSubGroup(String[] segments, int index, GroupModel parent) {
        return parent.getSubGroupsStream().map(group -> {
            String groupName = group.getName();
            String[] pathSegments = formatPathSegments(segments, index, groupName);

            if (groupName.equals(pathSegments[index])) {
                if (pathSegments.length == index + 1) {
                    return group;
                } else {
                    if (index + 1 < pathSegments.length) {
                        GroupModel found = findSubGroup(pathSegments, index + 1, group);
                        if (found != null) return found;
                    }
                }
            }
            return null;
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Given the {@code pathParts} of a group with the given {@code groupName}, format the {@code segments} in order to ignore
     * group names containing a {@code /} character.
     *
     * @param segments  the path segments
     * @param index     the index pointing to the position to start looking for the group name
     * @param groupName the groupName
     * @return a new array of strings with the correct segments in case the group has a name containing slashes
     */
    private static String[] formatPathSegments(String[] segments, int index, String groupName) {
        String[] nameSegments = groupName.split(GROUP_PATH_SEPARATOR);

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
        if (path.startsWith(GROUP_PATH_SEPARATOR)) {
            path = path.substring(1);
        }
        if (path.endsWith(GROUP_PATH_SEPARATOR)) {
            path = path.substring(0, path.length() - 1);
        }
        String[] split = path.split(GROUP_PATH_SEPARATOR);
        if (split.length == 0) return null;

        return realm.getTopLevelGroupsStream().map(group -> {
            String groupName = group.getName();
            String[] pathSegments = formatPathSegments(split, 0, groupName);

            if (groupName.equals(pathSegments[0])) {
                if (pathSegments.length == 1) {
                    return group;
                } else {
                    if (pathSegments.length > 1) {
                        GroupModel subGroup = findSubGroup(pathSegments, 1, group);
                        if (subGroup != null) return subGroup;
                    }
                }

            }
            return null;
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static void buildGroupPath(StringBuilder sb, String groupName, GroupModel parent) {
        if (parent != null) {
            buildGroupPath(sb, parent.getName(), parent.getParent());
        }
        sb.append(GROUP_PATH_SEPARATOR).append(groupName);
    }

    public static String buildGroupPath(GroupModel group) {
        StringBuilder sb = new StringBuilder();
        buildGroupPath(sb, group.getName(), group.getParent());
        return sb.toString();
    }

    public static String buildGroupPath(GroupModel group, GroupModel otherParentGroup) {
        StringBuilder sb = new StringBuilder();
        buildGroupPath(sb, group.getName(), otherParentGroup);
        return sb.toString();
    }

    public static String normalizeGroupPath(final String groupPath) {
        if (groupPath == null) {
            return null;
        }

        String normalized = groupPath;

        if (!normalized.startsWith(GROUP_PATH_SEPARATOR)) {
            normalized = GROUP_PATH_SEPARATOR +  normalized;
        }
        if (normalized.endsWith(GROUP_PATH_SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * @param client    {@link ClientModel}
     * @param container {@link ScopeContainerModel}
     * @return
     * @deprecated Use {@link #getClientScopeMappingsStream(ClientModel, ScopeContainerModel)}  getClientScopeMappingsStream} instead.
     */
    @Deprecated
    public static Set<RoleModel> getClientScopeMappings(ClientModel client, ScopeContainerModel container) {
        return getClientScopeMappingsStream(client, container).collect(Collectors.toSet());
    }

    public static Stream<RoleModel> getClientScopeMappingsStream(ClientModel client, ScopeContainerModel container) {
        return container.getScopeMappingsStream()
                .filter(role -> role.getContainer() instanceof ClientModel &&
                        Objects.equals(client.getId(), role.getContainer().getId()));
    }

    // Used in various role mappers
    public static RoleModel getRoleFromString(RealmModel realm, String roleName) {
        if (roleName == null) {
            return null;
        }

        // Check client roles for all possible splits by dot
        int scopeIndex = roleName.lastIndexOf(CLIENT_ROLE_SEPARATOR);
        while (scopeIndex >= 0) {
            String appName = roleName.substring(0, scopeIndex);
            ClientModel client = realm.getClientByClientId(appName);
            if (client != null) {
                String role = roleName.substring(scopeIndex + 1);
                return client.getRole(role);
            }

            scopeIndex = roleName.lastIndexOf(CLIENT_ROLE_SEPARATOR, scopeIndex - 1);
        }

        // determine if roleName is a realm role
        return realm.getRole(roleName);
    }

    // Used for hardcoded role mappers
    public static String[] parseRole(String role) {
        int scopeIndex = role.lastIndexOf(CLIENT_ROLE_SEPARATOR);
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

    public static String buildRoleQualifier(String clientId, String roleName) {
        if (clientId == null) {
            return roleName;
        }

        return clientId + CLIENT_ROLE_SEPARATOR + roleName;
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

        return realm.getIdentityProvidersStream().anyMatch(idp ->
                Objects.equals(idp.getFirstBrokerLoginFlowId(), model.getId()) ||
                        Objects.equals(idp.getPostBrokerLoginFlowId(), model.getId()));
    }

    public static ClientScopeModel getClientScopeByName(RealmModel realm, String clientScopeName) {
        return realm.getClientScopesStream()
                .filter(clientScope -> Objects.equals(clientScopeName, clientScope.getName()))
                .findFirst()
                // check if we are referencing a client instead of a scope
                .orElseGet(() -> realm.getClientByClientId(clientScopeName));
    }

    /**
     * Lookup clientScope OR client by id. Method is useful if you know just ID, but you don't know
     * if underlying model is clientScope or client
     */
    public static ClientScopeModel findClientScopeById(RealmModel realm, ClientModel client, String clientScopeId) {
        if (client.getId().equals(clientScopeId)) {
            return client;
        }

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);

        if (clientScope == null) {
            // as fallback we try to resolve dynamic scopes
            clientScope = client.getDynamicClientScope(clientScopeId);
        }

        if (clientScope != null) {
            return clientScope;
        } else {
            return realm.getClientById(clientScopeId);
        }
    }

    /**
     * Replace spaces in the name with underscore, so that scope name can be used as value of scope parameter
     **/
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
                realm.addToDefaultRoles(role);
            }
        }
    }

    public static void suspendJtaTransaction(KeycloakSessionFactory factory, Runnable runnable) {
        JtaTransactionManagerLookup lookup = (JtaTransactionManagerLookup) factory.getProviderFactory(JtaTransactionManagerLookup.class);
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

    /**
     * @return true if implementation of realmProvider is "jpa" . Which is always the case in standard Keycloak installations.
     */
    public static boolean isRealmProviderJpa(KeycloakSession session) {
        Set<String> providerIds = session.listProviderIds(RealmProvider.class);
        return providerIds != null && providerIds.size() == 1 && providerIds.iterator().next().equals("jpa");
    }

    /**
     * @param clientAuthenticatorType
     * @return secret size based on authentication type
     */
    public static int getSecretLengthByAuthenticationType(String clientAuthenticatorType, String signingAlg) {
        if (clientAuthenticatorType != null)
            switch (clientAuthenticatorType) {
                case AUTH_TYPE_CLIENT_SECRET_JWT: {
                    if (Algorithm.HS384.equals(signingAlg)) return SecretGenerator.SECRET_LENGTH_384_BITS;
                    if (Algorithm.HS512.equals(signingAlg)) return SecretGenerator.SECRET_LENGTH_512_BITS;
                }
            }
        return SecretGenerator.SECRET_LENGTH_256_BITS;
    }

    /**
     * Returns <code>true</code> if given realm has attribute {@link Constants#REALM_ATTR_USERNAME_CASE_SENSITIVE}
     * set and its value is <code>true</code>. Otherwise default value of it is returned. The default setting 
     * can be seen at {@link Constants#REALM_ATTR_USERNAME_CASE_SENSITIVE_DEFAULT}.
     * 
     * @param realm
     * @return See the description
     * @throws NullPointerException if <code>realm</code> is <code>null</code>
     */
    public static boolean isUsernameCaseSensitive(RealmModel realm) {
        return realm.getAttribute(REALM_ATTR_USERNAME_CASE_SENSITIVE, REALM_ATTR_USERNAME_CASE_SENSITIVE_DEFAULT);
    }
}
