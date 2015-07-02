/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.models.file.adapter;

import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.enums.SslRequired;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperEventImpl;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderCreationEventImpl;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.entities.AuthenticationExecutionEntity;
import org.keycloak.models.entities.AuthenticationFlowEntity;
import org.keycloak.models.entities.AuthenticatorConfigEntity;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.entities.IdentityProviderMapperEntity;
import org.keycloak.models.entities.RealmEntity;
import org.keycloak.models.entities.RequiredActionProviderEntity;
import org.keycloak.models.entities.RequiredCredentialEntity;
import org.keycloak.models.entities.RoleEntity;
import org.keycloak.models.entities.UserFederationMapperEntity;
import org.keycloak.models.entities.UserFederationProviderEntity;
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
 * RealmModel for JSON persistence.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class RealmAdapter implements RealmModel {

    private final InMemoryModel inMemoryModel;
    private final RealmEntity realm;

    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;
    protected volatile transient X509Certificate certificate;
    protected volatile transient Key codeSecretKey;

    private volatile transient PasswordPolicy passwordPolicy;
    private volatile transient KeycloakSession session;

    private final Map<String, ClientModel> allApps = new HashMap<String, ClientModel>();
    private ClientModel masterAdminApp = null;
    private final Map<String, RoleAdapter> allRoles = new HashMap<String, RoleAdapter>();
    private final Map<String, IdentityProviderModel> allIdProviders = new HashMap<String, IdentityProviderModel>();

    public RealmAdapter(KeycloakSession session, RealmEntity realm, InMemoryModel inMemoryModel) {
        this.session = session;
        this.realm = realm;
        this.inMemoryModel = inMemoryModel;
    }

    public RealmEntity getRealmEnity() {
        return realm;
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
        if (getName() == null) {
            realm.setName(name);
            return;
        }

        if (getName().equals(name)) return; // allow setting name to same value

        if (inMemoryModel.getRealmByName(name) != null) throw new ModelDuplicateException("Realm " + name + " already exists.");
        realm.setName(name);
    }

    @Override
    public boolean isEnabled() {
        return realm.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        realm.setEnabled(enabled);
    }

    @Override
    public SslRequired getSslRequired() {
        return SslRequired.valueOf(realm.getSslRequired());
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        realm.setSslRequired(sslRequired.name());
    }

    @Override
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        return realm.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        realm.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public boolean isRememberMe() {
        return realm.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        realm.setRememberMe(rememberMe);
    }

    @Override
    public boolean isBruteForceProtected() {
        return realm.isBruteForceProtected();
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        realm.setBruteForceProtected(value);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return realm.getMaxFailureWaitSeconds();
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        realm.setMaxFailureWaitSeconds(val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return realm.getWaitIncrementSeconds();
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        realm.setWaitIncrementSeconds(val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return realm.getQuickLoginCheckMilliSeconds();
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        realm.setQuickLoginCheckMilliSeconds(val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return realm.getMinimumQuickLoginWaitSeconds();
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        realm.setMinimumQuickLoginWaitSeconds(val);
    }


    @Override
    public int getMaxDeltaTimeSeconds() {
        return realm.getMaxDeltaTimeSeconds();
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        realm.setMaxDeltaTimeSeconds(val);
    }

    @Override
    public int getFailureFactor() {
        return realm.getFailureFactor();
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        realm.setFailureFactor(failureFactor);
    }


    @Override
    public boolean isVerifyEmail() {
        return realm.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        realm.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPassword) {
        realm.setResetPasswordAllowed(resetPassword);
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return realm.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        realm.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (passwordPolicy == null) {
            passwordPolicy = new PasswordPolicy(realm.getPasswordPolicy());
        }
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        this.passwordPolicy = policy;
        realm.setPasswordPolicy(policy.toString());
    }

    @Override
    public int getNotBefore() {
        return realm.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        realm.setNotBefore(notBefore);
    }


    @Override
    public int getSsoSessionIdleTimeout() {
        return realm.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        realm.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        return realm.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        realm.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        return realm.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int tokenLifespan) {
        realm.setAccessTokenLifespan(tokenLifespan);
    }

    @Override
    public int getAccessCodeLifespan() {
        return realm.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int accessCodeLifespan) {
        realm.setAccessCodeLifespan(accessCodeLifespan);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return realm.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        realm.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
    }

    @Override
    public String getPublicKeyPem() {
        return realm.getPublicKeyPem();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        realm.setPublicKeyPem(publicKeyPem);
        this.publicKey = null;
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
    }

    @Override
    public String getLoginTheme() {
        return realm.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        realm.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        return realm.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        realm.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        return realm.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        realm.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        return realm.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        realm.setEmailTheme(name);
    }

    @Override
    public RoleAdapter getRole(String name) {
        for (RoleAdapter role : allRoles.values()) {
            if (role.getName().equals(name)) return role;
        }
        return null;
    }

    @Override
    public RoleModel addRole(String name) {
        return this.addRole(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        if (id == null) throw new NullPointerException("id == null");
        if (name == null) throw new NullPointerException("name == null");
        if (hasRoleWithName(name)) throw new ModelDuplicateException("Realm already contains role with name " + name + ".");

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setRealmId(getId());

        RoleAdapter roleModel = new RoleAdapter(this, roleEntity, this);
        allRoles.put(id, roleModel);
        return roleModel;
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return removeRoleById(role.getId());
    }

    @Override
    public boolean removeRoleById(String id) {
        if (id == null) throw new NullPointerException("id == null");

        // try realm roles first
        if (allRoles.remove(id) != null) return true;

        for (ClientModel app : getClients()) {
            for (RoleModel appRole : app.getRoles()) {
                if (id.equals(appRole.getId())) {
                    app.removeRole(appRole);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Set<RoleModel> getRoles() {
        return new HashSet(allRoles.values());
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleModel found = allRoles.get(id);
        if (found != null) return found;

        for (ClientModel app : getClients()) {
            for (RoleModel appRole : app.getRoles()) {
                if (appRole.getId().equals(id)) return appRole;
            }
        }

        return null;
    }

    @Override
    public List<String> getDefaultRoles() {
        return realm.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            addRole(name);
        }

        List<String> roleNames = getDefaultRoles();
        if (roleNames.contains(name)) throw new IllegalArgumentException("Realm " + realm.getName() + " already contains default role named " + name);

        roleNames.add(name);
        realm.setDefaultRoles(roleNames);
    }

    boolean hasRoleWithName(String name) {
        for (RoleModel role : allRoles.values()) {
            if (role.getName().equals(name)) return true;
        }

        return false;
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
    }

    @Override
    public ClientModel getClientById(String id) {
        return allApps.get(id);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        for (ClientModel app : getClients()) {
            if (app.getClientId().equals(clientId)) return app;
        }

        return null;
    }

    @Override
    public Map<String, ClientModel> getClientNameMap() {
        Map<String, ClientModel> resourceMap = new HashMap<String, ClientModel>();
        for (ClientModel resource : getClients()) {
            resourceMap.put(resource.getClientId(), resource);
        }
        return resourceMap;
    }

    @Override
    public List<ClientModel> getClients() {
        return new ArrayList<ClientModel>(allApps.values());
    }

    @Override
    public ClientModel addClient(String name) {
        return this.addClient(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        if (clientId == null) throw new NullPointerException("name == null");
        if (id == null) throw new NullPointerException("id == null");

        if (getClientNameMap().containsKey(clientId)) {
            throw new ModelDuplicateException("Application named '" + clientId + "' already exists.");
        }

        ClientEntity appEntity = new ClientEntity();
        appEntity.setId(id);
        appEntity.setClientId(clientId);
        appEntity.setRealmId(getId());
        appEntity.setEnabled(true);

        final ClientModel app = new ClientAdapter(session, this, appEntity, inMemoryModel);
        session.getKeycloakSessionFactory().publish(new ClientCreationEvent() {
            @Override
            public ClientModel getCreatedClient() {
                return app;
            }
        });

        allApps.put(id, app);

        return app;
    }

    @Override
    public boolean removeClient(String id) {
        ClientModel appToBeRemoved = this.getClientById(id);
        if (appToBeRemoved == null) return false;

        // remove any composite role assignments for this app
        for (RoleModel role : this.getRoles()) {
            RoleAdapter roleAdapter = (RoleAdapter)role;
            roleAdapter.removeApplicationComposites(id);
        }

        for (RoleModel role : appToBeRemoved.getRoles()) {
            appToBeRemoved.removeRole(role);
        }

        return (allApps.remove(id) != null);
    }

    boolean hasUserWithEmail(String email) {
        for (UserModel user : inMemoryModel.getUsers(getId())) {
            if (user.getEmail() == null) continue;
            if (user.getEmail().equals(email)) return true;
        }

        return false;
    }

    @Override
    public void addRequiredCredential(String type) {
        if (type == null) throw new NullPointerException("Credential type can not be null");

        RequiredCredentialModel credentialModel = initRequiredCredentialModel(type);

        List<RequiredCredentialEntity> requiredCredList = realm.getRequiredCredentials();
        for (RequiredCredentialEntity cred : requiredCredList) {
            if (type.equals(cred.getType())) return;
        }

        addRequiredCredential(credentialModel, requiredCredList);
    }

    protected void addRequiredCredential(RequiredCredentialModel credentialModel, List<RequiredCredentialEntity> persistentCollection) {
        RequiredCredentialEntity credEntity = new RequiredCredentialEntity();
        credEntity.setType(credentialModel.getType());
        credEntity.setFormLabel(credentialModel.getFormLabel());
        credEntity.setInput(credentialModel.isInput());
        credEntity.setSecret(credentialModel.isSecret());

        persistentCollection.add(credEntity);
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
    }

    @Override
    public List<RequiredCredentialModel> getRequiredCredentials() {
        return convertRequiredCredentialEntities(realm.getRequiredCredentials());
    }

    protected List<RequiredCredentialModel> convertRequiredCredentialEntities(Collection<RequiredCredentialEntity> credEntities) {

        List<RequiredCredentialModel> result = new ArrayList<RequiredCredentialModel>();
        for (RequiredCredentialEntity entity : credEntities) {
            RequiredCredentialModel credentialModel = new RequiredCredentialModel();
            credentialModel.setFormLabel(entity.getFormLabel());
            credentialModel.setInput(entity.isInput());
            credentialModel.setSecret(entity.isSecret());
            credentialModel.setType(entity.getType());

            result.add(credentialModel);
        }
        return result;
    }

    protected RequiredCredentialModel initRequiredCredentialModel(String type) {
        RequiredCredentialModel credentialModel = RequiredCredentialModel.BUILT_IN.get(type);
        if (credentialModel == null) {
            throw new RuntimeException("Unknown credential type " + type);
        }
        return credentialModel;
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        return realm.getBrowserSecurityHeaders();
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        realm.setBrowserSecurityHeaders(headers);
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return realm.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        realm.setSmtpConfig(smtpConfig);
    }

    @Override
    public List<IdentityProviderModel> getIdentityProviders() {
        return new ArrayList(allIdProviders.values());
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
        if (identityProvider.getAlias() == null) throw new NullPointerException("identityProvider.getAlias() == null");
        if (identityProvider.getInternalId() == null) identityProvider.setInternalId(KeycloakModelUtils.generateId());
        allIdProviders.put(identityProvider.getInternalId(), identityProvider);
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        for (IdentityProviderModel provider : getIdentityProviders()) {
            if (provider.getAlias().equals(alias)) {
                allIdProviders.remove(provider.getInternalId());
                break;
            }
        }
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        removeIdentityProviderByAlias(identityProvider.getAlias());
        addIdentityProvider(identityProvider);
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

                Set<UserFederationMapperEntity> mappers = getUserFederationMapperEntitiesByFederationProvider(provider.getId());
                for (UserFederationMapperEntity mapper : mappers) {
                    realm.getUserFederationMappers().remove(mapper);
                }

                it.remove();
            }
        }
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
    }

    @Override
    public List<UserFederationProviderModel> getUserFederationProviders() {
        List<UserFederationProviderEntity> entities = realm.getUserFederationProviders();
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

        return result;
    }

    @Override
    public void setUserFederationProviders(List<UserFederationProviderModel> providers) {
        for (UserFederationProviderModel currentProvider : providers) {
            KeycloakModelUtils.ensureUniqueDisplayName(currentProvider.getDisplayName(), currentProvider, providers);
        }

        List<UserFederationProviderEntity> entities = new LinkedList<UserFederationProviderEntity>();
        for (UserFederationProviderModel model : providers) {
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
                entity.setDisplayName(entity.getId());
            }
            entity.setDisplayName(displayName);
            entity.setFullSyncPeriod(model.getFullSyncPeriod());
            entity.setChangedSyncPeriod(model.getChangedSyncPeriod());
            entity.setLastSync(model.getLastSync());
            entities.add(entity);

            session.getKeycloakSessionFactory().publish(new UserFederationProviderCreationEventImpl(this, model));
        }

        realm.setUserFederationProviders(entities);
    }

    @Override
    public boolean isEventsEnabled() {
        return realm.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        realm.setEventsEnabled(enabled);
    }

    @Override
    public long getEventsExpiration() {
        return realm.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        realm.setEventsExpiration(expiration);
    }

    @Override
    public Set<String> getEventsListeners() {
        return new HashSet<String>(realm.getEventsListeners());
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        if (listeners != null) {
            realm.setEventsListeners(new ArrayList<String>(listeners));
        } else {
            realm.setEventsListeners(Collections.EMPTY_LIST);
        }
    }
    
    @Override
    public Set<String> getEnabledEventTypes() {
        return new HashSet<String>(realm.getEnabledEventTypes());
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        if (enabledEventTypes != null) {
            realm.setEnabledEventTypes(new ArrayList<String>(enabledEventTypes));
        } else {
            realm.setEnabledEventTypes(Collections.EMPTY_LIST);
        }        
    }

    @Override
    public boolean isAdminEventsEnabled() {
        return realm.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        realm.setAdminEventsEnabled(enabled);
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        return realm.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        realm.setAdminEventsDetailsEnabled(enabled);
    }
    
    @Override
    public ClientModel getMasterAdminClient() {
        return this.masterAdminApp;
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        if (client == null) {
            realm.setMasterAdminClient(null);
            this.masterAdminApp = null;
        } else {
            String appId = client.getId();
            if (appId == null) {
                throw new IllegalStateException("Master Admin app not initialized.");
            }
            realm.setMasterAdminClient(appId);
            this.masterAdminApp = client;
        }
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        //TODO: not sure if we will support identity federation storage for file
        return getIdentityProviders() != null && !getIdentityProviders().isEmpty();
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        return realm.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int accessCodeLifespanLogin) {
        realm.setAccessCodeLifespanLogin(accessCodeLifespanLogin);
    }

    @Override
    public boolean isInternationalizationEnabled() {
        return realm.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        realm.setInternationalizationEnabled(enabled);
    }

    @Override
    public Set<String> getSupportedLocales() {
        return new HashSet<>(realm.getSupportedLocales());
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        realm.setSupportedLocales(new ArrayList<>(locales));
    }

    @Override
    public String getDefaultLocale() {
        return realm.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        realm.setDefaultLocale(locale);
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
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers() {
        Set<IdentityProviderMapperModel> mappings = new HashSet<>();
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
            IdentityProviderMapperModel mapping = entityToModel(entity);
            mappings.add(mapping);
        }
        return mappings;
    }
    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
        Set<IdentityProviderMapperModel> mappings = new HashSet<>();
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
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

        this.realm.getIdentityProviderMappers().add(entity);
        return entityToModel(entity);
    }

    protected IdentityProviderMapperEntity getIdentityProviderMapperEntity(String id) {
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected IdentityProviderMapperEntity getIdentityProviderMapperEntityByName(String alias, String name) {
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
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
    public List<AuthenticationFlowModel> getAuthenticationFlows() {
        List<AuthenticationFlowEntity> flows = realm.getAuthenticationFlows();
        if (flows.size() == 0) return Collections.EMPTY_LIST;
        List<AuthenticationFlowModel> models = new LinkedList<>();
        for (AuthenticationFlowEntity entity : flows) {
            AuthenticationFlowModel model = entityToModel(entity);
            models.add(model);
        }
        return models;
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
        model.setProviderId(entity.getProviderId());
        model.setBuiltIn(entity.isBuiltIn());
        model.setTopLevel(entity.isTopLevel());
        return model;
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        for (AuthenticationFlowModel model : getAuthenticationFlows()) {
            if (model.getId().equals(id)) return model;
        }
        return null;
    }

    protected AuthenticationFlowEntity getFlowEntity(String id) {
        List<AuthenticationFlowEntity> flows = realm.getAuthenticationFlows();
        for (AuthenticationFlowEntity entity : flows) {
            if (id.equals(entity.getId())) return entity;
        }
        return null;

    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity toDelete = getFlowEntity(model.getId());
        if (toDelete == null) return;
        realm.getAuthenticationFlows().remove(toDelete);
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity toUpdate = getFlowEntity(model.getId());
        if (toUpdate == null) return;
        toUpdate.setAlias(model.getAlias());
        toUpdate.setDescription(model.getDescription());
        toUpdate.setProviderId(model.getProviderId());
        toUpdate.setBuiltIn(model.isBuiltIn());
        toUpdate.setTopLevel(model.isTopLevel());

    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity entity = new AuthenticationFlowEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAlias(model.getAlias());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setTopLevel(model.isTopLevel());
        realm.getAuthenticationFlows().add(entity);
        model.setId(entity.getId());
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
        return executions;
    }

    public AuthenticationExecutionModel entityToModel(AuthenticationExecutionEntity entity) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(entity.getId());
        model.setUserSetupAllowed(entity.isUserSetupAllowed());
        model.setRequirement(entity.getRequirement());
        model.setPriority(entity.getPriority());
        model.setAuthenticator(entity.getAuthenticator());
        model.setParentFlow(entity.getParentFlow());
        model.setFlowId(entity.getFlowId());
        model.setAutheticatorFlow(entity.isAuthenticatorFlow());
        model.setAuthenticatorConfig(entity.getAuthenticatorConfig());
        return model;
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        AuthenticationExecutionEntity execution = getAuthenticationExecutionEntity(id);
        return entityToModel(execution);
    }

    public AuthenticationExecutionEntity getAuthenticationExecutionEntity(String id) {
        List<AuthenticationFlowEntity> flows = realm.getAuthenticationFlows();
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
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setRequirement(model.getRequirement());
        entity.setUserSetupAllowed(model.isUserSetupAllowed());
        entity.setAuthenticatorFlow(model.isAutheticatorFlow());
        entity.setFlowId(model.getFlowId());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        AuthenticationFlowEntity flow = getFlowEntity(model.getId());
        flow.getExecutions().add(entity);
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
        entity.setAuthenticatorFlow(model.isAutheticatorFlow());
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setRequirement(model.getRequirement());
        entity.setFlowId(model.getFlowId());
        entity.setUserSetupAllowed(model.isUserSetupAllowed());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
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

    }

    @Override
    public List<AuthenticatorConfigModel> getAuthenticatorConfigs() {
        List<AuthenticatorConfigModel> authenticators = new LinkedList<>();
        for (AuthenticatorConfigEntity entity : realm.getAuthenticatorConfigs()) {
            authenticators.add(entityToModel(entity));
        }
        return authenticators;
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity auth = new AuthenticatorConfigEntity();
        auth.setId(KeycloakModelUtils.generateId());
        auth.setAlias(model.getAlias());
        auth.setConfig(model.getConfig());
        realm.getAuthenticatorConfigs().add(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity entity = getAuthenticatorEntity(model.getId());
        if (entity == null) return;
        realm.getAuthenticatorConfigs().remove(entity);

    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        AuthenticatorConfigEntity entity = getAuthenticatorEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    public AuthenticatorConfigEntity getAuthenticatorEntity(String id) {
        AuthenticatorConfigEntity entity = null;
        for (AuthenticatorConfigEntity auth : realm.getAuthenticatorConfigs()) {
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
        AuthenticatorConfigEntity entity = getAuthenticatorEntity(model.getId());
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(model.getConfig());
        }
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
        return model;
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = getRequiredActionProviderEntity(model.getId());
        if (entity == null) return;
        realm.getRequiredActionProviders().remove(entity);
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
        entity.setProviderId(model.getProviderId());
        entity.setEnabled(model.isEnabled());
        entity.setName(model.getName());
        entity.setDefaultAction(model.isDefaultAction());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(model.getConfig());
        }
    }

    @Override
    public List<RequiredActionProviderModel> getRequiredActionProviders() {
        List<RequiredActionProviderModel> actions = new LinkedList<>();
        for (RequiredActionProviderEntity entity : realm.getRequiredActionProviders()) {
            actions.add(entityToModel(entity));
        }
        return actions;
    }

    public RequiredActionProviderEntity getRequiredActionProviderEntity(String id) {
        RequiredActionProviderEntity entity = null;
        for (RequiredActionProviderEntity auth : realm.getRequiredActionProviders()) {
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
        Set<UserFederationMapperModel> mappers = new HashSet<UserFederationMapperModel>();
        for (UserFederationMapperEntity entity : this.realm.getUserFederationMappers()) {
            UserFederationMapperModel mapper = entityToModel(entity);
            mappers.add(mapper);
        }
        return mappers;
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

        this.realm.getUserFederationMappers().add(entity);
        UserFederationMapperModel mapperModel = entityToModel(entity);

        session.getKeycloakSessionFactory().publish(new UserFederationMapperEventImpl(mapperModel, this, session));

        return mapperModel;
    }

    protected UserFederationMapperEntity getUserFederationMapperEntity(String id) {
        for (UserFederationMapperEntity entity : this.realm.getUserFederationMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected UserFederationMapperEntity getUserFederationMapperEntityByName(String federationProviderId, String name) {
        for (UserFederationMapperEntity entity : this.realm.getUserFederationMappers()) {
            if (entity.getFederationProviderId().equals(federationProviderId) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    protected Set<UserFederationMapperEntity> getUserFederationMapperEntitiesByFederationProvider(String federationProviderId) {
        Set<UserFederationMapperEntity> mappers = new HashSet<UserFederationMapperEntity>();
        for (UserFederationMapperEntity entity : this.realm.getUserFederationMappers()) {
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

        session.getKeycloakSessionFactory().publish(new UserFederationMapperEventImpl(mapper, this, session));
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

 }
