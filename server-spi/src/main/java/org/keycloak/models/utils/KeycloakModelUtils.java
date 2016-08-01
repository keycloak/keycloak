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

import org.bouncycastle.openssl.PEMWriter;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.PemUtils;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    public static String generateSecret() {
        return generateSecret(32);
    }

    public static String generateSecret(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64Url.encode(buf);
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
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        try {
            pemWriter.writeObject(key);
            pemWriter.flush();
            pemWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s = writer.toString();
        return PemUtils.removeBeginEnd(s);
    }

    public static String getPemFromCertificate(X509Certificate certificate) {
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        try {
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            pemWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s = writer.toString();
        return PemUtils.removeBeginEnd(s);
    }

    public static void generateRealmKeys(RealmModel realm) {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        realm.setPrivateKey(keyPair.getPrivate());
        realm.setPublicKey(keyPair.getPublic());
        X509Certificate certificate = null;
        try {
            certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, realm.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        realm.setCertificate(certificate);

        realm.setCodeSecret(generateCodeSecret());
    }

    public static void generateRealmCertificate(RealmModel realm) {
        X509Certificate certificate = null;
        try {
            certificate = CertificateUtils.generateV1SelfSignedCertificate(new KeyPair(realm.getPublicKey(), realm.getPrivateKey()), realm.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        realm.setCertificate(certificate);
    }

    public static CertificateRepresentation generateKeyPairCertificate(String subject) {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        X509Certificate certificate = null;
        try {
            certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String privateKeyPem = KeycloakModelUtils.getPemFromKey(keyPair.getPrivate());
        String certPem = KeycloakModelUtils.getPemFromCertificate(certificate);

        CertificateRepresentation rep = new CertificateRepresentation();
        rep.setPrivateKey(privateKeyPem);
        rep.setCertificate(certPem);
        return rep;
    }

    public static UserCredentialModel generateSecret(ClientModel client) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        client.setSecret(secret.getValue());
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
    public static boolean searchFor(RoleModel role, RoleModel composite, Set<RoleModel> visited) {
        if (visited.contains(composite)) return false;
        visited.add(composite);
        Set<RoleModel> composites = composite.getComposites();
        if (composites.contains(role)) return true;
        for (RoleModel contained : composites) {
            if (!contained.isComposite()) continue;
            if (searchFor(role, contained, visited)) return true;
        }
        return false;
    }

    /**
     * Try to find user by username or email
     *
     * @param realm    realm
     * @param username username or email of user
     * @return found user
     */
    public static UserModel findUserByNameOrEmail(KeycloakSession session, RealmModel realm, String username) {
        if (username.indexOf('@') != -1) {
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

    public static String getMasterRealmAdminApplicationClientId(String realmName) {
        return realmName + "-realm";
    }

    /**
     * @param roles
     * @param targetRole
     * @return true if targetRole is in roles (directly or indirectly via composite role)
     */
    public static boolean hasRole(Set<RoleModel> roles, RoleModel targetRole) {
        if (roles.contains(targetRole)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(targetRole)) return true;
        }
        return false;
    }

    /**
     *
     * @param groups
     * @param targetGroup
     * @return true if targetGroup is in groups (directly or indirectly via parent child relationship)
     */
    public static boolean isMember(Set<GroupModel> groups, GroupModel targetGroup) {
        if (groups.contains(targetGroup)) return true;

        for (GroupModel mapping : groups) {
            GroupModel child = mapping;
            while(child.getParent() != null) {
                if (child.getParent().equals(targetGroup)) return true;
                child = child.getParent();
            }
        }
        return false;
    }
    // USER FEDERATION RELATED STUFF

    /**
     * Ensure that displayName of myProvider (if not null) is unique and there is no other provider with same displayName in the list.
     *
     * @param displayName         to check for duplications
     * @param myProvider          provider, which is excluded from the list (if present)
     * @param federationProviders
     * @throws ModelDuplicateException if there is other provider with same displayName
     */
    public static void ensureUniqueDisplayName(String displayName, UserFederationProviderModel myProvider, List<UserFederationProviderModel> federationProviders) throws ModelDuplicateException {
        if (displayName != null) {

            for (UserFederationProviderModel federationProvider : federationProviders) {
                if (myProvider != null && (myProvider.equals(federationProvider) || (myProvider.getId() != null && myProvider.getId().equals(federationProvider.getId())))) {
                    continue;
                }

                if (displayName.equals(federationProvider.getDisplayName())) {
                    throw new ModelDuplicateException("There is already existing federation provider with display name: " + displayName);
                }
            }
        }
    }


    public static UserFederationProviderModel findUserFederationProviderByDisplayName(String displayName, RealmModel realm) {
        if (displayName == null) {
            return null;
        }

        for (UserFederationProviderModel fedProvider : realm.getUserFederationProviders()) {
            if (displayName.equals(fedProvider.getDisplayName())) {
                return fedProvider;
            }
        }
        return null;
    }

    public static UserFederationProviderModel findUserFederationProviderById(String fedProviderId, RealmModel realm) {
        for (UserFederationProviderModel fedProvider : realm.getUserFederationProviders()) {
            if (fedProviderId.equals(fedProvider.getId())) {
                return fedProvider;
            }
        }
        return null;
    }


    public static UserFederationMapperModel createUserFederationMapperModel(String name, String federationProviderId, String mapperType, String... config) {
        UserFederationMapperModel mapperModel = new UserFederationMapperModel();
        mapperModel.setName(name);
        mapperModel.setFederationProviderId(federationProviderId);
        mapperModel.setFederationMapperType(mapperType);

        Map<String, String> configMap = new HashMap<>();
        String key = null;
        for (String configEntry : config) {
            if (key == null) {
                key = configEntry;
            } else {
                configMap.put(key, configEntry);
                key = null;
            }
        }
        if (key != null) {
            throw new IllegalStateException("Invalid count of arguments for config. Maybe mistake?");
        }
        mapperModel.setConfig(configMap);

        return mapperModel;
    }

    public static UserFederationProviderFactory getFederationProviderFactory(KeycloakSession session, UserFederationProviderModel model) {
        return (UserFederationProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, model.getProviderName());
    }

    public static UserFederationProvider getFederationProviderInstance(KeycloakSession session, UserFederationProviderModel model) {
        UserFederationProviderFactory factory = getFederationProviderFactory(session, model);
        return factory.getInstance(session, model);

    }

    // END USER FEDERATION RELATED STUFF

    public static String toLowerCaseSafe(String str) {
        return str==null ? null : str.toLowerCase();
    }

    public static void setupOfflineTokens(RealmModel realm) {
        if (realm.getRole(Constants.OFFLINE_ACCESS_ROLE) == null) {
            RoleModel role = realm.addRole(Constants.OFFLINE_ACCESS_ROLE);
            role.setDescription("${role_offline-access}");
            role.setScopeParamRequired(true);
            realm.addDefaultRole(Constants.OFFLINE_ACCESS_ROLE);
        }
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


    public static List<String> resolveAttribute(UserModel user, String name) {
        List<String> values = user.getAttribute(name);
        if (!values.isEmpty()) return values;
        for (GroupModel group : user.getGroups()) {
            values = resolveAttribute(group, name);
            if (values != null) return values;
        }
        return Collections.emptyList();
    }


    private static GroupModel findSubGroup(String[] path, int index, GroupModel parent) {
        for (GroupModel group : parent.getSubGroups()) {
            if (group.getName().equals(path[index])) {
                if (path.length == index + 1) {
                    return group;
                }
                else {
                    if (index + 1 < path.length) {
                        GroupModel found = findSubGroup(path, index + 1, group);
                        if (found != null) return found;
                    } else {
                        return null;
                    }
                }

            }
        }
        return null;
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
            if (group.getName().equals(split[0])) {
                if (split.length == 1) {
                    found = group;
                    break;
                }
                else {
                    if (split.length > 1) {
                        found = findSubGroup(split, 1, group);
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
        String[] parsedRole = parseRole(roleName);
        RoleModel role = null;
        if (parsedRole[0] == null) {
            role = realm.getRole(parsedRole[1]);
        } else {
            ClientModel client = realm.getClientByClientId(parsedRole[0]);
            if (client != null) {
                role = client.getRole(parsedRole[1]);
            }
        }
        return role;
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

        for (IdentityProviderModel idp : realm.getIdentityProviders()) {
            if (model.getId().equals(idp.getFirstBrokerLoginFlowId())) return true;
            if (model.getId().equals(idp.getPostBrokerLoginFlowId())) return true;
        }

        return false;

    }

    public static boolean isClientTemplateUsed(RealmModel realm, ClientTemplateModel template) {
        for (ClientModel client : realm.getClients()) {
            if (client.getClientTemplate() != null && client.getClientTemplate().getId().equals(template.getId())) return true;
        }
        return false;
    }

    public static ClientTemplateModel getClientTemplateByName(RealmModel realm, String templateName) {
        for (ClientTemplateModel clientTemplate : realm.getClientTemplates()) {
            if (templateName.equals(clientTemplate.getName())) {
                return clientTemplate;
            }
        }

        return null;
    }

    public static void setupAuthorizationServices(RealmModel realm) {
        for (String roleName : Constants.AUTHZ_DEFAULT_AUTHORIZATION_ROLES) {
            if (realm.getRole(roleName) == null) {
                RoleModel role = realm.addRole(roleName);
                role.setDescription("${role_" + roleName + "}");
                role.setScopeParamRequired(false);
                realm.addDefaultRole(roleName);
            }
        }
    }
}
