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

import org.keycloak.enums.SslRequired;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.entities.RequiredCredentialEntity;
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
import org.keycloak.models.ClaimTypeModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.entities.ApplicationEntity;
import org.keycloak.models.entities.ClaimTypeEntity;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.entities.IdentityProviderEntity;
import org.keycloak.models.entities.OAuthClientEntity;
import org.keycloak.models.entities.ProtocolMapperEntity;
import org.keycloak.models.entities.RealmEntity;
import org.keycloak.models.entities.RoleEntity;
import org.keycloak.models.file.InMemoryModel;

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

    private final Map<String, ApplicationModel> allApps = new HashMap<String, ApplicationModel>();
    private ApplicationModel masterAdminApp = null;
    private final Map<String, RoleAdapter> allRoles = new HashMap<String, RoleAdapter>();
    private final Map<String, OAuthClientAdapter> allOAuthClients = new HashMap<String, OAuthClientAdapter>();
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
    public boolean isPasswordCredentialGrantAllowed() {
        return realm.isPasswordCredentialGrantAllowed();
    }

    @Override
    public void setPasswordCredentialGrantAllowed(boolean passwordCredentialGrantAllowed) {
        realm.setPasswordCredentialGrantAllowed(passwordCredentialGrantAllowed);
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

        for (ApplicationModel app : getApplications()) {
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

        for (ApplicationModel app : getApplications()) {
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
    public ClientModel findClient(String clientId) {
        ClientModel model = getApplicationByName(clientId);
        if (model != null) return model;
        return getOAuthClient(clientId);
    }

    @Override
    public ClientModel findClientById(String id) {
        ClientModel clientModel = getApplicationById(id);
        if (clientModel != null) return clientModel;
        return getOAuthClientById(id);
    }



    @Override
    public ApplicationModel getApplicationById(String id) {
        return allApps.get(id);
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        for (ApplicationModel app : getApplications()) {
            if (app.getName().equals(name)) return app;
        }

        return null;
    }

    @Override
    public Map<String, ApplicationModel> getApplicationNameMap() {
        Map<String, ApplicationModel> resourceMap = new HashMap<String, ApplicationModel>();
        for (ApplicationModel resource : getApplications()) {
            resourceMap.put(resource.getName(), resource);
        }
        return resourceMap;
    }

    @Override
    public List<ApplicationModel> getApplications() {
        return new ArrayList<ApplicationModel>(allApps.values());
    }

    @Override
    public ApplicationModel addApplication(String name) {
        return this.addApplication(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public ApplicationModel addApplication(String id, String name) {
        if (name == null) throw new NullPointerException("name == null");
        if (id == null) throw new NullPointerException("id == null");

        if (getApplicationNameMap().containsKey(name)) {
            throw new ModelDuplicateException("Application named '" + name + "' already exists.");
        }

        ApplicationEntity appEntity = new ApplicationEntity();
        appEntity.setId(id);
        appEntity.setName(name);
        appEntity.setRealmId(getId());
        appEntity.setEnabled(true);

        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setId(id);
        clientEntity.setName(name);
        clientEntity.setRealmId(getId());
        clientEntity.setEnabled(true);

        ApplicationModel app = new ApplicationAdapter(session, this, appEntity, clientEntity, inMemoryModel);
        allApps.put(id, app);

        return app;
    }

    @Override
    public boolean removeApplication(String id) {
        ApplicationModel appToBeRemoved = this.getApplicationById(id);
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

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        return this.addOAuthClient(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public OAuthClientModel addOAuthClient(String id, String name) {
        if (id == null) throw new NullPointerException("id == null");
        if (name == null) throw new NullPointerException("name == null");
        if (hasOAuthClientWithName(name)) throw new ModelDuplicateException("OAuth Client with name " + name + " already exists.");
        OAuthClientEntity oauthClient = new OAuthClientEntity();
        oauthClient.setId(id);
        oauthClient.setRealmId(getId());
        oauthClient.setName(name);

        OAuthClientAdapter oAuthClient = new OAuthClientAdapter(session, this, oauthClient);
        allOAuthClients.put(id, oAuthClient);

        return oAuthClient;
    }

    boolean hasOAuthClientWithName(String name) {
        for (OAuthClientAdapter oaClient : allOAuthClients.values()) {
            if (oaClient.getName().equals(name)) return true;
        }

        return false;
    }

    boolean hasOAuthClientWithClientId(String id) {
        for (OAuthClientAdapter oaClient : allOAuthClients.values()) {
            if (oaClient.getClientId().equals(id)) return true;
        }

        return false;
    }

    boolean hasUserWithEmail(String email) {
        for (UserModel user : inMemoryModel.getUsers(getId())) {
            if (user.getEmail() == null) continue;
            if (user.getEmail().equals(email)) return true;
        }

        return false;
    }

    @Override
    public boolean removeOAuthClient(String id) {
        return allOAuthClients.remove(id) != null;
    }

    @Override
    public OAuthClientModel getOAuthClient(String name) {
        for (OAuthClientAdapter oAuthClient : allOAuthClients.values()) {
            if (oAuthClient.getName().equals(name)) return oAuthClient;
        }

        return null;
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        for (OAuthClientAdapter oAuthClient : allOAuthClients.values()) {
            if (oAuthClient.getId().equals(id)) return oAuthClient;
        }

        return null;
    }

    @Override
    public List<OAuthClientModel> getOAuthClients() {
        return new ArrayList(allOAuthClients.values());
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
    public IdentityProviderModel getIdentityProviderById(String identityProviderId) {
        for (IdentityProviderModel identityProviderModel : getIdentityProviders()) {
            if (identityProviderModel.getId().equals(identityProviderId)) {
                return identityProviderModel;
            }
        }

        return null;
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        if (identityProvider.getId() == null) throw new NullPointerException("identityProvider.getId() == null");
        if (identityProvider.getInternalId() == null) identityProvider.setInternalId(KeycloakModelUtils.generateId());
        allIdProviders.put(identityProvider.getInternalId(), identityProvider);
    }

    @Override
    public void removeIdentityProviderById(String providerId) {
        for (IdentityProviderModel provider : getIdentityProviders()) {
            if (provider.getId().equals(providerId)) {
                allIdProviders.remove(provider.getInternalId());
                break;
            }
        }
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        removeIdentityProviderById(identityProvider.getId());
        addIdentityProvider(identityProvider);
    }

    @Override
    public UserFederationProviderModel addUserFederationProvider(String providerName, Map<String, String> config, int priority, String displayName, int fullSyncPeriod, int changedSyncPeriod, int lastSync) {
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

        return new UserFederationProviderModel(entity.getId(), providerName, config, priority, displayName, fullSyncPeriod, changedSyncPeriod, lastSync);
    }

    @Override
    public void removeUserFederationProvider(UserFederationProviderModel provider) {
        Iterator<UserFederationProviderEntity> it = realm.getUserFederationProviders().iterator();
        while (it.hasNext()) {
            UserFederationProviderEntity entity = it.next();
            if (entity.getId().equals(provider.getId())) {
                session.users().preRemove(this, new UserFederationProviderModel(entity.getId(), entity.getProviderName(), entity.getConfig(), entity.getPriority(), entity.getDisplayName(),
                        entity.getFullSyncPeriod(), entity.getChangedSyncPeriod(), entity.getLastSync()));
                it.remove();
            }
        }
    }

    @Override
    public void updateUserFederationProvider(UserFederationProviderModel model) {
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
        List<UserFederationProviderEntity> entities = new LinkedList<UserFederationProviderEntity>();
        for (UserFederationProviderModel model : providers) {
            UserFederationProviderEntity entity = new UserFederationProviderEntity();
            if (model.getId() != null) entity.setId(model.getId());
            else entity.setId(KeycloakModelUtils.generateId());
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
    public ApplicationModel getMasterAdminApp() {
        return this.masterAdminApp;
    }

    @Override
    public void setMasterAdminApp(ApplicationModel app) {
        if (app == null) throw new NullPointerException("app == null");
        String appId = app.getId();
        if (appId == null) {
            throw new IllegalStateException("Master Admin app not initialized.");
        }
        realm.setAdminAppId(appId);
        this.masterAdminApp = app;
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        //TODO: not sure if we will support identity federation storage for file
        return true;
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        Set<ProtocolMapperModel> result = new HashSet<ProtocolMapperModel>();
        for (ProtocolMapperEntity entity : realm.getProtocolMappers()) {
            ProtocolMapperModel mapping = new ProtocolMapperModel();
            mapping.setId(entity.getId());
            mapping.setName(entity.getName());
            mapping.setProtocol(entity.getProtocol());
            mapping.setAppliedByDefault(entity.isAppliedByDefault());
            mapping.setConsentRequired(entity.isConsentRequired());
            mapping.setConsentText(entity.getConsentText());
            Map<String, String> config = new HashMap<String, String>();
            if (entity.getConfig() != null) {
                config.putAll(entity.getConfig());
            }
            mapping.setConfig(config);
        }
        return result;
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (getProtocolMapperByName(model.getProtocol(), model.getName()) != null) {
            throw new RuntimeException("protocol mapper name must be unique per protocol");
        }
        ProtocolMapperEntity entity = new ProtocolMapperEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setProtocol(model.getProtocol());
        entity.setName(model.getName());
        entity.setAppliedByDefault(model.isAppliedByDefault());
        entity.setProtocolMapper(model.getProtocolMapper());
        entity.setConfig(model.getConfig());
        entity.setConsentRequired(model.isConsentRequired());
        entity.setConsentText(model.getConsentText());
        realm.getProtocolMappers().add(entity);
        return entityToModel(entity);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        for (ProtocolMapperEntity entity : realm.getProtocolMappers()) {
            if (entity.getId().equals(mapping.getId())) {
                realm.getProtocolMappers().remove(entity);
                break;
            }
        }

    }

    protected ProtocolMapperEntity getProtocolMapperyEntityById(String id) {
        for (ProtocolMapperEntity entity : realm.getProtocolMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected ProtocolMapperEntity getProtocolMapperEntityByName(String protocol, String name) {
        for (ProtocolMapperEntity entity : realm.getProtocolMappers()) {
            if (entity.getProtocol().equals(protocol) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity entity = getProtocolMapperyEntityById(mapping.getId());
        entity.setAppliedByDefault(mapping.isAppliedByDefault());
        entity.setProtocolMapper(mapping.getProtocolMapper());
        entity.setConsentRequired(mapping.isConsentRequired());
        entity.setConsentText(mapping.getConsentText());
        if (entity.getConfig() != null) {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapping.getConfig());
        } else {
            entity.setConfig(mapping.getConfig());
        }
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        ProtocolMapperEntity entity = getProtocolMapperyEntityById(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        ProtocolMapperEntity entity = getProtocolMapperEntityByName(protocol, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected ProtocolMapperModel entityToModel(ProtocolMapperEntity entity) {
        ProtocolMapperModel mapping = new ProtocolMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setProtocol(entity.getProtocol());
        mapping.setAppliedByDefault(entity.isAppliedByDefault());
        mapping.setProtocolMapper(entity.getProtocolMapper());
        mapping.setConsentRequired(entity.isConsentRequired());
        mapping.setConsentText(entity.getConsentText());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }

    @Override
    public Set<ClaimTypeModel> getClaimTypes() {
        Set<ClaimTypeModel> result = new HashSet<ClaimTypeModel>();
        for (ClaimTypeEntity entity : realm.getClaimTypes()) {
            result.add(new ClaimTypeModel(entity.getId(), entity.getName(), entity.isBuiltIn(), entity.getType()));
        }
       return result;
    }

    @Override
    public ClaimTypeModel addClaimType(ClaimTypeModel model) {
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        ClaimTypeModel claim = new ClaimTypeModel(id, model.getName(), model.isBuiltIn(), model.getType());
        ClaimTypeEntity entity = new ClaimTypeEntity();
        entity.setId(claim.getId());
        entity.setType(model.getType());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setName(model.getName());
        realm.getClaimTypes().add(entity);
        return claim;
    }

    @Override
    public void removeClaimType(ClaimTypeModel claimType) {
        for (ClaimTypeEntity entity : realm.getClaimTypes()) {
            if (entity.getId().equals(claimType.getId())) {
                realm.getClaimTypes().remove(entity);
                break;
            }
        }
    }

    @Override
    public ClaimTypeModel getClaimType(String name) {
        for (ClaimTypeModel claimType : getClaimTypes()) {
            if (claimType.getName().equals(name)) return claimType;
        }
        return null;
    }

    @Override
    public void updateClaimType(ClaimTypeModel claimType) {
        for (ClaimTypeEntity entity : realm.getClaimTypes()) {
            if (entity.getId().equals(claimType.getId())) {
                entity.setName(claimType.getName());
                entity.setBuiltIn(claimType.isBuiltIn());
                entity.setType(claimType.getType());
                break;
            }
        }
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
}
