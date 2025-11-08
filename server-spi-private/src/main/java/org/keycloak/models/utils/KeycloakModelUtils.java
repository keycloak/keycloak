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

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.spec.SecretKeySpec;

import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

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
import org.keycloak.constants.Oid4VciConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.deployment.DeployedConfigurationsManager;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.GroupProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.KeycloakSessionTaskWithResult;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.transaction.JtaTransactionManagerLookup;
import org.keycloak.transaction.RequestContextHelper;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

import static org.keycloak.utils.StreamsUtil.closing;

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
    public static final String GROUP_PATH_ESCAPE = "~";
    private static final char CLIENT_ROLE_SEPARATOR = '.';

    public static final int MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE = 25;

    public static final int DEFAULT_RSA_KEY_SIZE = 4096;
    public static final int DEFAULT_CERTIFICATE_VALIDITY_YEARS = 3;

    private KeycloakModelUtils() {
    }

    /**
     * Return an ID generated using the UUID java class.
     * @return The ID using UUID.toString (36 chars)
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Return an ID generated using the UUID class but using base64 URL encoding
     * with the two longs (msb+lsb).
     * @return The ID getting msb and lsb from UUID and encoding them in
     * base64 URL without padding (22 chars)
     */
    public static String generateShortId() {
        return generateShortId(UUID.randomUUID());
    }

    /**
     * Generates a short ID representation for the UUID. The representation is the
     * base64 url encoding of the msb+lsb of the UUID.
     * @param uuid The UUID to represent
     * @return The string representation in 22 characters
     */
    public static String generateShortId(final UUID uuid) {
        final byte[] bytes = new byte[2 * Long.BYTES];
        // first the msb
        long l = uuid.getMostSignificantBits();
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            bytes[i] = (byte) (l & 0xff);
            l >>= 8;
        }
        // second the lsb
        l = uuid.getLeastSignificantBits();
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            bytes[Long.BYTES + i] = (byte) (l & 0xff);
            l >>= 8;
        }
        // encode in base64 URL no padding (22 chars)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Check if a string is a valid UUID.
     * @param uuid The UUID string to verify
     * @return true if the string is a valid uuid
     */
    public static boolean isValidUUID(String uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, DEFAULT_CERTIFICATE_VALIDITY_YEARS);
        return generateKeyPairCertificate(subject, DEFAULT_RSA_KEY_SIZE, calendar);
    }

    public static CertificateRepresentation generateKeyPairCertificate(String subject, int keysize, Calendar endDate) {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(keysize);
        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject, BigInteger.valueOf(System.currentTimeMillis()), endDate.getTime());

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
     * If "Login with email" is enabled and the given username contains '@',
     * attempts to find the user by email for authentication.
     *
     * Otherwise, or if not found, attempts to find the user by username.
     *
     * @param realm the realm to search within
     * @param username the username or email of the user
     * @return the found user if present; otherwise, {@code null}
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
     * @param factory The session factory to use
     * @param task The task to execute
     */
    public static void runJobInTransaction(KeycloakSessionFactory factory, KeycloakSessionTask task) {
        runJobInTransaction(factory, null, task);
    }

    /**
     * Wrap given runnable job into KeycloakTransaction.
     * @param factory The session factory to use
     * @param context The context from the previous session
     * @param task The task to execute
     */
    public static void runJobInTransaction(KeycloakSessionFactory factory, KeycloakContext context, KeycloakSessionTask task) {
        runJobInTransactionWithResult(factory, context, session -> {
            task.run(session);
            return null;
        }, task.getTaskName());
    }

    /**
     * Sets up the context for the specified session with the RealmModel.
     *
     * @param origContext The original context to propagate
     * @param targetSession The new target session to propagate the context to
     */
    public static void cloneContextRealmClientToSession(final KeycloakContext origContext, final KeycloakSession targetSession) {
        cloneContextToSession(origContext, targetSession, false);
    }

    /**
     * Sets up the context for the specified session with the RealmModel, clientModel and
     * AuthenticatedSessionModel.
     *
     * @param origContext The original context to propagate
     * @param targetSession The new target session to propagate the context to
     */
    public static void cloneContextRealmClientSessionToSession(final KeycloakContext origContext, final KeycloakSession targetSession) {
        cloneContextToSession(origContext, targetSession, true);
    }

    /**
     * Sets up the context for the specified session.The original realm's context is used to
     * determine what models need to be re-loaded using the current session. The models
     * in the context are re-read from the new session via the IDs.
     */
    private static void cloneContextToSession(final KeycloakContext origContext, final KeycloakSession targetSession,
            final boolean includeAuthenticatedSessionModel) {
        if (origContext == null) {
            return;
        }

        // setup realm model if necessary.
        RealmModel realmModel = null;
        if (origContext.getRealm() != null) {
            realmModel = targetSession.realms().getRealm(origContext.getRealm().getId());
            if (realmModel != null) {
                targetSession.getContext().setRealm(realmModel);
            }
        }

        // setup client model if necessary.
        ClientModel clientModel = null;
        if (origContext.getClient() != null) {
            if (origContext.getRealm() == null || !Objects.equals(origContext.getRealm().getId(), origContext.getClient().getRealm().getId())) {
                realmModel = targetSession.realms().getRealm(origContext.getClient().getRealm().getId());
            }
            if (realmModel != null) {
                clientModel = targetSession.clients().getClientById(realmModel, origContext.getClient().getId());
                if (clientModel != null) {
                    targetSession.getContext().setClient(clientModel);
                }
            }
        }

        // setup auth session model if necessary.
        if (includeAuthenticatedSessionModel && origContext.getAuthenticationSession() != null) {
            if (origContext.getClient() == null || !Objects.equals(origContext.getClient().getId(), origContext.getAuthenticationSession().getClient().getId())) {
                realmModel = (origContext.getRealm() == null || !Objects.equals(origContext.getRealm().getId(), origContext.getAuthenticationSession().getRealm().getId()))
                        ? targetSession.realms().getRealm(origContext.getAuthenticationSession().getRealm().getId())
                        : targetSession.getContext().getRealm();
                clientModel = (realmModel != null)
                        ? targetSession.clients().getClientById(realmModel, origContext.getAuthenticationSession().getClient().getId())
                        : null;
            }
            if (clientModel != null) {
                RootAuthenticationSessionModel rootAuthSession = targetSession.authenticationSessions().getRootAuthenticationSession(
                        realmModel, origContext.getAuthenticationSession().getParentSession().getId());
                if (rootAuthSession != null) {
                    AuthenticationSessionModel authSessionModel = rootAuthSession.getAuthenticationSession(clientModel,
                            origContext.getAuthenticationSession().getTabId());
                    if (authSessionModel != null) {
                        targetSession.getContext().setAuthenticationSession(authSessionModel);
                    }
                }
            }
        }
    }

    /**
     * Wrap a given callable job into a KeycloakTransaction.
     * @param <V> The type for the result
     * @param factory The session factory
     * @param callable The callable to execute
     * @return The return value from the callable
     */
    public static <V> V runJobInTransactionWithResult(KeycloakSessionFactory factory, final KeycloakSessionTaskWithResult<V> callable) {
        return runJobInTransactionWithResult(factory, null, callable, "Non-HTTP task");
    }

    /**
     * Wrap a given callable job into a KeycloakTransaction.
     * @param <V> The type for the result
     * @param factory The session factory
     * @param context The context from the previous session to use
     * @param callable The callable to execute
     * @param taskName Name of the task. Can be useful for logging purposes
     * @return The return value from the callable
     */
    public static <V> V runJobInTransactionWithResult(KeycloakSessionFactory factory, KeycloakContext context, final KeycloakSessionTaskWithResult<V> callable,
                                                      String taskName) {
        V result;
        KeycloakSession existing = KeycloakSessionUtil.getKeycloakSession();
        try (KeycloakSession session = factory.create()) {
            RequestContextHelper.getContext(session).setContextMessage(taskName);
            session.getTransactionManager().begin();
            KeycloakSessionUtil.setKeycloakSession(session);
            try {
                cloneContextRealmClientToSession(context, session);
                result = callable.run(session);
            } catch (Throwable t) {
                session.getTransactionManager().setRollbackOnly();
                throw t;
            } finally {
                KeycloakSessionUtil.setKeycloakSession(existing);
            }
        }
        return result;
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

    public static String getMasterRealmAdminManagementClientId(String realmName) {
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

    /**
     * Helper to get from the session if group path slashes should be escaped or not.
     * @param session The session
     * @return true or false
     */
    public static boolean escapeSlashesInGroupPath(KeycloakSession session) {
        GroupProviderFactory fact = (GroupProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(GroupProvider.class);
        return fact.escapeSlashesInGroupPath();
    }

    /**
     * Finds group by path. Path is separated by '/' character. For example: /group/subgroup/subsubgroup
     * <p />
     * The method takes into consideration also groups with '/' in their name. For example: /group/sub/group/subgroup
     * This method allows escaping of slashes for example: /parent\/group/child which
     * is a two level path for ["parent/group", "child"].
     *
     * @param session Keycloak session
     * @param realm The realm
     * @param path Path that will be searched among groups
     *
     * @return {@code GroupModel} corresponding to the given {@code path} or {@code null} if no group was found
     */
    public static GroupModel findGroupByPath(KeycloakSession session, RealmModel realm, String path) {
        if (path == null) {
            return null;
        }
        String[] split = splitPath(path, escapeSlashesInGroupPath(session));
        if (split.length == 0) return null;
        return getGroupModel(session.groups(), realm, null, split, 0);
    }

    /**
     * Finds group by path. Variant when you have the path already separated by
     * group names.
     *
     * @param session Keycloak session
     * @param realm The realm
     * @param path Path The path hierarchy of groups
     *
     * @return {@code GroupModel} corresponding to the given {@code path} or {@code null} if no group was found
     */
    public static GroupModel findGroupByPath(KeycloakSession session, RealmModel realm, String[] path) {
        if (path == null || path.length == 0) {
            return null;
        }
        return getGroupModel(session.groups(), realm, null, path, 0);
    }

    private static GroupModel getGroupModel(GroupProvider groupProvider, RealmModel realm, GroupModel parent, String[] split, int index) {
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = index; i < split.length; i++) {
            nameBuilder.append(split[i]);
            GroupModel group = groupProvider.getGroupByName(realm, parent, nameBuilder.toString());
            if (group != null) {
                if (i < split.length-1) {
                    return getGroupModel(groupProvider, realm, group, split, i+1);
                } else {
                    return group;
                }
            }
            nameBuilder.append(GROUP_PATH_SEPARATOR);
        }
        return null;
    }

    /**
     * Splits a group path than can be escaped for slashes.
     * @param path The group path
     * @param escapedSlashes true if slashes are escaped in the path
     * @return
     */
    public static String[] splitPath(String path, boolean escapedSlashes) {
        if (path == null) {
            return null;
        }
        if (path.startsWith(GROUP_PATH_SEPARATOR)) {
            path = path.substring(1);
        }
        if (path.endsWith(GROUP_PATH_SEPARATOR)) {
            path = path.substring(0, path.length() - 1);
        }
        // just split by slashed that are not escaped
        return escapedSlashes
                ? Arrays.stream(path.split("(?<!" + Pattern.quote(GROUP_PATH_ESCAPE) + ")" + Pattern.quote(GROUP_PATH_SEPARATOR)))
                        .map(KeycloakModelUtils::unescapeGroupNameForPath)
                        .toArray(String[]::new)
                : path.split(GROUP_PATH_SEPARATOR);
    }

    /**
     * Escapes the slash in the name if found. "group/slash" returns "group\/slash".
     * @param groupName
     * @return
     */
    private static String escapeGroupNameForPath(String groupName) {
        return groupName.replace(GROUP_PATH_SEPARATOR, GROUP_PATH_ESCAPE + GROUP_PATH_SEPARATOR);
    }

    /**
     * Unescape the escaped slashes in name. "group\/slash" returns "group/slash".
     * @param groupName
     * @return
     */
    private static String unescapeGroupNameForPath(String groupName) {
        return groupName.replace(GROUP_PATH_ESCAPE + GROUP_PATH_SEPARATOR, GROUP_PATH_SEPARATOR);
    }

    public static String buildGroupPath(boolean escapeSlashes, String... names) {
        StringBuilder sb = new StringBuilder();
        sb.append(GROUP_PATH_SEPARATOR);
        for (int i = 0; i < names.length; i++) {
            sb.append(escapeSlashes? escapeGroupNameForPath(names[i]) : names[i]);
            if (i < names.length - 1) {
                sb.append(GROUP_PATH_SEPARATOR);
            }
        }
        return sb.toString();
    }

    private static void buildGroupPath(StringBuilder sb, String groupName, GroupModel parent, boolean escapeSlashes) {
        if (parent != null) {
            buildGroupPath(sb, parent.getName(), parent.getParent(), escapeSlashes);
        }
        sb.append(GROUP_PATH_SEPARATOR).append(escapeSlashes? escapeGroupNameForPath(groupName) : groupName);
    }

    public static String buildGroupPath(GroupModel group) {
        StringBuilder sb = new StringBuilder();
        buildGroupPath(sb, group.getName(), group.getParent(), group.escapeSlashesInGroupPath());
        return sb.toString();
    }

    public static String buildGroupPath(GroupModel group, GroupModel otherParentGroup) {
        StringBuilder sb = new StringBuilder();
        buildGroupPath(sb, group.getName(), otherParentGroup, group.escapeSlashesInGroupPath());
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
        int counter = 0;
        int scopeIndex = roleName.lastIndexOf(CLIENT_ROLE_SEPARATOR);
        while (scopeIndex >= 0 && counter < MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE) {
            counter++;
            String appName = roleName.substring(0, scopeIndex);
            ClientModel client = realm.getClientByClientId(appName);
            if (client != null) {
                String role = roleName.substring(scopeIndex + 1);
                return client.getRole(role);
            }

            scopeIndex = roleName.lastIndexOf(CLIENT_ROLE_SEPARATOR, scopeIndex - 1);
        }
        if (counter >= MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE) {
            logger.warnf("Not able to retrieve role model from the role name '%s'. Please use shorter role names with the limited amount of dots, roleName", roleName.length() > 100 ? roleName.substring(0, 100) + "..." : roleName);
            return null;
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
    public static boolean isFlowUsed(KeycloakSession session, RealmModel realm, AuthenticationFlowModel model) {
        AuthenticationFlowModel realmFlow = null;

        if ((realmFlow = realm.getBrowserFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getRegistrationFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getClientAuthenticationFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getDirectGrantFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getResetCredentialsFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getDockerAuthenticationFlow()) != null && realmFlow.getId().equals(model.getId())) return true;
        if ((realmFlow = realm.getFirstBrokerLoginFlow()) != null && realmFlow.getId().equals(model.getId())) return true;

        Stream<ClientModel> browserFlowOverridingClients = realm.searchClientByAuthenticationFlowBindingOverrides(Collections.singletonMap("browser", model.getId()), 0, 1);
        Stream<ClientModel> directGrantFlowOverridingClients = realm.searchClientByAuthenticationFlowBindingOverrides(Collections.singletonMap("direct_grant", model.getId()), 0, 1);
        boolean usedByClient = closing(Stream.concat(browserFlowOverridingClients, directGrantFlowOverridingClients))
                .limit(1)
                .findAny()
                .isPresent();

        if (usedByClient) {
            return true;
        }

        return session.identityProviders().getByFlow(model.getId(), null,0, 1).findAny().isPresent();
    }

    /**
     * Recursively remove authentication flow (including all subflows and executions) from the model storage
     *
     * @param session The keycloak session
     * @param realm The realm
     * @param authFlow flow to delete
     * @param flowUnavailableHandler Will be executed when flow, sub-flow or executor is null
     * @param builtinFlowHandler will be executed when flow is built-in flow
     */
    public static void deepDeleteAuthenticationFlow(KeycloakSession session, RealmModel realm, AuthenticationFlowModel authFlow, Runnable flowUnavailableHandler, Runnable builtinFlowHandler) {
        if (authFlow == null) {
            flowUnavailableHandler.run();
            return;
        }
        if (authFlow.isBuiltIn()) {
            builtinFlowHandler.run();
        }

        realm.getAuthenticationExecutionsStream(authFlow.getId())
                .forEachOrdered(authExecutor -> deepDeleteAuthenticationExecutor(session, realm, authExecutor, flowUnavailableHandler, builtinFlowHandler));

        realm.removeAuthenticationFlow(authFlow);
    }

    /**
     * Recursively remove authentication executor (including sub-flows and configs) from the model storage
     *
     * @param session The keycloak session
     * @param realm The realm
     * @param authExecutor The authentication executor to remove
     * @param flowUnavailableHandler Handler that will be executed when flow, sub-flow or executor is null
     * @param builtinFlowHandler Handler that will be executed when flow is built-in flow
     */
    public static void deepDeleteAuthenticationExecutor(KeycloakSession session, RealmModel realm, AuthenticationExecutionModel authExecutor, Runnable flowUnavailableHandler, Runnable builtinFlowHandler) {
        if (authExecutor == null) {
            flowUnavailableHandler.run();
            return;
        }

        // recursively remove sub flows
        if (authExecutor.getFlowId() != null) {
            AuthenticationFlowModel authFlow = realm.getAuthenticationFlowById(authExecutor.getFlowId());
            deepDeleteAuthenticationFlow(session, realm, authFlow, flowUnavailableHandler, builtinFlowHandler);
        }

        // remove the config if not shared
        if (authExecutor.getAuthenticatorConfig() != null) {
            DeployedConfigurationsManager configManager = new DeployedConfigurationsManager(session);
            if (configManager.getDeployedAuthenticatorConfig(authExecutor.getAuthenticatorConfig()) == null) {
                AuthenticatorConfigModel config = configManager.getAuthenticatorConfig(realm, authExecutor.getAuthenticatorConfig());
                if (config != null) {
                    realm.removeAuthenticatorConfig(config);
                }
            }
        }

        // remove the executor at the end
        realm.removeAuthenticatorExecution(authExecutor);
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
     * @param clientAuthenticatorType
     * @return secret size based on authentication type
     */
    public static int getSecretLengthByAuthenticationType(String clientAuthenticatorType, String signingAlg) {
        if (clientAuthenticatorType != null)
            switch (clientAuthenticatorType) {
                case AUTH_TYPE_CLIENT_SECRET_JWT: {
                    if (Algorithm.HS384.equals(signingAlg))
                        return SecretGenerator.equivalentEntropySize(SecretGenerator.SECRET_LENGTH_384_BITS, SecretGenerator.ALPHANUM.length);
                    else if (Algorithm.HS512.equals(signingAlg))
                        return SecretGenerator.equivalentEntropySize(SecretGenerator.SECRET_LENGTH_512_BITS, SecretGenerator.ALPHANUM.length);
                    else
                        return SecretGenerator.equivalentEntropySize(SecretGenerator.SECRET_LENGTH_256_BITS, SecretGenerator.ALPHANUM.length);
                }
            }
        return SecretGenerator.SECRET_LENGTH_256_BITS;
    }

    /**
     * Sets the default groups on the realm
     * @param session
     * @param realm
     * @param groups
     * @throws RuntimeException if a group does not exist
     */
    public static void setDefaultGroups(KeycloakSession session, RealmModel realm, Stream<String> groups) {
        realm.getDefaultGroupsStream().collect(Collectors.toList()).forEach(realm::removeDefaultGroup);
        groups.forEach(path -> {
            GroupModel found = KeycloakModelUtils.findGroupByPath(session, realm, path);
            if (found == null) throw new RuntimeException("default group in realm rep doesn't exist: " + path);
            realm.addDefaultGroup(found);
        });
    }

    /**
     * <p>Runs the given {@code operation} within the scope of the given @{target} realm.
     *
     * <p>Only use this method when you need to execute operations in a {@link RealmModel} object that is different
     * than the one associated with the {@code session}.
     *
     * @param session the session
     * @param target the target realm
     * @param operation the operation
     * @return the result from the supplier
     */
    public static <T> T runOnRealm(KeycloakSession session, RealmModel target, Function<KeycloakSession, T> operation) {
        KeycloakContext context = session.getContext();
        RealmModel currentRealm = context.getRealm();

        if (currentRealm.equals(target)) {
            return operation.apply(session);
        }

        try {
            context.setRealm(target);
            return operation.apply(session);
        } finally {
            context.setRealm(currentRealm);
        }
    }

    /**
     * @return the list of protocols accepted for the given client.
     */
    public static List<String> getAcceptedClientScopeProtocols(ClientModel client) {
        List<String> acceptedClientProtocols;
        if (client.getProtocol() == null || "openid-connect".equals(client.getProtocol())) {
            acceptedClientProtocols = List.of("openid-connect", Oid4VciConstants.OID4VC_PROTOCOL);
        }else {
            acceptedClientProtocols = List.of(client.getProtocol());
        }
        return acceptedClientProtocols;
    }
}
