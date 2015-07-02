package org.keycloak.models.jpa;

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
import org.keycloak.models.jpa.entities.AuthenticationExecutionEntity;
import org.keycloak.models.jpa.entities.AuthenticationFlowEntity;
import org.keycloak.models.jpa.entities.AuthenticatorConfigEntity;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.IdentityProviderEntity;
import org.keycloak.models.jpa.entities.IdentityProviderMapperEntity;
import org.keycloak.models.jpa.entities.RealmAttributeEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RequiredActionProviderEntity;
import org.keycloak.models.jpa.entities.RequiredCredentialEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.UserFederationMapperEntity;
import org.keycloak.models.jpa.entities.UserFederationProviderEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

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
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdapter implements RealmModel {
    protected RealmEntity realm;
    protected EntityManager em;
    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;
    protected volatile transient X509Certificate certificate;
    protected volatile transient Key codeSecretKey;
    protected KeycloakSession session;
    private PasswordPolicy passwordPolicy;

    public RealmAdapter(KeycloakSession session, EntityManager em, RealmEntity realm) {
        this.session = session;
        this.em = em;
        this.realm = realm;
    }

    public RealmEntity getEntity() {
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
        realm.setName(name);
        em.flush();
    }

    @Override
    public boolean isEnabled() {
        return realm.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        realm.setEnabled(enabled);
        em.flush();
    }

    @Override
    public SslRequired getSslRequired() {
        return realm.getSslRequired() != null ? SslRequired.valueOf(realm.getSslRequired()) : null;
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        realm.setSslRequired(sslRequired.name());
        em.flush();
    }

    @Override
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
        em.flush();
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        return realm.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        realm.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        em.flush();
    }

    @Override
    public boolean isRememberMe() {
        return realm.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        realm.setRememberMe(rememberMe);
        em.flush();
    }

    public void setAttribute(String name, String value) {
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            if (attr.getName().equals(name)) {
                attr.setValue(value);
                return;
            }
        }
        RealmAttributeEntity attr = new RealmAttributeEntity();
        attr.setName(name);
        attr.setValue(value);
        attr.setRealm(realm);
        em.persist(attr);
        realm.getAttributes().add(attr);
    }

    public void setAttribute(String name, Boolean value) {
        setAttribute(name, value.toString());
    }

    public void setAttribute(String name, Integer value) {
        setAttribute(name, value.toString());
    }

    public void setAttribute(String name, Long value) {
        setAttribute(name, value.toString());
    }

    public void removeAttribute(String name) {
        Iterator<RealmAttributeEntity> it = realm.getAttributes().iterator();
        while (it.hasNext()) {
            RealmAttributeEntity attr = it.next();
            if (attr.getName().equals(name)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    public String getAttribute(String name) {
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            if (attr.getName().equals(name)) {
                return attr.getValue();
            }
        }
        return null;
    }

    public Integer getAttribute(String name, Integer defaultValue) {
        String v = getAttribute(name);
        return v != null ? Integer.parseInt(v) : defaultValue;

    }

    public Long getAttribute(String name, Long defaultValue) {
        String v = getAttribute(name);
        return v != null ? Long.parseLong(v) : defaultValue;

    }

    public Boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;

    }

    public Map<String, String> getAttributes() {
        // should always return a copy
        Map<String, String> result = new HashMap<String, String>();
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            result.put(attr.getName(), attr.getValue());
        }
        return result;
    }
    @Override
    public boolean isBruteForceProtected() {
        return getAttribute("bruteForceProtected", false);
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        setAttribute("bruteForceProtected", value);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return getAttribute("maxFailureWaitSeconds", 0);
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        setAttribute("maxFailureWaitSeconds", val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return getAttribute("waitIncrementSeconds", 0);
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        setAttribute("waitIncrementSeconds", val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return getAttribute("quickLoginCheckMilliSeconds", 0l);
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        setAttribute("quickLoginCheckMilliSeconds", val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return getAttribute("minimumQuickLoginWaitSeconds", 0);
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        setAttribute("minimumQuickLoginWaitSeconds", val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return getAttribute("maxDeltaTimeSeconds", 0);
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        setAttribute("maxDeltaTimeSeconds", val);
    }

    @Override
    public int getFailureFactor() {
        return getAttribute("failureFactor", 0);
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        setAttribute("failureFactor", failureFactor);
    }

    @Override
    public boolean isVerifyEmail() {
        return realm.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        realm.setVerifyEmail(verifyEmail);
        em.flush();
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        realm.setResetPasswordAllowed(resetPasswordAllowed);
        em.flush();
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return realm.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        realm.setEditUsernameAllowed(editUsernameAllowed);
        em.flush();
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
    public int getAccessTokenLifespan() {
        return realm.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int tokenLifespan) {
        realm.setAccessTokenLifespan(tokenLifespan);
        em.flush();
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
    public int getAccessCodeLifespan() {
        return realm.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int accessCodeLifespan) {
        realm.setAccessCodeLifespan(accessCodeLifespan);
        em.flush();
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return realm.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        realm.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
        em.flush();
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        return realm.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int accessCodeLifespanLogin) {
        realm.setAccessCodeLifespanLogin(accessCodeLifespanLogin);
        em.flush();
    }

    @Override
    public String getPublicKeyPem() {
        return realm.getPublicKeyPem();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        realm.setPublicKeyPem(publicKeyPem);
        em.flush();
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
        em.flush();
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

    protected RequiredCredentialModel initRequiredCredentialModel(String type) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(type);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + type);
        }
        return model;
    }

    @Override
    public void addRequiredCredential(String type) {
        RequiredCredentialModel model = initRequiredCredentialModel(type);
        addRequiredCredential(model);
        em.flush();
    }

    public void addRequiredCredential(RequiredCredentialModel model) {
        RequiredCredentialEntity entity = new RequiredCredentialEntity();
        entity.setRealm(realm);
        entity.setInput(model.isInput());
        entity.setSecret(model.isSecret());
        entity.setType(model.getType());
        entity.setFormLabel(model.getFormLabel());
        em.persist(entity);
        realm.getRequiredCredentials().add(entity);
        em.flush();
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        Collection<RequiredCredentialEntity> relationships = realm.getRequiredCredentials();
        if (relationships == null) relationships = new ArrayList<RequiredCredentialEntity>();

        Set<String> already = new HashSet<String>();
        List<RequiredCredentialEntity> remove = new ArrayList<RequiredCredentialEntity>();
        for (RequiredCredentialEntity rel : relationships) {
            if (!creds.contains(rel.getType())) {
                remove.add(rel);
            } else {
                already.add(rel.getType());
            }
        }
        for (RequiredCredentialEntity entity : remove) {
            relationships.remove(entity);
            em.remove(entity);
        }
        for (String cred : creds) {
            if (!already.contains(cred)) {
                addRequiredCredential(cred);
            }
        }
        em.flush();
    }


    @Override
    public List<RequiredCredentialModel> getRequiredCredentials() {
        List<RequiredCredentialModel> requiredCredentialModels = new ArrayList<RequiredCredentialModel>();
        Collection<RequiredCredentialEntity> entities = realm.getRequiredCredentials();
        if (entities == null) return requiredCredentialModels;
        for (RequiredCredentialEntity entity : entities) {
            RequiredCredentialModel model = new RequiredCredentialModel();
            model.setFormLabel(entity.getFormLabel());
            model.setType(entity.getType());
            model.setSecret(entity.isSecret());
            model.setInput(entity.isInput());
            requiredCredentialModels.add(model);
        }
        return requiredCredentialModels;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public List<String> getDefaultRoles() {
        Collection<RoleEntity> entities = realm.getDefaultRoles();
        List<String> roles = new ArrayList<String>();
        if (entities == null) return roles;
        for (RoleEntity entity : entities) {
            roles.add(entity.getName());
        }
        return roles;
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            role = addRole(name);
        }
        Collection<RoleEntity> entities = realm.getDefaultRoles();
        for (RoleEntity entity : entities) {
            if (entity.getId().equals(role.getId())) {
                return;
            }
        }
        RoleEntity roleEntity = RoleAdapter.toRoleEntity(role, em);
        entities.add(roleEntity);
        em.flush();
    }

    public static boolean contains(String str, String[] array) {
        for (String s : array) {
            if (str.equals(s)) return true;
        }
        return false;
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        Collection<RoleEntity> entities = realm.getDefaultRoles();
        Set<String> already = new HashSet<String>();
        List<RoleEntity> remove = new ArrayList<RoleEntity>();
        for (RoleEntity rel : entities) {
            if (!contains(rel.getName(), defaultRoles)) {
                remove.add(rel);
            } else {
                already.add(rel.getName());
            }
        }
        for (RoleEntity entity : remove) {
            entities.remove(entity);
        }
        em.flush();
        for (String roleName : defaultRoles) {
            if (!already.contains(roleName)) {
                addDefaultRole(roleName);
            }
        }
        em.flush();
    }

    @Override
    public Map<String, ClientModel> getClientNameMap() {
        Map<String, ClientModel> map = new HashMap<String, ClientModel>();
        for (ClientModel app : getClients()) {
            map.put(app.getClientId(), app);
        }
        return map;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ClientModel> getClients() {
        List<ClientModel> list = new ArrayList<ClientModel>();
        if (realm.getClients() == null) return list;
        for (ClientEntity entity : realm.getClients()) {
            list.add(new ClientAdapter(this, em, session, entity));
        }
        return list;
    }

    @Override
    public ClientModel addClient(String name) {
        return this.addClient(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        ClientEntity entity = new ClientEntity();
        entity.setId(id);
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setRealm(realm);
        realm.getClients().add(entity);
        em.persist(entity);
        em.flush();
        final ClientModel resource = new ClientAdapter(this, em, session, entity);
        em.flush();
        session.getKeycloakSessionFactory().publish(new ClientCreationEvent() {
            @Override
            public ClientModel getCreatedClient() {
                return resource;
            }
        });
        return resource;
    }

    @Override
    public boolean removeClient(String id) {
        if (id == null) return false;
        ClientModel client = getClientById(id);
        if (client == null) return false;

        session.users().preRemove(this, client);

        for (RoleModel role : client.getRoles()) {
            client.removeRole(role);
        }

        ClientEntity clientEntity = null;
        Iterator<ClientEntity> it = realm.getClients().iterator();
        while (it.hasNext()) {
            ClientEntity ae = it.next();
            if (ae.getId().equals(id)) {
                clientEntity = ae;
                it.remove();
                break;
            }
        }
        for (ClientEntity a : realm.getClients()) {
            if (a.getId().equals(id)) {
                clientEntity = a;
            }
        }
        if (client == null) {
            return false;
        }
        em.remove(clientEntity);
        em.createNamedQuery("deleteScopeMappingByClient").setParameter("client", clientEntity).executeUpdate();
        em.flush();

        return true;
    }

    @Override
    public ClientModel getClientById(String id) {
        return session.realms().getClientById(id, this);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return getClientNameMap().get(clientId);
    }

    private static final String BROWSER_HEADER_PREFIX = "_browser_header.";

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        Map<String, String> attributes = getAttributes();
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().startsWith(BROWSER_HEADER_PREFIX)) {
                headers.put(entry.getKey().substring(BROWSER_HEADER_PREFIX.length()), entry.getValue());
            }
        }
        return headers;
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            setAttribute(BROWSER_HEADER_PREFIX + entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return realm.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        realm.setSmtpConfig(smtpConfig);
        em.flush();
    }

    @Override
    public List<UserFederationProviderModel> getUserFederationProviders() {
        List<UserFederationProviderEntity> entities = realm.getUserFederationProviders();
        List<UserFederationProviderEntity> copy = new ArrayList<UserFederationProviderEntity>();
        for (UserFederationProviderEntity entity : entities) {
            copy.add(entity);

        }
        Collections.sort(copy, new Comparator<UserFederationProviderEntity>() {

            @Override
            public int compare(UserFederationProviderEntity o1, UserFederationProviderEntity o2) {
                return o1.getPriority() - o2.getPriority();
            }

        });
        List<UserFederationProviderModel> result = new ArrayList<UserFederationProviderModel>();
        for (UserFederationProviderEntity entity : copy) {
            result.add(new UserFederationProviderModel(entity.getId(), entity.getProviderName(), entity.getConfig(), entity.getPriority(), entity.getDisplayName(),
                    entity.getFullSyncPeriod(), entity.getChangedSyncPeriod(), entity.getLastSync()));
        }

        return result;
    }

    @Override
    public UserFederationProviderModel addUserFederationProvider(String providerName, Map<String, String> config, int priority, String displayName, int fullSyncPeriod, int changedSyncPeriod, int lastSync) {
        KeycloakModelUtils.ensureUniqueDisplayName(displayName, null, getUserFederationProviders());

        String id = KeycloakModelUtils.generateId();
        UserFederationProviderEntity entity = new UserFederationProviderEntity();
        entity.setId(id);
        entity.setRealm(realm);
        entity.setProviderName(providerName);
        entity.setConfig(config);
        entity.setPriority(priority);
        if (displayName == null) {
            displayName = id;
        }
        entity.setDisplayName(displayName);
        entity.setFullSyncPeriod(fullSyncPeriod);
        entity.setChangedSyncPeriod(changedSyncPeriod);
        entity.setLastSync(lastSync);
        em.persist(entity);
        realm.getUserFederationProviders().add(entity);
        em.flush();
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

                session.users().preRemove(this, provider);
                removeFederationMappersForProvider(provider.getId());

                it.remove();
                em.remove(entity);
                return;
            }
        }
    }

    private void removeFederationMappersForProvider(String federationProviderId) {
        Set<UserFederationMapperEntity> mappers = getUserFederationMapperEntitiesByFederationProvider(federationProviderId);
        for (UserFederationMapperEntity mapper : mappers) {
            realm.getUserFederationMappers().remove(mapper);
            em.remove(mapper);
        }
    }

    @Override
    public void updateUserFederationProvider(UserFederationProviderModel model) {
        KeycloakModelUtils.ensureUniqueDisplayName(model.getDisplayName(), model, getUserFederationProviders());

        Iterator<UserFederationProviderEntity> it = realm.getUserFederationProviders().iterator();
        while (it.hasNext()) {
            UserFederationProviderEntity entity = it.next();
            if (entity.getId().equals(model.getId())) {
                String displayName = model.getDisplayName();
                if (displayName != null) {
                    entity.setDisplayName(model.getDisplayName());
                }
                entity.setConfig(model.getConfig());
                entity.setPriority(model.getPriority());
                entity.setProviderName(model.getProviderName());
                entity.setPriority(model.getPriority());
                entity.setFullSyncPeriod(model.getFullSyncPeriod());
                entity.setChangedSyncPeriod(model.getChangedSyncPeriod());
                entity.setLastSync(model.getLastSync());
                break;
            }
        }
    }

    @Override
    public void setUserFederationProviders(List<UserFederationProviderModel> providers) {
        for (UserFederationProviderModel currentProvider : providers) {
            KeycloakModelUtils.ensureUniqueDisplayName(currentProvider.getDisplayName(), currentProvider, providers);
        }

        Iterator<UserFederationProviderEntity> it = realm.getUserFederationProviders().iterator();
        while (it.hasNext()) {
            UserFederationProviderEntity entity = it.next();
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

            it.remove();
            em.remove(entity);
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
            entity.setConfig(model.getConfig());
            entity.setPriority(model.getPriority());
            entity.setProviderName(model.getProviderName());
            entity.setPriority(model.getPriority());
            String displayName = model.getDisplayName();
            if (displayName == null) {
                displayName = entity.getId();
            }
            entity.setDisplayName(displayName);
            entity.setFullSyncPeriod(model.getFullSyncPeriod());
            entity.setChangedSyncPeriod(model.getChangedSyncPeriod());
            entity.setLastSync(model.getLastSync());
            em.persist(entity);
            realm.getUserFederationProviders().add(entity);

            session.getKeycloakSessionFactory().publish(new UserFederationProviderCreationEventImpl(this, model));
        }
    }

    protected UserFederationProviderEntity getUserFederationProviderEntityById(String federationProviderId) {
        for (UserFederationProviderEntity entity : realm.getUserFederationProviders()) {
            if (entity.getId().equals(federationProviderId)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public RoleModel getRole(String name) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getRealmRoleByName", RoleEntity.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm);
        List<RoleEntity> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return new RoleAdapter(this, em, roles.get(0));
    }

    @Override
    public RoleModel addRole(String name) {
        return this.addRole(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        RoleEntity entity = new RoleEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setRealm(realm);
        entity.setRealmId(realm.getId());
        realm.getRoles().add(entity);
        em.persist(entity);
        em.flush();
        return new RoleAdapter(this, em, entity);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        if (role == null) {
            return false;
        }
        if (!role.getContainer().equals(this)) return false;
        session.users().preRemove(this, role);
        RoleEntity roleEntity = RoleAdapter.toRoleEntity(role, em);
        realm.getRoles().remove(roleEntity);
        realm.getDefaultRoles().remove(roleEntity);

        em.createNativeQuery("delete from COMPOSITE_ROLE where CHILD_ROLE = :role").setParameter("role", roleEntity).executeUpdate();
        em.createNamedQuery("deleteScopeMappingByRole").setParameter("role", roleEntity).executeUpdate();

        em.remove(roleEntity);

        return true;
    }

    @Override
    public Set<RoleModel> getRoles() {
        Set<RoleModel> list = new HashSet<RoleModel>();
        Collection<RoleEntity> roles = realm.getRoles();
        if (roles == null) return list;
        for (RoleEntity entity : roles) {
            list.add(new RoleAdapter(this, em, entity));
        }
        return list;
    }

    @Override
    public RoleModel getRoleById(String id) {
        return session.realms().getRoleById(id, this);
    }

    @Override
    public boolean removeRoleById(String id) {
        RoleModel role = getRoleById(id);
        if (role == null) return false;
        return role.getContainer().removeRole(role);
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
        em.flush();
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
    public String getLoginTheme() {
        return realm.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        realm.setLoginTheme(name);
        em.flush();
    }

    @Override
    public String getAccountTheme() {
        return realm.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        realm.setAccountTheme(name);
        em.flush();
    }

    @Override
    public String getAdminTheme() {
        return realm.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        realm.setAdminTheme(name);
        em.flush();
    }

    @Override
    public String getEmailTheme() {
        return realm.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        realm.setEmailTheme(name);
        em.flush();
    }

    @Override
    public boolean isEventsEnabled() {
        return realm.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        realm.setEventsEnabled(enabled);
        em.flush();
    }

    @Override
    public long getEventsExpiration() {
        return realm.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        realm.setEventsExpiration(expiration);
        em.flush();
    }

    @Override
    public Set<String> getEventsListeners() {
        return realm.getEventsListeners();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        realm.setEventsListeners(listeners);
        em.flush();
    }
    
    @Override
    public Set<String> getEnabledEventTypes() {
        return realm.getEnabledEventTypes();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        realm.setEnabledEventTypes(enabledEventTypes);
        em.flush();
    }
    
    @Override
    public boolean isAdminEventsEnabled() {
        return realm.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        realm.setAdminEventsEnabled(enabled);
        em.flush();
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        return realm.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        realm.setAdminEventsDetailsEnabled(enabled);
        em.flush();
    }
    
    @Override
    public ClientModel getMasterAdminClient() {
        return new ClientAdapter(this, em, session, realm.getMasterAdminClient());
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        ClientEntity appEntity = client !=null ? em.getReference(ClientEntity.class, client.getId()) : null;
        realm.setMasterAdminClient(appEntity);
        em.flush();
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
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
        entity.setUpdateProfileFirstLoginMode(identityProvider.getUpdateProfileFirstLoginMode());
        entity.setTrustEmail(identityProvider.isTrustEmail());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
        entity.setConfig(identityProvider.getConfig());

        realm.addIdentityProvider(entity);

        em.persist(entity);
        em.flush();
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        for (IdentityProviderEntity entity : realm.getIdentityProviders()) {
            if (entity.getAlias().equals(alias)) {
                em.remove(entity);
                em.flush();
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

        em.flush();
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        return !this.realm.getIdentityProviders().isEmpty();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        return realm.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        realm.setInternationalizationEnabled(enabled);
        em.flush();
    }

    @Override
    public Set<String> getSupportedLocales() {
        return realm.getSupportedLocales();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        realm.setSupportedLocales(locales);
        em.flush();
    }

    @Override
    public String getDefaultLocale() {
        return realm.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        realm.setDefaultLocale(locale);
        em.flush();
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers() {
        Set<IdentityProviderMapperModel> mappings = new HashSet<IdentityProviderMapperModel>();
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
            IdentityProviderMapperModel mapping = entityToModel(entity);
            mappings.add(mapping);
        }
        return mappings;
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
        Set<IdentityProviderMapperModel> mappings = new HashSet<IdentityProviderMapperModel>();
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
        entity.setRealm(this.realm);
        entity.setConfig(model.getConfig());

        em.persist(entity);
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
            em.remove(toDelete);
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
        em.flush();

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
            UserFederationMapperModel mapper = entityToModel(entity);
            mappers.add(mapper);
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
        entity.setFederationProvider(getUserFederationProviderEntityById(model.getFederationProviderId()));
        entity.setFederationMapperType(model.getFederationMapperType());
        entity.setRealm(this.realm);
        entity.setConfig(model.getConfig());

        em.persist(entity);
        this.realm.getUserFederationMappers().add(entity);
        UserFederationMapperModel mapperModel = entityToModel(entity);

        session.getKeycloakSessionFactory().publish(new UserFederationMapperEventImpl(mapperModel, this, session));

        return mapperModel;
    }

    @Override
    public void removeUserFederationMapper(UserFederationMapperModel mapper) {
        UserFederationMapperEntity toDelete = getUserFederationMapperEntity(mapper.getId());
        if (toDelete != null) {
            this.realm.getUserFederationMappers().remove(toDelete);
            em.remove(toDelete);
        }
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
            if (federationProviderId.equals(entity.getFederationProvider().getId()) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    protected Set<UserFederationMapperEntity> getUserFederationMapperEntitiesByFederationProvider(String federationProviderId) {
        Set<UserFederationMapperEntity> mappers = new HashSet<UserFederationMapperEntity>();
        for (UserFederationMapperEntity entity : this.realm.getUserFederationMappers()) {
            if (federationProviderId.equals(entity.getFederationProvider().getId())) {
                mappers.add(entity);
            }
        }
        return mappers;
    }

    @Override
    public void updateUserFederationMapper(UserFederationMapperModel mapper) {
        UserFederationMapperEntity entity = getUserFederationMapperEntity(mapper.getId());
        entity.setFederationProvider(getUserFederationProviderEntityById(mapper.getFederationProviderId()));
        entity.setFederationMapperType(mapper.getFederationMapperType());
        if (entity.getConfig() == null) {
            entity.setConfig(mapper.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapper.getConfig());
        }
        em.flush();

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
        mapper.setFederationProviderId(entity.getFederationProvider().getId());
        mapper.setFederationMapperType(entity.getFederationMapperType());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapper.setConfig(config);
        return mapper;
    }

    @Override
    public List<AuthenticationFlowModel> getAuthenticationFlows() {
        TypedQuery<AuthenticationFlowEntity> query = em.createNamedQuery("getAuthenticationFlowsByRealm", AuthenticationFlowEntity.class);
        query.setParameter("realm", realm);
        List<AuthenticationFlowEntity> flows = query.getResultList();
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
        model.setProviderId(entity.getProviderId());
        model.setDescription(entity.getDescription());
        model.setBuiltIn(entity.isBuiltIn());
        model.setTopLevel(entity.isTopLevel());
        return model;
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        AuthenticationFlowEntity entity = em.find(AuthenticationFlowEntity.class, id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity entity = em.find(AuthenticationFlowEntity.class, model.getId());
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity entity = em.find(AuthenticationFlowEntity.class, model.getId());
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setTopLevel(model.isTopLevel());

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
        entity.setRealm(realm);
        realm.getAuthenticationFlows().add(entity);
        em.persist(entity);
        em.flush();
        model.setId(entity.getId());
        return model;
    }

    @Override
    public List<AuthenticationExecutionModel> getAuthenticationExecutions(String flowId) {
        TypedQuery<AuthenticationExecutionEntity> query = em.createNamedQuery("getAuthenticationExecutionsByFlow", AuthenticationExecutionEntity.class);
        AuthenticationFlowEntity flow = em.getReference(AuthenticationFlowEntity.class, flowId);
        query.setParameter("realm", realm);
        query.setParameter("parentFlow", flow);
        List<AuthenticationExecutionEntity> queryResult = query.getResultList();
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
        model.setParentFlow(entity.getParentFlow().getId());
        model.setAutheticatorFlow(entity.isAutheticatorFlow());
        model.setAuthenticatorConfig(entity.getAuthenticatorConfig());
        return model;
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        AuthenticationExecutionEntity entity = em.find(AuthenticationExecutionEntity.class, id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = new AuthenticationExecutionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setFlowId(model.getFlowId());
        entity.setRequirement(model.getRequirement());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        AuthenticationFlowEntity flow = em.find(AuthenticationFlowEntity.class, model.getParentFlow());
        entity.setParentFlow(flow);
        flow.getExecutions().add(entity);
        entity.setRealm(realm);
        entity.setUserSetupAllowed(model.isUserSetupAllowed());
        entity.setAutheticatorFlow(model.isAutheticatorFlow());
        em.persist(entity);
        em.flush();
        model.setId(entity.getId());
        return model;

    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = em.find(AuthenticationExecutionEntity.class, model.getId());
        if (entity == null) return;
        entity.setAutheticatorFlow(model.isAutheticatorFlow());
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setRequirement(model.getRequirement());
        entity.setUserSetupAllowed(model.isUserSetupAllowed());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        entity.setFlowId(model.getFlowId());
        em.flush();
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = em.find(AuthenticationExecutionEntity.class, model.getId());
        if (entity == null) return;
        em.remove(entity);
        em.flush();

    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity auth = new AuthenticatorConfigEntity();
        auth.setId(KeycloakModelUtils.generateId());
        auth.setAlias(model.getAlias());
        auth.setRealm(realm);
        auth.setConfig(model.getConfig());
        realm.getAuthenticatorConfigs().add(auth);
        em.persist(auth);
        em.flush();
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity entity = em.find(AuthenticatorConfigEntity.class, model.getId());
        if (entity == null) return;
        em.remove(entity);
        em.flush();

    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        AuthenticatorConfigEntity entity = em.find(AuthenticatorConfigEntity.class, id);
        if (entity == null) return null;
        return entityToModel(entity);
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
        AuthenticatorConfigEntity entity = em.find(AuthenticatorConfigEntity.class, model.getId());
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(model.getConfig());
        }
        em.flush();

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
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity auth = new RequiredActionProviderEntity();
        auth.setId(KeycloakModelUtils.generateId());
        auth.setAlias(model.getAlias());
        auth.setName(model.getName());
        auth.setRealm(realm);
        auth.setProviderId(model.getProviderId());
        auth.setConfig(model.getConfig());
        auth.setEnabled(model.isEnabled());
        auth.setDefaultAction(model.isDefaultAction());
        realm.getRequiredActionProviders().add(auth);
        em.persist(auth);
        em.flush();
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = em.find(RequiredActionProviderEntity.class, model.getId());
        if (entity == null) return;
        em.remove(entity);
        em.flush();

    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        RequiredActionProviderEntity entity = em.find(RequiredActionProviderEntity.class, id);
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
        model.setName(entity.getName());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = em.find(RequiredActionProviderEntity.class, model.getId());
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        entity.setProviderId(model.getProviderId());
        entity.setEnabled(model.isEnabled());
        entity.setDefaultAction(model.isDefaultAction());
        entity.setName(model.getName());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(model.getConfig());
        }
        em.flush();

    }

    @Override
    public List<RequiredActionProviderModel> getRequiredActionProviders() {
        List<RequiredActionProviderModel> actions = new LinkedList<>();
        for (RequiredActionProviderEntity entity : realm.getRequiredActionProviders()) {
            actions.add(entityToModel(entity));
        }
        return actions;
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        for (RequiredActionProviderModel action : getRequiredActionProviders()) {
            if (action.getAlias().equals(alias)) return action;
        }
        return null;
    }
}