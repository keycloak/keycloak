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

package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderCreationEventImpl;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.entities.AuthenticationExecutionEntity;
import org.keycloak.models.entities.AuthenticationFlowEntity;
import org.keycloak.models.entities.AuthenticatorConfigEntity;
import org.keycloak.models.entities.ComponentEntity;
import org.keycloak.models.entities.IdentityProviderEntity;
import org.keycloak.models.entities.IdentityProviderMapperEntity;
import org.keycloak.models.entities.RequiredActionProviderEntity;
import org.keycloak.models.entities.RequiredCredentialEntity;
import org.keycloak.models.entities.UserFederationMapperEntity;
import org.keycloak.models.entities.UserFederationProviderEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoClientTemplateEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmAdapter extends AbstractMongoAdapter<MongoRealmEntity> implements RealmModel {

    private final MongoRealmEntity realm;
    private final RealmProvider model;

    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;
    protected volatile transient X509Certificate certificate;
    protected volatile transient Key codeSecretKey;

    private volatile transient OTPPolicy otpPolicy;
    private volatile transient PasswordPolicy passwordPolicy;
    private volatile transient KeycloakSession session;

    public RealmAdapter(KeycloakSession session, MongoRealmEntity realmEntity, MongoStoreInvocationContext invocationContext) {
        super(invocationContext);
        this.realm = realmEntity;
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public String getId() {
        return realm.getId();
    }

    @Override
    public String getName() {
        return realm.getName();
    }

    @Override
    public void setName(String name) {
        realm.setName(name);
        updateRealm();
    }

    @Override
    public String getDisplayName() {
        return realm.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        realm.setDisplayName(displayName);
        updateRealm();
    }

    @Override
    public String getDisplayNameHtml() {
        return realm.getDisplayNameHtml();
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        realm.setDisplayNameHtml(displayNameHtml);
        updateRealm();
    }

    @Override
    public boolean isEnabled() {
        return realm.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        realm.setEnabled(enabled);
        updateRealm();
    }

    @Override
    public SslRequired getSslRequired() {
        return realm.getSslRequired() != null ? SslRequired.valueOf(realm.getSslRequired()) : null;
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        realm.setSslRequired(sslRequired.name());
        updateRealm();
    }

    @Override
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
        updateRealm();
    }

    public boolean isRegistrationEmailAsUsername() {
        return realm.isRegistrationEmailAsUsername();
    }

    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        realm.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        updateRealm();
    }

    @Override
    public boolean isRememberMe() {
        return realm.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        realm.setRememberMe(rememberMe);
        updateRealm();
    }

    @Override
    public boolean isBruteForceProtected() {
        return realm.isBruteForceProtected();
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        realm.setBruteForceProtected(value);
        updateRealm();
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return realm.getMaxFailureWaitSeconds();
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        realm.setMaxFailureWaitSeconds(val);
        updateRealm();
    }

    @Override
    public int getWaitIncrementSeconds() {
        return realm.getWaitIncrementSeconds();
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        realm.setWaitIncrementSeconds(val);
        updateRealm();
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return realm.getQuickLoginCheckMilliSeconds();
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        realm.setQuickLoginCheckMilliSeconds(val);
        updateRealm();
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return realm.getMinimumQuickLoginWaitSeconds();
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        realm.setMinimumQuickLoginWaitSeconds(val);
        updateRealm();
    }


    @Override
    public int getMaxDeltaTimeSeconds() {
        return realm.getMaxDeltaTimeSeconds();
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        realm.setMaxDeltaTimeSeconds(val);
        updateRealm();
    }

    @Override
    public int getFailureFactor() {
        return realm.getFailureFactor();
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        realm.setFailureFactor(failureFactor);
        updateRealm();
    }


    @Override
    public boolean isVerifyEmail() {
        return realm.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        realm.setVerifyEmail(verifyEmail);
        updateRealm();
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPassword) {
        realm.setResetPasswordAllowed(resetPassword);
        updateRealm();
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return realm.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        realm.setEditUsernameAllowed(editUsernameAllowed);
        updateRealm();
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (passwordPolicy == null) {
            passwordPolicy = PasswordPolicy.parse(session, realm.getPasswordPolicy());
        }
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        this.passwordPolicy = policy;
        realm.setPasswordPolicy(policy.toString());
        updateRealm();
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        if (otpPolicy == null) {
            otpPolicy = new OTPPolicy();
            otpPolicy.setDigits(realm.getOtpPolicyDigits());
            otpPolicy.setAlgorithm(realm.getOtpPolicyAlgorithm());
            otpPolicy.setInitialCounter(realm.getOtpPolicyInitialCounter());
            otpPolicy.setLookAheadWindow(realm.getOtpPolicyLookAheadWindow());
            otpPolicy.setType(realm.getOtpPolicyType());
            otpPolicy.setPeriod(realm.getOtpPolicyPeriod());
        }
        return otpPolicy;
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        realm.setOtpPolicyAlgorithm(policy.getAlgorithm());
        realm.setOtpPolicyDigits(policy.getDigits());
        realm.setOtpPolicyInitialCounter(policy.getInitialCounter());
        realm.setOtpPolicyLookAheadWindow(policy.getLookAheadWindow());
        realm.setOtpPolicyType(policy.getType());
        realm.setOtpPolicyPeriod(policy.getPeriod());
        updateRealm();
    }


    @Override
    public int getNotBefore() {
        return realm.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        realm.setNotBefore(notBefore);
        updateRealm();
    }

    @Override
    public boolean isRevokeRefreshToken() {
        return realm.isRevokeRefreshToken();
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        realm.setRevokeRefreshToken(revokeRefreshToken);
        updateRealm();
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        return realm.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        realm.setSsoSessionIdleTimeout(seconds);
        updateRealm();
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        return realm.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        realm.setSsoSessionMaxLifespan(seconds);
        updateRealm();
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        return realm.getOfflineSessionIdleTimeout();
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        realm.setOfflineSessionIdleTimeout(seconds);
        updateRealm();
    }

    @Override
    public int getAccessTokenLifespan() {
        return realm.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int tokenLifespan) {
        realm.setAccessTokenLifespan(tokenLifespan);
        updateRealm();
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        return realm.getAccessTokenLifespanForImplicitFlow();
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        realm.setAccessTokenLifespanForImplicitFlow(seconds);
        updateRealm();
    }

    @Override
    public int getAccessCodeLifespan() {
        return realm.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int accessCodeLifespan) {
        realm.setAccessCodeLifespan(accessCodeLifespan);
        updateRealm();
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return realm.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        realm.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
        updateRealm();
    }

    @Override
    public void setAccessCodeLifespanLogin(int accessCodeLifespanLogin) {
        realm.setAccessCodeLifespanLogin(accessCodeLifespanLogin);
        updateRealm();
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        return realm.getAccessCodeLifespanLogin();
    }

    @Override
    public String getKeyId() {
        PublicKey publicKey = getPublicKey();
        return publicKey != null ? JWKBuilder.create().rs256(publicKey).getKeyId() : null;
    }

    @Override
    public String getPublicKeyPem() {
        return realm.getPublicKeyPem();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        realm.setPublicKeyPem(publicKeyPem);
        this.publicKey = null;
        updateRealm();
    }

    @Override
    public X509Certificate getCertificate() {
        if (certificate != null) return certificate;
        certificate = KeycloakModelUtils.getCertificate(getCertificatePem());
        return certificate;
    }

    @Override
    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
        String certificatePem = KeycloakModelUtils.getPemFromCertificate(certificate);
        setCertificatePem(certificatePem);

    }

    @Override
    public String getCertificatePem() {
        return realm.getCertificatePem();
    }

    @Override
    public void setCertificatePem(String certificate) {
        realm.setCertificatePem(certificate);

    }


    @Override
    public String getPrivateKeyPem() {
        return realm.getPrivateKeyPem();
    }

    @Override
    public void setPrivateKeyPem(String privateKeyPem) {
        realm.setPrivateKeyPem(privateKeyPem);
        this.privateKey = null;
        updateRealm();
    }

    @Override
    public PublicKey getPublicKey() {
        if (publicKey != null) return publicKey;
        publicKey = KeycloakModelUtils.getPublicKey(getPublicKeyPem());
        return publicKey;
    }

    @Override
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        String publicKeyPem = KeycloakModelUtils.getPemFromKey(publicKey);
        setPublicKeyPem(publicKeyPem);
    }

    @Override
    public PrivateKey getPrivateKey() {
        if (privateKey != null) return privateKey;
        privateKey = KeycloakModelUtils.getPrivateKey(getPrivateKeyPem());
        return privateKey;
    }

    @Override
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        String privateKeyPem = KeycloakModelUtils.getPemFromKey(privateKey);
        setPrivateKeyPem(privateKeyPem);
    }

    @Override
    public String getCodeSecret() {
        return realm.getCodeSecret();
    }

    @Override
    public Key getCodeSecretKey() {
        if (codeSecretKey == null) {
            codeSecretKey = KeycloakModelUtils.getSecretKey(getCodeSecret());
        }
        return codeSecretKey;
    }

    @Override
    public void setCodeSecret(String codeSecret) {
        realm.setCodeSecret(codeSecret);
        updateRealm();
    }

    @Override
    public String getLoginTheme() {
        return realm.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        realm.setLoginTheme(name);
        updateRealm();
    }

    @Override
    public String getAccountTheme() {
        return realm.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        realm.setAccountTheme(name);
        updateRealm();
    }

    @Override
    public String getAdminTheme() {
        return realm.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        realm.setAdminTheme(name);
        updateRealm();
    }

    @Override
    public String getEmailTheme() {
        return realm.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        realm.setEmailTheme(name);
        updateRealm();
    }

    @Override
    public RoleModel getRole(String name) {
        return session.realms().getRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return session.realms().addRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.realms().addRealmRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return session.realms().removeRole(this, role);
    }

    @Override
    public boolean removeRoleById(String id) {
        RoleModel role = getRoleById(id);
        if (role == null) return false;
        return removeRole(role);
    }

    @Override
    public Set<RoleModel> getRoles() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<MongoRoleEntity> roles = getMongoStore().loadEntities(MongoRoleEntity.class, query, invocationContext);


        if (roles == null) return Collections.EMPTY_SET;
        Set<RoleModel> result = new HashSet<RoleModel>();
        for (MongoRoleEntity role : roles) {
            result.add(new RoleAdapter(session, this, role, this, invocationContext));
        }

        return Collections.unmodifiableSet(result);
    }

    @Override
    public RoleModel getRoleById(String id) {
        return session.realms().getRoleById(id, this);
    }

    @Override
    public GroupModel createGroup(String name) {
        return session.realms().createGroup(this, name);
    }

    @Override
    public GroupModel createGroup(String id, String name) {
        return session.realms().createGroup(this, id, name);
    }

    @Override
    public void addTopLevelGroup(GroupModel subGroup) {
        session.realms().addTopLevelGroup(this, subGroup);

    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        session.realms().moveGroup(this, group, toParent);
    }

    @Override
    public GroupModel getGroupById(String id) {
        return model.getGroupById(id, this);
    }

    @Override
    public List<GroupModel> getGroups() {
        return session.realms().getGroups(this);
    }

    @Override
    public List<GroupModel> getTopLevelGroups() {
        return session.realms().getTopLevelGroups(this);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return session.realms().removeGroup(this, group);
    }



    @Override
    public List<String> getDefaultRoles() {
        return Collections.unmodifiableList(realm.getDefaultRoles());
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            addRole(name);
        }

        getMongoStore().pushItemToList(realm, "defaultRoles", name, true, invocationContext);
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        List<String> roleNames = new ArrayList<String>();
        for (String roleName : defaultRoles) {
            RoleModel role = getRole(roleName);
            if (role == null) {
                addRole(roleName);
            }

            roleNames.add(roleName);
        }

        realm.setDefaultRoles(roleNames);
        updateRealm();
    }

    public static boolean contains(String str, String[] array) {
        for (String s : array) {
            if (str.equals(s)) return true;
        }
        return false;
    }


    @Override
    public void removeDefaultRoles(String... defaultRoles) {
        List<String> roleNames = new ArrayList<String>();
        for (String role : realm.getDefaultRoles()) {
            if (!contains(role, defaultRoles)) roleNames.add(role);
        }
        realm.setDefaultRoles(roleNames);
        updateRealm();
    }

    @Override
    public List<GroupModel> getDefaultGroups() {
        List<String> entities = realm.getDefaultGroups();
        if (entities == null || entities.isEmpty()) return Collections.EMPTY_LIST;
        List<GroupModel> defaultGroups = new LinkedList<>();
        for (String id : entities) {
            defaultGroups.add(session.realms().getGroupById(id, this));
        }
        return Collections.unmodifiableList(defaultGroups);
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        getMongoStore().pushItemToList(realm, "defaultGroups", group.getId(), true, invocationContext);

    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        getMongoStore().pullItemFromList(realm, "defaultGroups", group.getId(), invocationContext);

    }

    @Override
    public ClientModel getClientById(String id) {
        return model.getClientById(id, this);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return session.realms().getClientByClientId(clientId, this);
    }

    @Override
    public List<ClientModel> getClients() {
        return session.realms().getClients(this);
    }

    @Override
    public ClientModel addClient(String name) {
        return session.realms().addClient(this, name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        return session.realms().addClient(this, id, clientId);

    }

    @Override
    public boolean removeClient(String id) {
        if (id == null) return false;
        ClientModel client = getClientById(id);
        if (client == null) return false;
        return session.realms().removeClient(id, this);
    }

    @Override
    public void addRequiredCredential(String type) {
        RequiredCredentialModel credentialModel = initRequiredCredentialModel(type);
        addRequiredCredential(credentialModel, realm.getRequiredCredentials());
    }

    protected void addRequiredCredential(RequiredCredentialModel credentialModel, List<RequiredCredentialEntity> persistentCollection) {
        RequiredCredentialEntity credEntity = new RequiredCredentialEntity();
        credEntity.setType(credentialModel.getType());
        credEntity.setFormLabel(credentialModel.getFormLabel());
        credEntity.setInput(credentialModel.isInput());
        credEntity.setSecret(credentialModel.isSecret());

        persistentCollection.add(credEntity);

        updateRealm();
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        updateRequiredCredentials(creds, realm.getRequiredCredentials());
    }

    protected void updateRequiredCredentials(Set<String> creds, List<RequiredCredentialEntity> credsEntities) {
        Set<String> already = new HashSet<String>();
        Set<RequiredCredentialEntity> toRemove = new HashSet<RequiredCredentialEntity>();
        for (RequiredCredentialEntity entity : credsEntities) {
            if (!creds.contains(entity.getType())) {
                toRemove.add(entity);
            } else {
                already.add(entity.getType());
            }
        }
        for (RequiredCredentialEntity entity : toRemove) {
            credsEntities.remove(entity);
        }
        for (String cred : creds) {
            if (!already.contains(cred)) {
                RequiredCredentialModel credentialModel = initRequiredCredentialModel(cred);
                addRequiredCredential(credentialModel, credsEntities);
            }
        }
        updateRealm();
    }

    @Override
    public List<RequiredCredentialModel> getRequiredCredentials() {
        return convertRequiredCredentialEntities(realm.getRequiredCredentials());
    }

    protected List<RequiredCredentialModel> convertRequiredCredentialEntities(Collection<RequiredCredentialEntity> credEntities) {
        if (credEntities == null || credEntities.isEmpty()) return Collections.EMPTY_LIST;
        List<RequiredCredentialModel> result = new LinkedList<>();
        for (RequiredCredentialEntity entity : credEntities) {
            RequiredCredentialModel model = new RequiredCredentialModel();
            model.setFormLabel(entity.getFormLabel());
            model.setInput(entity.isInput());
            model.setSecret(entity.isSecret());
            model.setType(entity.getType());

            result.add(model);
        }
        return Collections.unmodifiableList(result);
    }

    protected void updateRealm() {
        super.updateMongoEntity();
    }

    protected RequiredCredentialModel initRequiredCredentialModel(String type) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(type);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + type);
        }
        return model;
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        return Collections.unmodifiableMap(realm.getBrowserSecurityHeaders());
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        realm.setBrowserSecurityHeaders(headers);
        updateRealm();
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return Collections.unmodifiableMap(realm.getSmtpConfig());
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        realm.setSmtpConfig(smtpConfig);
        updateRealm();
    }


    @Override
    public List<IdentityProviderModel> getIdentityProviders() {
        List<IdentityProviderEntity> entities = realm.getIdentityProviders();
        if (entities.isEmpty()) return Collections.EMPTY_LIST;
        List<IdentityProviderModel> identityProviders = new ArrayList<IdentityProviderModel>();

        for (IdentityProviderEntity entity: entities) {
            IdentityProviderModel identityProviderModel = new IdentityProviderModel();

            identityProviderModel.setProviderId(entity.getProviderId());
            identityProviderModel.setAlias(entity.getAlias());
            identityProviderModel.setInternalId(entity.getInternalId());
            Map<String, String> config = entity.getConfig();
            Map<String, String> copy = new HashMap<>();
            copy.putAll(config);
            identityProviderModel.setConfig(copy);
            identityProviderModel.setEnabled(entity.isEnabled());
            identityProviderModel.setTrustEmail(entity.isTrustEmail());
            identityProviderModel.setAuthenticateByDefault(entity.isAuthenticateByDefault());
            identityProviderModel.setFirstBrokerLoginFlowId(entity.getFirstBrokerLoginFlowId());
            identityProviderModel.setPostBrokerLoginFlowId(entity.getPostBrokerLoginFlowId());
            identityProviderModel.setStoreToken(entity.isStoreToken());
            identityProviderModel.setAddReadTokenRoleOnCreate(entity.isAddReadTokenRoleOnCreate());

            identityProviders.add(identityProviderModel);
        }

        return Collections.unmodifiableList(identityProviders);
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        for (IdentityProviderModel identityProviderModel : getIdentityProviders()) {
            if (identityProviderModel.getAlias().equals(alias)) {
                return identityProviderModel;
            }
        }

        return null;
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        IdentityProviderEntity entity = new IdentityProviderEntity();

        entity.setInternalId(KeycloakModelUtils.generateId());
        entity.setAlias(identityProvider.getAlias());
        entity.setProviderId(identityProvider.getProviderId());
        entity.setEnabled(identityProvider.isEnabled());
        entity.setTrustEmail(identityProvider.isTrustEmail());
        entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
        entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
        entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
        entity.setConfig(identityProvider.getConfig());

        realm.getIdentityProviders().add(entity);
        updateRealm();
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        for (IdentityProviderEntity entity : realm.getIdentityProviders()) {
            if (entity.getAlias().equals(alias)) {
                realm.getIdentityProviders().remove(entity);
                updateRealm();
                break;
            }
        }
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        for (IdentityProviderEntity entity : this.realm.getIdentityProviders()) {
            if (entity.getInternalId().equals(identityProvider.getInternalId())) {
                entity.setAlias(identityProvider.getAlias());
                entity.setEnabled(identityProvider.isEnabled());
                entity.setTrustEmail(identityProvider.isTrustEmail());
                entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
                entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
                entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
                entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
                entity.setStoreToken(identityProvider.isStoreToken());
                entity.setConfig(identityProvider.getConfig());
            }
        }

        updateRealm();
    }


    private void removeFederationMappersForProvider(String federationProviderId) {
        Set<UserFederationMapperEntity> mappers = getUserFederationMapperEntitiesByFederationProvider(federationProviderId);
        for (UserFederationMapperEntity mapper : mappers) {
            getMongoEntity().getUserFederationMappers().remove(mapper);
        }
    }

    @Override
    public UserFederationProviderModel addUserFederationProvider(String providerName, Map<String, String> config, int priority, String displayName, int fullSyncPeriod, int changedSyncPeriod, int lastSync) {
        KeycloakModelUtils.ensureUniqueDisplayName(displayName, null, getUserFederationProviders());

        UserFederationProviderEntity entity = new UserFederationProviderEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setPriority(priority);
        entity.setProviderName(providerName);
        entity.setConfig(config);
        if (displayName == null) {
            displayName = entity.getId();
        }
        entity.setDisplayName(displayName);
        entity.setFullSyncPeriod(fullSyncPeriod);
        entity.setChangedSyncPeriod(changedSyncPeriod);
        entity.setLastSync(lastSync);
        realm.getUserFederationProviders().add(entity);
        updateRealm();

        UserFederationProviderModel providerModel = new UserFederationProviderModel(entity.getId(), providerName, config, priority, displayName, fullSyncPeriod, changedSyncPeriod, lastSync);

        session.getKeycloakSessionFactory().publish(new UserFederationProviderCreationEventImpl(this, providerModel));

        return providerModel;
    }

    @Override
    public void removeUserFederationProvider(UserFederationProviderModel provider) {
        Iterator<UserFederationProviderEntity> it = realm.getUserFederationProviders().iterator();
        while (it.hasNext()) {
            UserFederationProviderEntity entity = it.next();
            if (entity.getId().equals(provider.getId())) {
                session.users().preRemove(this, new UserFederationProviderModel(entity.getId(), entity.getProviderName(), entity.getConfig(), entity.getPriority(), entity.getDisplayName(),
                        entity.getFullSyncPeriod(), entity.getChangedSyncPeriod(), entity.getLastSync()));
                removeFederationMappersForProvider(provider.getId());

                it.remove();
            }
        }
        updateRealm();
    }
    @Override
    public void updateUserFederationProvider(UserFederationProviderModel model) {
        KeycloakModelUtils.ensureUniqueDisplayName(model.getDisplayName(), model, getUserFederationProviders());

        Iterator<UserFederationProviderEntity> it = realm.getUserFederationProviders().iterator();
        while (it.hasNext()) {
            UserFederationProviderEntity entity = it.next();
            if (entity.getId().equals(model.getId())) {
                entity.setProviderName(model.getProviderName());
                entity.setConfig(model.getConfig());
                entity.setPriority(model.getPriority());
                String displayName = model.getDisplayName();
                if (displayName != null) {
                    entity.setDisplayName(model.getDisplayName());
                }
                entity.setFullSyncPeriod(model.getFullSyncPeriod());
                entity.setChangedSyncPeriod(model.getChangedSyncPeriod());
                entity.setLastSync(model.getLastSync());
            }
        }
        updateRealm();
    }

    @Override
    public List<UserFederationProviderModel> getUserFederationProviders() {
        List<UserFederationProviderEntity> entities = realm.getUserFederationProviders();
        if (entities.isEmpty()) return Collections.EMPTY_LIST;
        List<UserFederationProviderEntity> copy = new LinkedList<UserFederationProviderEntity>();
        for (UserFederationProviderEntity entity : entities) {
            copy.add(entity);

        }
        Collections.sort(copy, new Comparator<UserFederationProviderEntity>() {

            @Override
            public int compare(UserFederationProviderEntity o1, UserFederationProviderEntity o2) {
                return o1.getPriority() - o2.getPriority();
            }

        });
        List<UserFederationProviderModel> result = new LinkedList<UserFederationProviderModel>();
        for (UserFederationProviderEntity entity : copy) {
            result.add(new UserFederationProviderModel(entity.getId(), entity.getProviderName(), entity.getConfig(), entity.getPriority(), entity.getDisplayName(),
                    entity.getFullSyncPeriod(), entity.getChangedSyncPeriod(), entity.getLastSync()));
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public void setUserFederationProviders(List<UserFederationProviderModel> providers) {
        for (UserFederationProviderModel currentProvider : providers) {
            KeycloakModelUtils.ensureUniqueDisplayName(currentProvider.getDisplayName(), currentProvider, providers);
        }

        List<UserFederationProviderEntity> existingProviders = realm.getUserFederationProviders();
        List<UserFederationProviderEntity> toRemove = new LinkedList<>();
        for (UserFederationProviderEntity entity : existingProviders) {
            boolean found = false;
            for (UserFederationProviderModel model : providers) {
                if (entity.getId().equals(model.getId())) {
                    entity.setConfig(model.getConfig());
                    entity.setPriority(model.getPriority());
                    entity.setProviderName(model.getProviderName());
                    String displayName = model.getDisplayName();
                    if (displayName != null) {
                        entity.setDisplayName(displayName);
                    }
                    entity.setFullSyncPeriod(model.getFullSyncPeriod());
                    entity.setChangedSyncPeriod(model.getChangedSyncPeriod());
                    entity.setLastSync(model.getLastSync());
                    found = true;
                    break;
                }

            }
            if (found) continue;
            session.users().preRemove(this, new UserFederationProviderModel(entity.getId(), entity.getProviderName(), entity.getConfig(), entity.getPriority(), entity.getDisplayName(),
                    entity.getFullSyncPeriod(), entity.getChangedSyncPeriod(), entity.getLastSync()));
            removeFederationMappersForProvider(entity.getId());

            toRemove.add(entity);
        }

        for (UserFederationProviderEntity entity : toRemove) {
            realm.getUserFederationProviders().remove(entity);
        }

        List<UserFederationProviderModel> add = new LinkedList<UserFederationProviderModel>();
        for (UserFederationProviderModel model : providers) {
            boolean found = false;
            for (UserFederationProviderEntity entity : realm.getUserFederationProviders()) {
                if (entity.getId().equals(model.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) add.add(model);
        }

        for (UserFederationProviderModel model : add) {
            UserFederationProviderEntity entity = new UserFederationProviderEntity();
            if (model.getId() != null) {
                entity.setId(model.getId());
            } else {
                String id = KeycloakModelUtils.generateId();
                entity.setId(id);
                model.setId(id);
            }
            entity.setProviderName(model.getProviderName());
            entity.setConfig(model.getConfig());
            entity.setPriority(model.getPriority());
            String displayName = model.getDisplayName();
            if (displayName == null) {
                displayName = entity.getId();
            }
            entity.setDisplayName(displayName);
            entity.setFullSyncPeriod(model.getFullSyncPeriod());
            entity.setChangedSyncPeriod(model.getChangedSyncPeriod());
            entity.setLastSync(model.getLastSync());
            realm.getUserFederationProviders().add(entity);

            session.getKeycloakSessionFactory().publish(new UserFederationProviderCreationEventImpl(this, model));
        }

        updateRealm();
    }

    @Override
    public boolean isEventsEnabled() {
        return realm.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        realm.setEventsEnabled(enabled);
        updateRealm();
    }

    @Override
    public long getEventsExpiration() {
        return realm.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        realm.setEventsExpiration(expiration);
        updateRealm();
    }

    @Override
    public Set<String> getEventsListeners() {
        List<String> eventsListeners = realm.getEventsListeners();
        if (eventsListeners.isEmpty()) return Collections.EMPTY_SET;
        Set<String> copy = new HashSet<>();
        copy.addAll(eventsListeners);
        return Collections.unmodifiableSet(copy);
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        if (listeners != null) {
            realm.setEventsListeners(new ArrayList<String>(listeners));
        } else {
            realm.setEventsListeners(Collections.EMPTY_LIST);
        }
        updateRealm();
    }

    @Override
    public Set<String> getEnabledEventTypes() {
        List<String> enabledEventTypes = realm.getEnabledEventTypes();
        if (enabledEventTypes.isEmpty()) return Collections.EMPTY_SET;
        Set<String> copy = new HashSet<>();
        copy.addAll(enabledEventTypes);
        return Collections.unmodifiableSet(copy);
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        if (enabledEventTypes != null) {
            realm.setEnabledEventTypes(new ArrayList<String>(enabledEventTypes));
        } else {
            realm.setEnabledEventTypes(Collections.EMPTY_LIST);
        }
        updateRealm();
    }
    
    @Override
    public boolean isAdminEventsEnabled() {
        return realm.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        realm.setAdminEventsEnabled(enabled);
        updateRealm();
        
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        return realm.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        realm.setAdminEventsDetailsEnabled(enabled);
        updateRealm();
    }
    
    @Override
    public ClientModel getMasterAdminClient() {
        MongoClientEntity appData = getMongoStore().loadEntity(MongoClientEntity.class, realm.getMasterAdminClient(), invocationContext);
        if (appData == null) {
            return null;
        }

        MongoRealmEntity masterRealm = getMongoStore().loadEntity(MongoRealmEntity.class, appData.getRealmId(), invocationContext);
        RealmModel masterRealmModel = new RealmAdapter(session, masterRealm, invocationContext);
        return new ClientAdapter(session, masterRealmModel, appData, invocationContext);
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        String adminAppId = client != null ? client.getId() : null;
        realm.setMasterAdminClient(adminAppId);
        updateRealm();
    }

    @Override
    public MongoRealmEntity getMongoEntity() {
        return realm;
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        return this.realm.getIdentityProviders() != null && !this.realm.getIdentityProviders().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RealmModel)) return false;

        RealmModel that = (RealmModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        return realm.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        realm.setInternationalizationEnabled(enabled);
        updateRealm();
    }

    @Override
    public Set<String> getSupportedLocales() {
        List<String> supportedLocales = realm.getSupportedLocales();
        if (supportedLocales == null || supportedLocales.isEmpty()) return Collections.EMPTY_SET;
        Set<String> copy = new HashSet<>();
        copy.addAll(supportedLocales);
        return Collections.unmodifiableSet(copy);
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        if (locales != null) {
            realm.setSupportedLocales(new ArrayList<String>(locales));
        } else {
            realm.setSupportedLocales(Collections.EMPTY_LIST);
        }
        updateRealm();
    }

    @Override
    public String getDefaultLocale() {
        return realm.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        realm.setDefaultLocale(locale);
        updateRealm();
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers() {
        List<IdentityProviderMapperEntity> entities = getMongoEntity().getIdentityProviderMappers();
        if (entities.isEmpty()) return Collections.EMPTY_SET;
        Set<IdentityProviderMapperModel> mappings = new HashSet<IdentityProviderMapperModel>();
        for (IdentityProviderMapperEntity entity : entities) {
            IdentityProviderMapperModel mapping = entityToModel(entity);
            mappings.add(mapping);
        }
        return Collections.unmodifiableSet(mappings);
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
        Set<IdentityProviderMapperModel> mappings = new HashSet<IdentityProviderMapperModel>();
        for (IdentityProviderMapperEntity entity : getMongoEntity().getIdentityProviderMappers()) {
            if (!entity.getIdentityProviderAlias().equals(brokerAlias)) {
                continue;
            }
            IdentityProviderMapperModel mapping = entityToModel(entity);
            mappings.add(mapping);
        }
        return mappings;
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        if (getIdentityProviderMapperByName(model.getIdentityProviderAlias(), model.getIdentityProviderMapper()) != null) {
            throw new RuntimeException("identity provider mapper name must be unique per identity provider");
        }
        String id = KeycloakModelUtils.generateId();
        IdentityProviderMapperEntity entity = new IdentityProviderMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setConfig(model.getConfig());

        getMongoEntity().getIdentityProviderMappers().add(entity);
        updateMongoEntity();
        return entityToModel(entity);
    }

    protected IdentityProviderMapperEntity getIdentityProviderMapperEntity(String id) {
        for (IdentityProviderMapperEntity entity : getMongoEntity().getIdentityProviderMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected IdentityProviderMapperEntity getIdentityProviderMapperEntityByName(String alias, String name) {
        for (IdentityProviderMapperEntity entity : getMongoEntity().getIdentityProviderMappers()) {
            if (entity.getIdentityProviderAlias().equals(alias) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        IdentityProviderMapperEntity toDelete = getIdentityProviderMapperEntity(mapping.getId());
        if (toDelete != null) {
            this.realm.getIdentityProviderMappers().remove(toDelete);
            updateMongoEntity();
        }
    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntity(mapping.getId());
        entity.setIdentityProviderAlias(mapping.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(mapping.getIdentityProviderMapper());
        if (entity.getConfig() == null) {
            entity.setConfig(mapping.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapping.getConfig());
        }
        updateMongoEntity();

    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String alias, String name) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntityByName(alias, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected IdentityProviderMapperModel entityToModel(IdentityProviderMapperEntity entity) {
        IdentityProviderMapperModel mapping = new IdentityProviderMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setIdentityProviderAlias(entity.getIdentityProviderAlias());
        mapping.setIdentityProviderMapper(entity.getIdentityProviderMapper());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        String flowId = realm.getBrowserFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        realm.setBrowserFlow(flow.getId());
        updateRealm();

    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        String flowId = realm.getRegistrationFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        realm.setRegistrationFlow(flow.getId());
        updateRealm();

    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        String flowId = realm.getDirectGrantFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        realm.setDirectGrantFlow(flow.getId());
        updateRealm();

    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        String flowId = realm.getResetCredentialsFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        realm.setResetCredentialsFlow(flow.getId());
        updateRealm();
    }

    public AuthenticationFlowModel getClientAuthenticationFlow() {
        String flowId = realm.getClientAuthenticationFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        realm.setClientAuthenticationFlow(flow.getId());
        updateRealm();
    }

    @Override
    public List<AuthenticationFlowModel> getAuthenticationFlows() {
        List<AuthenticationFlowEntity> flows = getMongoEntity().getAuthenticationFlows();
        if (flows.isEmpty()) return Collections.EMPTY_LIST;
        List<AuthenticationFlowModel> models = new LinkedList<>();
        for (AuthenticationFlowEntity entity : flows) {
            AuthenticationFlowModel model = entityToModel(entity);
            models.add(model);
        }
        return Collections.unmodifiableList(models);
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        for (AuthenticationFlowModel flow : getAuthenticationFlows()) {
            if (flow.getAlias().equals(alias)) {
                return flow;
            }
        }
        return null;
    }


    protected AuthenticationFlowModel entityToModel(AuthenticationFlowEntity entity) {
        AuthenticationFlowModel model = new AuthenticationFlowModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        model.setDescription(entity.getDescription());
        model.setBuiltIn(entity.isBuiltIn());
        model.setTopLevel(entity.isTopLevel());
        model.setProviderId(entity.getProviderId());
        return model;
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        AuthenticationFlowEntity entity = getFlowEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected AuthenticationFlowEntity getFlowEntity(String id) {
        List<AuthenticationFlowEntity> flows = getMongoEntity().getAuthenticationFlows();
        for (AuthenticationFlowEntity entity : flows) {
            if (id.equals(entity.getId())) return entity;
        }
        return null;

    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        if (KeycloakModelUtils.isFlowUsed(this, model)) {
            throw new ModelException("Cannot remove authentication flow, it is currently in use");
        }
        AuthenticationFlowEntity toDelete = getFlowEntity(model.getId());
        if (toDelete == null) return;
        getMongoEntity().getAuthenticationFlows().remove(toDelete);
        updateMongoEntity();
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity toUpdate = getFlowEntity(model.getId());;
        if (toUpdate == null) return;
        toUpdate.setAlias(model.getAlias());
        toUpdate.setDescription(model.getDescription());
        toUpdate.setProviderId(model.getProviderId());
        toUpdate.setBuiltIn(model.isBuiltIn());
        toUpdate.setTopLevel(model.isTopLevel());
        updateMongoEntity();
    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity entity = new AuthenticationFlowEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId(): model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setTopLevel(model.isTopLevel());
        getMongoEntity().getAuthenticationFlows().add(entity);
        model.setId(entity.getId());
        updateMongoEntity();
        return model;
    }

    @Override
    public List<AuthenticationExecutionModel> getAuthenticationExecutions(String flowId) {
        AuthenticationFlowEntity flow = getFlowEntity(flowId);
        if (flow == null) return Collections.EMPTY_LIST;

        List<AuthenticationExecutionEntity> queryResult = flow.getExecutions();
        List<AuthenticationExecutionModel> executions = new LinkedList<>();
        for (AuthenticationExecutionEntity entity : queryResult) {
            AuthenticationExecutionModel model = entityToModel(entity);
            executions.add(model);
        }
        Collections.sort(executions, AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
        return Collections.unmodifiableList(executions);
    }

    public AuthenticationExecutionModel entityToModel(AuthenticationExecutionEntity entity) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(entity.getId());
        model.setRequirement(entity.getRequirement());
        model.setPriority(entity.getPriority());
        model.setAuthenticator(entity.getAuthenticator());
        model.setFlowId(entity.getFlowId());
        model.setParentFlow(entity.getParentFlow());
        model.setAuthenticatorFlow(entity.isAuthenticatorFlow());
        model.setAuthenticatorConfig(entity.getAuthenticatorConfig());
        return model;
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        AuthenticationExecutionEntity execution = getAuthenticationExecutionEntity(id);
        return entityToModel(execution);
    }

    public AuthenticationExecutionEntity getAuthenticationExecutionEntity(String id) {
        List<AuthenticationFlowEntity> flows = getMongoEntity().getAuthenticationFlows();
        for (AuthenticationFlowEntity entity : flows) {
            for (AuthenticationExecutionEntity exe : entity.getExecutions()) {
                if (exe.getId().equals(id)) {
                   return exe;
                }
            }
        }
        return null;
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = new AuthenticationExecutionEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId(): model.getId();
        entity.setId(id);
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setRequirement(model.getRequirement());
        entity.setAuthenticatorFlow(model.isAuthenticatorFlow());
        entity.setFlowId(model.getFlowId());
        entity.setParentFlow(model.getParentFlow());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        AuthenticationFlowEntity flow = getFlowEntity(model.getParentFlow());
        flow.getExecutions().add(entity);
        updateMongoEntity();
        model.setId(entity.getId());
        return model;

    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = null;
        AuthenticationFlowEntity flow = getFlowEntity(model.getParentFlow());
        for (AuthenticationExecutionEntity exe : flow.getExecutions()) {
            if (exe.getId().equals(model.getId())) {
                entity = exe;
            }
        }
        if (entity == null) return;
        entity.setAuthenticatorFlow(model.isAuthenticatorFlow());
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setRequirement(model.getRequirement());
        entity.setFlowId(model.getFlowId());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        updateMongoEntity();
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = null;
        AuthenticationFlowEntity flow = getFlowEntity(model.getParentFlow());
        for (AuthenticationExecutionEntity exe : flow.getExecutions()) {
            if (exe.getId().equals(model.getId())) {
                entity = exe;
            }
        }
        if (entity == null) return;
        flow.getExecutions().remove(entity);
        updateMongoEntity();

    }

    @Override
    public List<AuthenticatorConfigModel> getAuthenticatorConfigs() {
        List<AuthenticatorConfigEntity> entities = getMongoEntity().getAuthenticatorConfigs();
        if (entities.isEmpty()) return Collections.EMPTY_LIST;
        List<AuthenticatorConfigModel> authenticators = new LinkedList<>();
        for (AuthenticatorConfigEntity entity : entities) {
            authenticators.add(entityToModel(entity));
        }
        return Collections.unmodifiableList(authenticators);
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        for (AuthenticatorConfigModel config : getAuthenticatorConfigs()) {
            if (config.getAlias().equals(alias)) {
                return config;
            }
        }
        return null;
    }


    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity auth = new AuthenticatorConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId(): model.getId();
        auth.setId(id);
        auth.setAlias(model.getAlias());
        auth.setConfig(model.getConfig());
        realm.getAuthenticatorConfigs().add(auth);
        model.setId(auth.getId());
        updateMongoEntity();
        return model;
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(model.getId());
        if (entity == null) return;
        getMongoEntity().getAuthenticatorConfigs().remove(entity);
        updateMongoEntity();

    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    public AuthenticatorConfigEntity getAuthenticatorConfigEntity(String id) {
        AuthenticatorConfigEntity entity = null;
        for (AuthenticatorConfigEntity auth : getMongoEntity().getAuthenticatorConfigs()) {
            if (auth.getId().equals(id)) {
                entity = auth;
                break;
            }
        }
        return entity;
    }

    public AuthenticatorConfigModel entityToModel(AuthenticatorConfigEntity entity) {
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(model.getId());
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(model.getConfig());
        }
        updateMongoEntity();
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity auth = new RequiredActionProviderEntity();
        auth.setId(KeycloakModelUtils.generateId());
        auth.setAlias(model.getAlias());
        auth.setName(model.getName());
        auth.setProviderId(model.getProviderId());
        auth.setConfig(model.getConfig());
        auth.setEnabled(model.isEnabled());
        auth.setDefaultAction(model.isDefaultAction());
        realm.getRequiredActionProviders().add(auth);
        model.setId(auth.getId());
        updateMongoEntity();
        return model;
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = getRequiredActionProviderEntity(model.getId());
        if (entity == null) return;
        getMongoEntity().getRequiredActionProviders().remove(entity);
        updateMongoEntity();
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        RequiredActionProviderEntity entity = getRequiredActionProviderEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    public RequiredActionProviderModel entityToModel(RequiredActionProviderEntity entity) {
        RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setId(entity.getId());
        model.setProviderId(entity.getProviderId());
        model.setAlias(entity.getAlias());
        model.setName(entity.getName());
        model.setEnabled(entity.isEnabled());
        model.setDefaultAction(entity.isDefaultAction());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = getRequiredActionProviderEntity(model.getId());
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        entity.setName(model.getName());
        entity.setProviderId(model.getProviderId());
        entity.setEnabled(model.isEnabled());
        entity.setDefaultAction(model.isDefaultAction());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(model.getConfig());
        }
        updateMongoEntity();
    }

    @Override
    public List<RequiredActionProviderModel> getRequiredActionProviders() {
        List<RequiredActionProviderEntity> entities = realm.getRequiredActionProviders();
        if (entities.isEmpty()) return Collections.EMPTY_LIST;
        List<RequiredActionProviderModel> actions = new LinkedList<>();
        for (RequiredActionProviderEntity entity : entities) {
            actions.add(entityToModel(entity));
        }
        return Collections.unmodifiableList(actions);
    }

    public RequiredActionProviderEntity getRequiredActionProviderEntity(String id) {
        RequiredActionProviderEntity entity = null;
        for (RequiredActionProviderEntity auth : getMongoEntity().getRequiredActionProviders()) {
            if (auth.getId().equals(id)) {
                entity = auth;
                break;
            }
        }
        return entity;
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        for (RequiredActionProviderModel action : getRequiredActionProviders()) {
            if (action.getAlias().equals(alias)) return action;
        }
        return null;
    }





    @Override
    public Set<UserFederationMapperModel> getUserFederationMappers() {
        List<UserFederationMapperEntity> entities = getMongoEntity().getUserFederationMappers();
        if (entities.isEmpty()) return Collections.EMPTY_SET;
        Set<UserFederationMapperModel> mappers = new HashSet<UserFederationMapperModel>();
        for (UserFederationMapperEntity entity : entities) {
            UserFederationMapperModel mapper = entityToModel(entity);
            mappers.add(mapper);
        }
        return Collections.unmodifiableSet(mappers);
    }

    @Override
    public Set<UserFederationMapperModel> getUserFederationMappersByFederationProvider(String federationProviderId) {
        Set<UserFederationMapperModel> mappers = new HashSet<UserFederationMapperModel>();
        Set<UserFederationMapperEntity> mapperEntities = getUserFederationMapperEntitiesByFederationProvider(federationProviderId);
        for (UserFederationMapperEntity entity : mapperEntities) {
            mappers.add(entityToModel(entity));
        }
        return mappers;
    }

    @Override
    public UserFederationMapperModel addUserFederationMapper(UserFederationMapperModel model) {
        if (getUserFederationMapperByName(model.getFederationProviderId(), model.getName()) != null) {
            throw new ModelDuplicateException("User federation mapper must be unique per federation provider. There is already: " + model.getName());
        }
        String id = KeycloakModelUtils.generateId();
        UserFederationMapperEntity entity = new UserFederationMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setFederationProviderId(model.getFederationProviderId());
        entity.setFederationMapperType(model.getFederationMapperType());
        entity.setConfig(model.getConfig());

        getMongoEntity().getUserFederationMappers().add(entity);
        updateMongoEntity();
        UserFederationMapperModel mapperModel = entityToModel(entity);

        return mapperModel;
    }

    protected UserFederationMapperEntity getUserFederationMapperEntity(String id) {
        for (UserFederationMapperEntity entity : getMongoEntity().getUserFederationMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected UserFederationMapperEntity getUserFederationMapperEntityByName(String federationProviderId, String name) {
        for (UserFederationMapperEntity entity : getMongoEntity().getUserFederationMappers()) {
            if (entity.getFederationProviderId().equals(federationProviderId) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    protected Set<UserFederationMapperEntity> getUserFederationMapperEntitiesByFederationProvider(String federationProviderId) {
        Set<UserFederationMapperEntity> mappers = new HashSet<UserFederationMapperEntity>();
        for (UserFederationMapperEntity entity : getMongoEntity().getUserFederationMappers()) {
            if (federationProviderId.equals(entity.getFederationProviderId())) {
                mappers.add(entity);
            }
        }
        return mappers;
    }

    @Override
    public void removeUserFederationMapper(UserFederationMapperModel mapper) {
        UserFederationMapperEntity toDelete = getUserFederationMapperEntity(mapper.getId());
        if (toDelete != null) {
            this.realm.getUserFederationMappers().remove(toDelete);
            updateMongoEntity();
        }
    }

    @Override
    public void updateUserFederationMapper(UserFederationMapperModel mapper) {
        UserFederationMapperEntity entity = getUserFederationMapperEntity(mapper.getId());
        entity.setFederationProviderId(mapper.getFederationProviderId());
        entity.setFederationMapperType(mapper.getFederationMapperType());
        if (entity.getConfig() == null) {
            entity.setConfig(mapper.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapper.getConfig());
        }
        updateMongoEntity();
    }

    @Override
    public UserFederationMapperModel getUserFederationMapperById(String id) {
        UserFederationMapperEntity entity = getUserFederationMapperEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public UserFederationMapperModel getUserFederationMapperByName(String federationProviderId, String name) {
        UserFederationMapperEntity entity = getUserFederationMapperEntityByName(federationProviderId, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected UserFederationMapperModel entityToModel(UserFederationMapperEntity entity) {
        UserFederationMapperModel mapper = new UserFederationMapperModel();
        mapper.setId(entity.getId());
        mapper.setName(entity.getName());
        mapper.setFederationProviderId(entity.getFederationProviderId());
        mapper.setFederationMapperType(entity.getFederationMapperType());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapper.setConfig(config);
        return mapper;
    }

    @Override
    public List<ClientTemplateModel> getClientTemplates() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<MongoClientTemplateEntity> clientEntities = getMongoStore().loadEntities(MongoClientTemplateEntity.class, query, invocationContext);
        if (clientEntities.isEmpty()) return Collections.EMPTY_LIST;
        List<ClientTemplateModel> result = new LinkedList<>();
        for (MongoClientTemplateEntity clientEntity : clientEntities) {
            result.add(new ClientTemplateAdapter(session, this, clientEntity, invocationContext));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public ClientTemplateModel addClientTemplate(String name) {
        return this.addClientTemplate(null, name);
    }

    @Override
    public ClientTemplateModel addClientTemplate(String id, String name) {
        MongoClientTemplateEntity clientEntity = new MongoClientTemplateEntity();
        clientEntity.setId(id);
        clientEntity.setName(name);
        clientEntity.setRealmId(getId());
        getMongoStore().insertEntity(clientEntity, invocationContext);

        final ClientTemplateModel model = new ClientTemplateAdapter(session, this, clientEntity, invocationContext);
        return model;
    }

    @Override
    public boolean removeClientTemplate(String id) {
        if (id == null) return false;
        ClientTemplateModel client = getClientTemplateById(id);
        if (client == null) return false;

        if (KeycloakModelUtils.isClientTemplateUsed(this, client)) {
            throw new ModelException("Cannot remove client template, it is currently in use");
        }

        return getMongoStore().removeEntity(MongoClientTemplateEntity.class, id, invocationContext);
    }

    @Override
    public ClientTemplateModel getClientTemplateById(String id) {
        return model.getClientTemplateById(id, this);
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        ComponentEntity entity = new ComponentEntity();
        if (model.getId() == null) {
            entity.setId(KeycloakModelUtils.generateId());
        } else {
            entity.setId(model.getId());
        }
        entity.setConfig(model.getConfig());
        entity.setParentId(model.getParentId());
        entity.setProviderType(model.getProviderType());
        entity.setProviderId(model.getProviderId());
        entity.setName(model.getName());
        model.setId(entity.getId());
        realm.getComponentEntities().add(entity);
        updateRealm();

        return model;
    }

    @Override
    public void updateComponent(ComponentModel model) {
        for (ComponentEntity entity : realm.getComponentEntities()) {
            if (entity.getId().equals(model.getId())) {
                entity.setConfig(model.getConfig());
                entity.setParentId(model.getParentId());
                entity.setProviderType(model.getProviderType());
                entity.setProviderId(model.getProviderId());
                entity.setName(model.getName());

            }
        }
        updateRealm();

    }

    @Override
    public void removeComponent(ComponentModel component) {
        Iterator<ComponentEntity> it = realm.getComponentEntities().iterator();
        while(it.hasNext()) {
            if (it.next().getId().equals(component.getId())) {
                session.users().preRemove(this, component);
                it.remove();
                break;
            }
        }
        updateRealm();

    }

    @Override
    public void removeComponents(String parentId) {
        Iterator<ComponentEntity> it = realm.getComponentEntities().iterator();
        while(it.hasNext()) {
            ComponentEntity next = it.next();
            if (next.getParentId().equals(parentId)) {
                session.users().preRemove(this, entityToModel(next));
                it.remove();
            }
        }
        updateRealm();

    }

    @Override
    public List<ComponentModel> getComponents(String parentId, String providerType) {
        List<ComponentModel> results = new LinkedList<>();
        for (ComponentEntity entity : realm.getComponentEntities()) {
            if (entity.getParentId().equals(parentId) && entity.getProviderType().equals(providerType)) {
                ComponentModel model = entityToModel(entity);
                results.add(model);
            }

        }
        return results;
    }

    @Override
    public List<ComponentModel> getComponents(String parentId) {
        List<ComponentModel> results = new LinkedList<>();
        for (ComponentEntity entity : realm.getComponentEntities()) {
            if (entity.getParentId().equals(parentId)) {
                ComponentModel model = entityToModel(entity);
                results.add(model);
            }

        }
        return results;
    }

    protected ComponentModel entityToModel(ComponentEntity entity) {
        ComponentModel model = new ComponentModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setParentId(entity.getParentId());
        model.setProviderId(entity.getProviderId());
        model.setProviderType(entity.getProviderType());
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.putAll(entity.getConfig());
        model.setConfig(map);
        return model;
    }

    @Override
    public List<ComponentModel> getComponents() {
        List<ComponentModel> results = new LinkedList<>();
        for (ComponentEntity entity : realm.getComponentEntities()) {
            ComponentModel model = entityToModel(entity);
            results.add(model);
        }
        return results;
    }

    @Override
    public ComponentModel getComponent(String id) {
        for (ComponentEntity entity : realm.getComponentEntities()) {
            if (entity.getId() == entity.getId()) {
                return entityToModel(entity);
            }
        }
        return null;
    }
}
