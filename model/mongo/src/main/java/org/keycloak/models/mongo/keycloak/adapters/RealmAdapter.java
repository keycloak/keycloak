package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
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
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperEventImpl;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderCreationEventImpl;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.entities.AuthenticationExecutionEntity;
import org.keycloak.models.entities.AuthenticationFlowEntity;
import org.keycloak.models.entities.AuthenticatorConfigEntity;
import org.keycloak.models.entities.IdentityProviderEntity;
import org.keycloak.models.entities.IdentityProviderMapperEntity;
import org.keycloak.models.entities.RequiredActionProviderEntity;
import org.keycloak.models.entities.RequiredCredentialEntity;
import org.keycloak.models.entities.UserFederationMapperEntity;
import org.keycloak.models.entities.UserFederationProviderEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoClientEntity;
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
            passwordPolicy = new PasswordPolicy(realm.getPasswordPolicy());
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
    public int getNotBefore() {
        return realm.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        realm.setNotBefore(notBefore);
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
    public int getAccessTokenLifespan() {
        return realm.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int tokenLifespan) {
        realm.setAccessTokenLifespan(tokenLifespan);
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
    public RoleAdapter getRole(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .and("realmId").is(getId())
                .get();
        MongoRoleEntity role = getMongoStore().loadSingleEntity(MongoRoleEntity.class, query, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(session, this, role, this, invocationContext);
        }
    }

    @Override
    public RoleModel addRole(String name) {
        return this.addRole(null, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        MongoRoleEntity roleEntity = new MongoRoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setRealmId(getId());

        getMongoStore().insertEntity(roleEntity, invocationContext);

        return new RoleAdapter(session, this, roleEntity, this, invocationContext);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return removeRoleById(role.getId());
    }

    @Override
    public boolean removeRoleById(String id) {
        RoleModel role = getRoleById(id);
        if (role == null) return false;
        session.users().preRemove(this, role);
        return getMongoStore().removeEntity(MongoRoleEntity.class, id, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoles() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<MongoRoleEntity> roles = getMongoStore().loadEntities(MongoRoleEntity.class, query, invocationContext);

        Set<RoleModel> result = new HashSet<RoleModel>();

        if (roles == null) return result;
        for (MongoRoleEntity role : roles) {
            result.add(new RoleAdapter(session, this, role, this, invocationContext));
        }

        return result;
    }

    @Override
    public RoleModel getRoleById(String id) {
        return model.getRoleById(id, this);
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

    @Override
    public ClientModel getClientById(String id) {
        return model.getClientById(id, this);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .and("clientId").is(clientId)
                .get();
        MongoClientEntity appEntity = getMongoStore().loadSingleEntity(MongoClientEntity.class, query, invocationContext);
        return appEntity == null ? null : new ClientAdapter(session, this, appEntity, invocationContext);
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
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<MongoClientEntity> clientEntities = getMongoStore().loadEntities(MongoClientEntity.class, query, invocationContext);

        List<ClientModel> result = new ArrayList<ClientModel>();
        for (MongoClientEntity clientEntity : clientEntities) {
            result.add(new ClientAdapter(session, this, clientEntity, invocationContext));
        }
        return result;
    }

    @Override
    public ClientModel addClient(String name) {
        return this.addClient(null, name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        MongoClientEntity clientEntity = new MongoClientEntity();
        clientEntity.setId(id);
        clientEntity.setClientId(clientId);
        clientEntity.setRealmId(getId());
        clientEntity.setEnabled(true);
        getMongoStore().insertEntity(clientEntity, invocationContext);

        final ClientModel model = new ClientAdapter(session, this, clientEntity, invocationContext);
        session.getKeycloakSessionFactory().publish(new ClientCreationEvent() {
            @Override
            public ClientModel getCreatedClient() {
                return model;
            }
        });
        return model;
    }

    @Override
    public boolean removeClient(String id) {
        if (id == null) return false;
        ClientModel client = getClientById(id);
        if (client == null) return false;

        session.users().preRemove(this, client);

        return getMongoStore().removeEntity(MongoClientEntity.class, id, invocationContext);
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

        List<RequiredCredentialModel> result = new ArrayList<RequiredCredentialModel>();
        for (RequiredCredentialEntity entity : credEntities) {
            RequiredCredentialModel model = new RequiredCredentialModel();
            model.setFormLabel(entity.getFormLabel());
            model.setInput(entity.isInput());
            model.setSecret(entity.isSecret());
            model.setType(entity.getType());

            result.add(model);
        }
        return result;
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
        return realm.getBrowserSecurityHeaders();
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        realm.setBrowserSecurityHeaders(headers);
        updateRealm();
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return realm.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        realm.setSmtpConfig(smtpConfig);
        updateRealm();
    }


    @Override
    public List<IdentityProviderModel> getIdentityProviders() {
        List<IdentityProviderModel> identityProviders = new ArrayList<IdentityProviderModel>();

        for (IdentityProviderEntity entity: realm.getIdentityProviders()) {
            IdentityProviderModel identityProviderModel = new IdentityProviderModel();

            identityProviderModel.setProviderId(entity.getProviderId());
            identityProviderModel.setAlias(entity.getAlias());
            identityProviderModel.setInternalId(entity.getInternalId());
            identityProviderModel.setConfig(entity.getConfig());
            identityProviderModel.setEnabled(entity.isEnabled());
            identityProviderModel.setUpdateProfileFirstLoginMode(entity.getUpdateProfileFirstLoginMode());
            identityProviderModel.setTrustEmail(entity.isTrustEmail());
            identityProviderModel.setAuthenticateByDefault(entity.isAuthenticateByDefault());
            identityProviderModel.setStoreToken(entity.isStoreToken());
            identityProviderModel.setAddReadTokenRoleOnCreate(entity.isAddReadTokenRoleOnCreate());

            identityProviders.add(identityProviderModel);
        }

        return identityProviders;
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
        entity.setUpdateProfileFirstLoginMode(identityProvider.getUpdateProfileFirstLoginMode());
        entity.setTrustEmail(identityProvider.isTrustEmail());
        entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
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
                entity.setUpdateProfileFirstLoginMode(identityProvider.getUpdateProfileFirstLoginMode());
                entity.setTrustEmail(identityProvider.isTrustEmail());
                entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
                entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
                entity.setStoreToken(identityProvider.isStoreToken());
                entity.setConfig(identityProvider.getConfig());
            }
        }

        updateRealm();
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

    private void removeFederationMappersForProvider(String federationProviderId) {
        Set<UserFederationMapperEntity> mappers = getUserFederationMapperEntitiesByFederationProvider(federationProviderId);
        for (UserFederationMapperEntity mapper : mappers) {
            getMongoEntity().getUserFederationMappers().remove(mapper);
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
        updateRealm();
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
        return new HashSet<String>(realm.getEventsListeners());
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
        return new HashSet<String>(realm.getEnabledEventTypes());
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
        return appData != null ? new ClientAdapter(session, this, appData, invocationContext) : null;
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
        return new HashSet<String>(realm.getSupportedLocales());
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
        Set<IdentityProviderMapperModel> mappings = new HashSet<IdentityProviderMapperModel>();
        for (IdentityProviderMapperEntity entity : getMongoEntity().getIdentityProviderMappers()) {
            IdentityProviderMapperModel mapping = entityToModel(entity);
            mappings.add(mapping);
        }
        return mappings;
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
    public List<AuthenticationFlowModel> getAuthenticationFlows() {
        List<AuthenticationFlowEntity> flows = getMongoEntity().getAuthenticationFlows();
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
        model.setBuiltIn(entity.isBuiltIn());
        model.setTopLevel(entity.isTopLevel());
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
        return executions;
    }

    public AuthenticationExecutionModel entityToModel(AuthenticationExecutionEntity entity) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(entity.getId());
        model.setUserSetupAllowed(entity.isUserSetupAllowed());
        model.setRequirement(entity.getRequirement());
        model.setPriority(entity.getPriority());
        model.setAuthenticator(entity.getAuthenticator());
        model.setFlowId(entity.getFlowId());
        model.setParentFlow(entity.getParentFlow());
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
        entity.setUserSetupAllowed(model.isUserSetupAllowed());
        entity.setAuthenticatorFlow(model.isAutheticatorFlow());
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
        entity.setAuthenticatorFlow(model.isAutheticatorFlow());
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setRequirement(model.getRequirement());
        entity.setFlowId(model.getFlowId());
        entity.setUserSetupAllowed(model.isUserSetupAllowed());
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
        List<AuthenticatorConfigModel> authenticators = new LinkedList<>();
        for (AuthenticatorConfigEntity entity : getMongoEntity().getAuthenticatorConfigs()) {
            authenticators.add(entityToModel(entity));
        }
        return authenticators;
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
        List<RequiredActionProviderModel> actions = new LinkedList<>();
        for (RequiredActionProviderEntity entity : realm.getRequiredActionProviders()) {
            actions.add(entityToModel(entity));
        }
        return actions;
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
        Set<UserFederationMapperModel> mappers = new HashSet<UserFederationMapperModel>();
        for (UserFederationMapperEntity entity : getMongoEntity().getUserFederationMappers()) {
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

        getMongoEntity().getUserFederationMappers().add(entity);
        updateMongoEntity();
        UserFederationMapperModel mapperModel = entityToModel(entity);

        session.getKeycloakSessionFactory().publish(new UserFederationMapperEventImpl(mapperModel, this, session));

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
