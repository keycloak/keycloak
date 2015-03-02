package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.enums.SslRequired;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClaimTypeModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.entities.ClaimTypeEntity;
import org.keycloak.models.entities.IdentityProviderEntity;
import org.keycloak.models.entities.ProtocolMapperEntity;
import org.keycloak.models.entities.RequiredCredentialEntity;
import org.keycloak.models.entities.UserFederationProviderEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOAuthClientEntity;
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
        return SslRequired.valueOf(realm.getSslRequired());
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        realm.setSslRequired(sslRequired.name());
        updateRealm();
    }

    @Override
    public boolean isPasswordCredentialGrantAllowed() {
        return realm.isPasswordCredentialGrantAllowed();
    }

    @Override
    public void setPasswordCredentialGrantAllowed(boolean passwordCredentialGrantAllowed) {
        realm.setPasswordCredentialGrantAllowed(passwordCredentialGrantAllowed);
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
    public ClientModel findClient(String clientId) {
        ClientModel model = getApplicationByName(clientId);
        if (model != null) return model;
        return getOAuthClient(clientId);
    }

    @Override
    public ClientModel findClientById(String id) {
        ClientModel model = getApplicationById(id);
        if (model != null) return model;
        return getOAuthClientById(id);
    }



    @Override
    public ApplicationModel getApplicationById(String id) {
        return model.getApplicationById(id, this);
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .and("name").is(name)
                .get();
        MongoApplicationEntity appEntity = getMongoStore().loadSingleEntity(MongoApplicationEntity.class, query, invocationContext);
        return appEntity == null ? null : new ApplicationAdapter(session, this, appEntity, invocationContext);
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
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<MongoApplicationEntity> appDatas = getMongoStore().loadEntities(MongoApplicationEntity.class, query, invocationContext);

        List<ApplicationModel> result = new ArrayList<ApplicationModel>();
        for (MongoApplicationEntity appData : appDatas) {
            result.add(new ApplicationAdapter(session, this, appData, invocationContext));
        }
        return result;
    }

    public void addDefaultClientProtocolMappers(ClientModel client) {
        Set<String> adding = new HashSet<String>();
        for (ProtocolMapperEntity mapper : realm.getProtocolMappers()) {
            if (mapper.isAppliedByDefault()) adding.add(mapper.getId());
        }
        client.setProtocolMappers(adding);

    }
    @Override
    public ApplicationModel addApplication(String name) {
        return this.addApplication(null, name);
    }

    @Override
    public ApplicationModel addApplication(String id, String name) {
        MongoApplicationEntity appData = new MongoApplicationEntity();
        appData.setId(id);
        appData.setName(name);
        appData.setRealmId(getId());
        appData.setEnabled(true);
        getMongoStore().insertEntity(appData, invocationContext);

        ApplicationModel model = new ApplicationAdapter(session, this, appData, invocationContext);
        addDefaultClientProtocolMappers(model);
        return model;
    }

    @Override
    public boolean removeApplication(String id) {
        return getMongoStore().removeEntity(MongoApplicationEntity.class, id, invocationContext);
    }

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        return this.addOAuthClient(null, name);
    }

    @Override
    public OAuthClientModel addOAuthClient(String id, String name) {
        MongoOAuthClientEntity oauthClient = new MongoOAuthClientEntity();
        oauthClient.setId(id);
        oauthClient.setRealmId(getId());
        oauthClient.setName(name);
        getMongoStore().insertEntity(oauthClient, invocationContext);

        OAuthClientAdapter model = new OAuthClientAdapter(session, this, oauthClient, invocationContext);
        addDefaultClientProtocolMappers(model);
        return model;
    }

    @Override
    public boolean removeOAuthClient(String id) {
        return getMongoStore().removeEntity(MongoOAuthClientEntity.class, id, invocationContext);
    }

    @Override
    public OAuthClientModel getOAuthClient(String name) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .and("name").is(name)
                .get();
        MongoOAuthClientEntity oauthClient = getMongoStore().loadSingleEntity(MongoOAuthClientEntity.class, query, invocationContext);
        return oauthClient == null ? null : new OAuthClientAdapter(session, this, oauthClient, invocationContext);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        return model.getOAuthClientById(id, this);
    }

    @Override
    public List<OAuthClientModel> getOAuthClients() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<MongoOAuthClientEntity> results = getMongoStore().loadEntities(MongoOAuthClientEntity.class, query, invocationContext);
        List<OAuthClientModel> list = new ArrayList<OAuthClientModel>();
        for (MongoOAuthClientEntity data : results) {
            list.add(new OAuthClientAdapter(session, this, data, invocationContext));
        }
        return list;
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
        updateRealm();
        return entityToModel(entity);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        for (ProtocolMapperEntity entity : realm.getProtocolMappers()) {
            if (entity.getId().equals(mapping.getId())) {
                realm.getProtocolMappers().remove(entity);
                updateRealm();
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
        updateRealm();

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
        updateRealm();
        return claim;
    }

    @Override
    public void removeClaimType(ClaimTypeModel claimType) {
        for (ClaimTypeEntity entity : realm.getClaimTypes()) {
            if (entity.getId().equals(claimType.getId())) {
                realm.getClaimTypes().remove(entity);
                updateRealm();
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
                updateRealm();
                break;
            }
        }
    }


    @Override
    public List<IdentityProviderModel> getIdentityProviders() {
        List<IdentityProviderModel> identityProviders = new ArrayList<IdentityProviderModel>();

        for (IdentityProviderEntity entity: realm.getIdentityProviders()) {
            IdentityProviderModel identityProviderModel = new IdentityProviderModel();

            identityProviderModel.setProviderId(entity.getProviderId());
            identityProviderModel.setId(entity.getId());
            identityProviderModel.setInternalId(entity.getInternalId());
            identityProviderModel.setName(entity.getName());
            identityProviderModel.setConfig(entity.getConfig());
            identityProviderModel.setEnabled(entity.isEnabled());
            identityProviderModel.setUpdateProfileFirstLogin(entity.isUpdateProfileFirstLogin());
            identityProviderModel.setAuthenticateByDefault(entity.isAuthenticateByDefault());
            identityProviderModel.setStoreToken(entity.isStoreToken());

            identityProviders.add(identityProviderModel);
        }

        return identityProviders;
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
        IdentityProviderEntity entity = new IdentityProviderEntity();

        entity.setInternalId(KeycloakModelUtils.generateId());
        entity.setId(identityProvider.getId());
        entity.setProviderId(identityProvider.getProviderId());
        entity.setName(identityProvider.getName());
        entity.setEnabled(identityProvider.isEnabled());
        entity.setUpdateProfileFirstLogin(identityProvider.isUpdateProfileFirstLogin());
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
        entity.setConfig(identityProvider.getConfig());

        realm.getIdentityProviders().add(entity);
        updateRealm();
    }

    @Override
    public void removeIdentityProviderById(String providerId) {
        IdentityProviderEntity toRemove;
        for (IdentityProviderEntity entity : realm.getIdentityProviders()) {
            if (entity.getId().equals(providerId)) {
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
                entity.setId(identityProvider.getId());
                entity.setName(identityProvider.getName());
                entity.setEnabled(identityProvider.isEnabled());
                entity.setUpdateProfileFirstLogin(identityProvider.isUpdateProfileFirstLogin());
                entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
                entity.setStoreToken(identityProvider.isStoreToken());
                entity.setConfig(identityProvider.getConfig());
            }
        }

        updateRealm();
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
        updateRealm();

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
        updateRealm();
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
    public ApplicationModel getMasterAdminApp() {
        MongoApplicationEntity appData = getMongoStore().loadEntity(MongoApplicationEntity.class, realm.getAdminAppId(), invocationContext);
        return appData != null ? new ApplicationAdapter(session, this, appData, invocationContext) : null;
    }

    @Override
    public void setMasterAdminApp(ApplicationModel app) {
        String adminAppId = app != null ? app.getId() : null;
        realm.setAdminAppId(adminAppId);
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
            realm.setEventsListeners(new ArrayList<String>(locales));
        } else {
            realm.setEventsListeners(Collections.EMPTY_LIST);
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
}
