package org.keycloak.models.jpa;

import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.jpa.entities.ApplicationEntity;
import org.keycloak.models.jpa.entities.AuthenticationLinkEntity;
import org.keycloak.models.jpa.entities.AuthenticationProviderEntity;
import org.keycloak.models.jpa.entities.OAuthClientEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RequiredCredentialEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.ScopeMappingEntity;
import org.keycloak.models.jpa.entities.SocialLinkEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserSessionEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.models.jpa.entities.UsernameLoginFailureEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.util.Time;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.security.PrivateKey;
import java.security.PublicKey;
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

import static org.keycloak.models.utils.Pbkdf2PasswordEncoder.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdapter implements RealmModel {
    protected RealmEntity realm;
    protected EntityManager em;
    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;
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
    public boolean isSslNotRequired() {
        return realm.isSslNotRequired();
    }

    @Override
    public void setSslNotRequired(boolean sslNotRequired) {
        realm.setSslNotRequired(sslNotRequired);
        em.flush();
    }

    @Override
    public boolean isPasswordCredentialGrantAllowed() {
        return realm.isPasswordCredentialGrantAllowed();
    }

    @Override
    public void setPasswordCredentialGrantAllowed(boolean passwordCredentialGrantAllowed) {
        realm.setPasswordCredentialGrantAllowed(passwordCredentialGrantAllowed);
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
    public boolean isRememberMe() {
        return realm.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        realm.setRememberMe(rememberMe);
        em.flush();
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
    public String getPublicKeyPem() {
        return realm.getPublicKeyPem();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        realm.setPublicKeyPem(publicKeyPem);
        em.flush();
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
    public UserModel getUser(String name) {
        return session.getUserByUsername(name, this);
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username) {
        return session.getUserLoginFailure(username, this);
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username) {
        return session.addUserLoginFailure(username, this);
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures() {
        return session.getAllUserLoginFailures();
    }

    @Override
    public UserModel getUserByEmail(String email) {
        return session.getUserByEmail(email, this);
    }

    @Override
    public UserModel getUserById(String id) {
        return session.getUserById(id, this);
    }

    @Override
    public UserModel addUser(String username) {
        return this.addUser(KeycloakModelUtils.generateId(), username);
    }

    @Override
    public UserModel addUser(String id, String username) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setLoginName(username);
        entity.setRealm(realm);
        em.persist(entity);
        em.flush();
        UserModel userModel = new UserAdapter(this, em, entity);

        for (String r : getDefaultRoles()) {
            userModel.grantRole(getRole(r));
        }

        for (ApplicationModel application : getApplications()) {
            for (String r : application.getDefaultRoles()) {
                userModel.grantRole(application.getRole(r));
            }
        }

        return userModel;
    }

    @Override
    public boolean removeUser(String name) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByLoginName", UserEntity.class);
        query.setParameter("loginName", name);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return false;
        removeUser(results.get(0));
        return true;
    }

    private void removeUser(UserEntity user) {
        em.createNamedQuery("removeClientUserSessionByUser").setParameter("userId", user.getId()).executeUpdate();
        em.createNamedQuery("removeUserSessionByUser").setParameter("userId", user.getId()).executeUpdate();

        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where user = :user").setParameter("user", user).executeUpdate();
        em.createQuery("delete from " + SocialLinkEntity.class.getSimpleName() + " where user = :user").setParameter("user", user).executeUpdate();
        if (user.getAuthenticationLink() != null) {
            em.remove(user.getAuthenticationLink());
        }
        em.remove(user);
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
        entities.add(((RoleAdapter) role).getRole());
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
    public Map<String, ApplicationModel> getApplicationNameMap() {
        Map<String, ApplicationModel> map = new HashMap<String, ApplicationModel>();
        for (ApplicationModel app : getApplications()) {
            map.put(app.getName(), app);
        }
        return map;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ApplicationModel> getApplications() {
        List<ApplicationModel> list = new ArrayList<ApplicationModel>();
        if (realm.getApplications() == null) return list;
        for (ApplicationEntity entity : realm.getApplications()) {
            list.add(new ApplicationAdapter(this, em, entity));
        }
        return list;
    }

    @Override
    public ApplicationModel addApplication(String name) {
        return this.addApplication(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public ApplicationModel addApplication(String id, String name) {
        ApplicationEntity applicationData = new ApplicationEntity();
        applicationData.setId(id);
        applicationData.setName(name);
        applicationData.setEnabled(true);
        applicationData.setRealm(realm);
        realm.getApplications().add(applicationData);
        em.persist(applicationData);
        em.flush();
        ApplicationModel resource = new ApplicationAdapter(this, em, applicationData);
        em.flush();
        return resource;
    }

    @Override
    public boolean removeApplication(String id) {
        if (id == null) return false;
        ApplicationModel application = getApplicationById(id);
        if (application == null) return false;

        ((ApplicationAdapter)application).deleteUserSessionAssociation();
        for (RoleModel role : application.getRoles()) {
            application.removeRole(role);
        }

        ApplicationEntity applicationEntity = null;
        Iterator<ApplicationEntity> it = realm.getApplications().iterator();
        while (it.hasNext()) {
            ApplicationEntity ae = it.next();
            if (ae.getId().equals(id)) {
                applicationEntity = ae;
                it.remove();
                break;
            }
        }
        for (ApplicationEntity a : realm.getApplications()) {
            if (a.getId().equals(id)) {
                applicationEntity = a;
            }
        }
        if (application == null) {
            return false;
        }
        em.remove(applicationEntity);
        em.createQuery("delete from " + ScopeMappingEntity.class.getSimpleName() + " where client = :client").setParameter("client", applicationEntity).executeUpdate();

        return true;
    }

    @Override
    public ApplicationModel getApplicationById(String id) {
        return session.getApplicationById(id, this);
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        return getApplicationNameMap().get(name);
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        return session.getUserBySocialLink(socialLink, this);
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user) {
        return session.getSocialLinks(user, this);
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider) {
        return session.getSocialLink(user, socialProvider, this);
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        SocialLinkEntity entity = new SocialLinkEntity();
        entity.setRealm(realm);
        entity.setSocialProvider(socialLink.getSocialProvider());
        entity.setSocialUserId(socialLink.getSocialUserId());
        entity.setSocialUsername(socialLink.getSocialUsername());
        entity.setUser(((UserAdapter) user).getUser());
        em.persist(entity);
        em.flush();
    }

    private SocialLinkEntity findSocialLink(UserModel user, String socialProvider) {
        TypedQuery<SocialLinkEntity> query = em.createNamedQuery("findSocialLinkByUserAndProvider", SocialLinkEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        query.setParameter("socialProvider", socialProvider);
        List<SocialLinkEntity> results = query.getResultList();
        return results.size() > 0 ? results.get(0) : null;
    }


    @Override
    public boolean removeSocialLink(UserModel user, String socialProvider) {
        SocialLinkEntity entity = findSocialLink(user, socialProvider);
        if (entity != null) {
            em.remove(entity);
            em.flush();
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean isSocial() {
        return realm.isSocial();
    }

    @Override
    public void setSocial(boolean social) {
        realm.setSocial(social);
        em.flush();
    }

    @Override
    public boolean isUpdateProfileOnInitialSocialLogin() {
        return realm.isUpdateProfileOnInitialSocialLogin();
    }

    @Override
    public void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin) {
        realm.setUpdateProfileOnInitialSocialLogin(updateProfileOnInitialSocialLogin);
        em.flush();
    }

    @Override
    public List<UserModel> getUsers() {
        return session.getUsers(this);
    }

    @Override
    public List<UserModel> searchForUser(String search) {
        return session.searchForUser(search, this);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes) {
        return session.searchForUserByAttributes(attributes, this);
    }

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        return this.addOAuthClient(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public OAuthClientModel addOAuthClient(String id, String name) {
        OAuthClientEntity data = new OAuthClientEntity();
        data.setId(id);
        data.setEnabled(true);
        data.setName(name);
        data.setRealm(realm);
        em.persist(data);
        em.flush();
        return new OAuthClientAdapter(this, data, em);
    }

    @Override
    public boolean removeOAuthClient(String id) {
        OAuthClientModel oauth = getOAuthClientById(id);
        if (oauth == null) return false;
        ((OAuthClientAdapter)oauth).deleteUserSessionAssociation();
        OAuthClientEntity client = (OAuthClientEntity) ((OAuthClientAdapter) oauth).getEntity();
        em.createQuery("delete from " + ScopeMappingEntity.class.getSimpleName() + " where client = :client").setParameter("client", client).executeUpdate();
        em.remove(client);
        return true;
    }


    @Override
    public OAuthClientModel getOAuthClient(String name) {
        TypedQuery<OAuthClientEntity> query = em.createNamedQuery("findOAuthClientByName", OAuthClientEntity.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm);
        List<OAuthClientEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        return new OAuthClientAdapter(this, entities.get(0), em);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        return session.getOAuthClientById(id, this);
    }


    @Override
    public List<OAuthClientModel> getOAuthClients() {
        TypedQuery<OAuthClientEntity> query = em.createNamedQuery("findOAuthClientByRealm", OAuthClientEntity.class);
        query.setParameter("realm", realm);
        List<OAuthClientEntity> entities = query.getResultList();
        List<OAuthClientModel> list = new ArrayList<OAuthClientModel>();
        for (OAuthClientEntity entity : entities) list.add(new OAuthClientAdapter(this, entity, em));
        return list;
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
    public Map<String, String> getSocialConfig() {
        return realm.getSocialConfig();
    }

    @Override
    public void setSocialConfig(Map<String, String> socialConfig) {
        realm.setSocialConfig(socialConfig);
        em.flush();
    }

    @Override
    public Map<String, String> getLdapServerConfig() {
        return realm.getLdapServerConfig();
    }

    @Override
    public void setLdapServerConfig(Map<String, String> ldapServerConfig) {
        realm.setLdapServerConfig(ldapServerConfig);
        em.flush();
    }

    @Override
    public List<AuthenticationProviderModel> getAuthenticationProviders() {
        List<AuthenticationProviderEntity> entities = realm.getAuthenticationProviders();
        List<AuthenticationProviderEntity> copy = new ArrayList<AuthenticationProviderEntity>();
        for (AuthenticationProviderEntity entity : entities) {
            copy.add(entity);

        }
        Collections.sort(copy, new Comparator<AuthenticationProviderEntity>() {

            @Override
            public int compare(AuthenticationProviderEntity o1, AuthenticationProviderEntity o2) {
                return o1.getPriority() - o2.getPriority();
            }

        });
        List<AuthenticationProviderModel> result = new ArrayList<AuthenticationProviderModel>();
        for (AuthenticationProviderEntity entity : copy) {
            result.add(new AuthenticationProviderModel(entity.getProviderName(), entity.isPasswordUpdateSupported(), entity.getConfig()));
        }

        return result;
    }

    @Override
    public void setAuthenticationProviders(List<AuthenticationProviderModel> authenticationProviders) {
        List<AuthenticationProviderEntity> newEntities = new ArrayList<AuthenticationProviderEntity>();
        int counter = 1;
        for (AuthenticationProviderModel model : authenticationProviders) {
            AuthenticationProviderEntity entity = new AuthenticationProviderEntity();
            entity.setProviderName(model.getProviderName());
            entity.setPasswordUpdateSupported(model.isPasswordUpdateSupported());
            entity.setConfig(model.getConfig());
            entity.setPriority(counter++);
            newEntities.add(entity);
        }

        // Remove all existing first
        Collection<AuthenticationProviderEntity> existing = realm.getAuthenticationProviders();
        Collection<AuthenticationProviderEntity> copy = new ArrayList<AuthenticationProviderEntity>(existing);
        for (AuthenticationProviderEntity apToRemove : copy) {
            existing.remove(apToRemove);
            em.remove(apToRemove);
        }

        // Now create all new providers
        for (AuthenticationProviderEntity apToAdd : newEntities) {
            existing.add(apToAdd);
            em.persist(apToAdd);
        }

        em.flush();
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

        RoleEntity roleEntity = ((RoleAdapter) role).getRole();
        realm.getRoles().remove(role);
        realm.getDefaultRoles().remove(role);

        em.createNativeQuery("delete from CompositeRole where role = :role").setParameter("role", roleEntity).executeUpdate();
        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", roleEntity).executeUpdate();
        em.createQuery("delete from " + ScopeMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", roleEntity).executeUpdate();

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
        return session.getRoleById(id, this);
    }

    @Override
    public boolean removeRoleById(String id) {
        RoleModel role = getRoleById(id);
        if (role == null) return false;
        return role.getContainer().removeRole(role);
    }





    @Override
    public boolean validatePassword(UserModel user, String password) {
        for (UserCredentialValueModel cred : user.getCredentialsDirectly()) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return new Pbkdf2PasswordEncoder(cred.getSalt()).verify(password, cred.getValue());

            }
        }
        return false;
    }

    @Override
    public boolean validateTOTP(UserModel user, String password, String token) {
        if (!validatePassword(user, password)) return false;
        for (UserCredentialValueModel cred : user.getCredentialsDirectly()) {
            if (cred.getType().equals(UserCredentialModel.TOTP)) {
                return new TimeBasedOTP().validate(token, cred.getValue().getBytes());
            }
        }
        return false;
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

    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof RealmAdapter)) return false;
        RealmAdapter r = (RealmAdapter) o;
        return r.getId().equals(getId());
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
    public boolean isAuditEnabled() {
        return realm.isAuditEnabled();
    }

    @Override
    public void setAuditEnabled(boolean enabled) {
        realm.setAuditEnabled(enabled);
        em.flush();
    }

    @Override
    public long getAuditExpiration() {
        return realm.getAuditExpiration();
    }

    @Override
    public void setAuditExpiration(long expiration) {
        realm.setAuditExpiration(expiration);
        em.flush();
    }

    @Override
    public Set<String> getAuditListeners() {
        return realm.getAuditListeners();
    }

    @Override
    public void setAuditListeners(Set<String> listeners) {
        realm.setAuditListeners(listeners);
        em.flush();
    }

    @Override
    public ApplicationModel getMasterAdminApp() {
        return new ApplicationAdapter(this, em, realm.getMasterAdminApp());
    }

    @Override
    public void setMasterAdminApp(ApplicationModel app) {
        realm.setMasterAdminApp(((ApplicationAdapter) app).getJpaEntity());
        em.flush();
    }

    @Override
    public UserSessionModel createUserSession(UserModel user, String ipAddress) {
        return session.createUserSession(this, user, ipAddress);
    }

    @Override
    public UserSessionModel getUserSession(String id) {
        return session.getUserSession(id, this);
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user) {
        return session.getUserSessions(user, this);
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        this.session.removeUserSession(session);
    }

    @Override
    public void removeUserSessions() {
        session.removeUserSessions(this);

    }

    @Override
    public void removeUserSessions(UserModel user) {
        session.removeUserSessions(this, user);
    }

    @Override
    public void removeExpiredUserSessions() {
        session.removeExpiredUserSessions(this);
    }

}
