package org.keycloak.models.jpa;

import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.jpa.entities.ApplicationEntity;
import org.keycloak.models.jpa.entities.ApplicationRoleEntity;
import org.keycloak.models.jpa.entities.AuthenticationLinkEntity;
import org.keycloak.models.jpa.entities.AuthenticationProviderEntity;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.OAuthClientEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RealmRoleEntity;
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
    private PasswordPolicy passwordPolicy;

    public RealmAdapter(EntityManager em, RealmEntity realm) {
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
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByLoginName", UserEntity.class);
        query.setParameter("loginName", name);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return null;
        return new UserAdapter(results.get(0));
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username) {
        String id = username + "-" + realm.getId();
        UsernameLoginFailureEntity entity = em.find(UsernameLoginFailureEntity.class, id);
        if (entity == null) return null;
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username) {
        UsernameLoginFailureModel model = getUserLoginFailure(username);
        if (model != null) return model;
        String id = username + "-" + realm.getId();
        UsernameLoginFailureEntity entity = new UsernameLoginFailureEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setRealm(realm);
        em.persist(entity);
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures() {
        TypedQuery<UsernameLoginFailureEntity> query = em.createNamedQuery("getAllFailures", UsernameLoginFailureEntity.class);
        List<UsernameLoginFailureEntity> entities = query.getResultList();
        List<UsernameLoginFailureModel> models = new ArrayList<UsernameLoginFailureModel>();
        for (UsernameLoginFailureEntity entity : entities) {
            models.add(new UsernameLoginFailureAdapter(entity));
        }
        return models;
    }

    @Override
    public UserModel getUserByEmail(String email) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        return results.isEmpty() ? null : new UserAdapter(results.get(0));
    }

    @Override
    public UserModel getUserById(String id) {
        UserEntity entity = em.find(UserEntity.class, id);

        // Check if user belongs to this realm
        if (entity == null || !this.realm.equals(entity.getRealm())) return null;
        return new UserAdapter(entity);
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
        UserModel userModel = new UserAdapter(entity);

        for (String r : getDefaultRoles()) {
            grantRole(userModel, getRole(r));
        }

        for (ApplicationModel application : getApplications()) {
            for (String r : application.getDefaultRoles()) {
                grantRole(userModel, application.getRole(r));
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
        removeUserSessions(user);

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
        ApplicationEntity app = em.find(ApplicationEntity.class, id);

        // Check if application belongs to this realm
        if (app == null || !this.realm.equals(app.getRealm())) return null;
        return new ApplicationAdapter(this, em, app);
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        return getApplicationNameMap().get(name);
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        TypedQuery<UserEntity> query = em.createNamedQuery("findUserByLinkAndRealm", UserEntity.class);
        query.setParameter("realm", realm);
        query.setParameter("socialProvider", socialLink.getSocialProvider());
        query.setParameter("socialUserId", socialLink.getSocialUserId());
        List<UserEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More results found for socialProvider=" + socialLink.getSocialProvider() +
                    ", socialUserId=" + socialLink.getSocialUserId() + ", results=" + results);
        } else {
            UserEntity user = results.get(0);
            return new UserAdapter(user);
        }
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user) {
        TypedQuery<SocialLinkEntity> query = em.createNamedQuery("findSocialLinkByUser", SocialLinkEntity.class);
        query.setParameter("user", ((UserAdapter) user).getUser());
        List<SocialLinkEntity> results = query.getResultList();
        Set<SocialLinkModel> set = new HashSet<SocialLinkModel>();
        for (SocialLinkEntity entity : results) {
            set.add(new SocialLinkModel(entity.getSocialProvider(), entity.getSocialUserId(), entity.getSocialUsername()));
        }
        return set;
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider) {
        SocialLinkEntity entity = findSocialLink(user, socialProvider);
        return (entity != null) ? new SocialLinkModel(entity.getSocialProvider(), entity.getSocialUserId(), entity.getSocialUsername()) : null;
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

    private SocialLinkEntity findSocialLink(UserModel user, String socialProvider) {
        TypedQuery<SocialLinkEntity> query = em.createNamedQuery("findSocialLinkByUserAndProvider", SocialLinkEntity.class);
        query.setParameter("user", ((UserAdapter) user).getUser());
        query.setParameter("socialProvider", socialProvider);
        List<SocialLinkEntity> results = query.getResultList();
        return results.size() > 0 ? results.get(0) : null;
    }

    @Override
    public AuthenticationLinkModel getAuthenticationLink(UserModel user) {
        UserEntity userEntity = ((UserAdapter) user).getUser();
        AuthenticationLinkEntity authLinkEntity = userEntity.getAuthenticationLink();
        return authLinkEntity == null ? null : new AuthenticationLinkModel(authLinkEntity.getAuthProvider(), authLinkEntity.getAuthUserId());
    }

    @Override
    public void setAuthenticationLink(UserModel user, AuthenticationLinkModel authenticationLink) {
        AuthenticationLinkEntity entity = new AuthenticationLinkEntity();
        entity.setAuthProvider(authenticationLink.getAuthProvider());
        entity.setAuthUserId(authenticationLink.getAuthUserId());

        UserEntity userEntity = ((UserAdapter) user).getUser();
        userEntity.setAuthenticationLink(entity);
        em.persist(entity);
        em.persist(userEntity);
        em.flush();
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
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm", UserEntity.class);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search) {
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm and ( lower(u.loginName) like :search or lower(concat(u.firstName, ' ', u.lastName)) like :search or u.email like :search )", UserEntity.class);
        query.setParameter("realm", realm);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes) {
        StringBuilder builder = new StringBuilder("select u from UserEntity u");
        boolean first = true;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attribute = null;
            if (entry.getKey().equals(UserModel.LOGIN_NAME)) {
                attribute = "lower(loginName)";
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                attribute = "lower(firstName)";
            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                attribute = "lower(lastName)";
            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                attribute = "lower(email)";
            }
            if (attribute == null) continue;
            if (first) {
                first = false;
                builder.append(" where ");
            } else {
                builder.append(" and ");
            }
            builder.append(attribute).append(" like '%").append(entry.getValue().toLowerCase()).append("%'");
        }
        String q = builder.toString();
        TypedQuery<UserEntity> query = em.createQuery(q, UserEntity.class);
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(entity));
        return users;
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
        OAuthClientEntity client = em.find(OAuthClientEntity.class, id);

        // Check if client belongs to this realm
        if (client == null || !this.realm.getId().equals(client.getRealm().getId())) return null;
        return new OAuthClientAdapter(this, client, em);
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
        TypedQuery<RealmRoleEntity> query = em.createNamedQuery("getRealmRoleByName", RealmRoleEntity.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm);
        List<RealmRoleEntity> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return new RoleAdapter(this, em, roles.get(0));
    }

    @Override
    public RoleModel addRole(String name) {
        return this.addRole(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        RealmRoleEntity entity = new RealmRoleEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setRealm(realm);
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
        Collection<RealmRoleEntity> roles = realm.getRoles();
        if (roles == null) return list;
        for (RoleEntity entity : roles) {
            list.add(new RoleAdapter(this, em, entity));
        }
        return list;
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (entity instanceof RealmRoleEntity) {
            RealmRoleEntity roleEntity = (RealmRoleEntity) entity;
            if (!roleEntity.getRealm().getId().equals(getId())) return null;
        } else {
            ApplicationRoleEntity roleEntity = (ApplicationRoleEntity) entity;
            if (!roleEntity.getApplication().getRealm().getId().equals(getId())) return null;
        }
        return new RoleAdapter(this, em, entity);
    }

    @Override
    public boolean removeRoleById(String id) {
        RoleModel role = getRoleById(id);
        if (role == null) return false;
        return role.getContainer().removeRole(role);
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        Set<RoleModel> roles = getRoleMappings(user);
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public boolean hasScope(ClientModel client, RoleModel role) {
        Set<RoleModel> roles = getScopeMappings(client);
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }


    protected TypedQuery<UserRoleMappingEntity> getUserRoleMappingEntityTypedQuery(UserAdapter user, RoleAdapter role) {
        TypedQuery<UserRoleMappingEntity> query = em.createNamedQuery("userHasRole", UserRoleMappingEntity.class);
        query.setParameter("user", user.getUser());
        query.setParameter("role", role.getRole());
        return query;
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        if (hasRole(user, role)) return;
        UserRoleMappingEntity entity = new UserRoleMappingEntity();
        entity.setUser(((UserAdapter) user).getUser());
        entity.setRole(((RoleAdapter) role).getRole());
        em.persist(entity);
        em.flush();
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings(UserModel user) {
        Set<RoleModel> roleMappings = getRoleMappings(user);

        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }


    @Override
    public Set<RoleModel> getRoleMappings(UserModel user) {
        TypedQuery<UserRoleMappingEntity> query = em.createNamedQuery("userRoleMappings", UserRoleMappingEntity.class);
        query.setParameter("user", ((UserAdapter) user).getUser());
        List<UserRoleMappingEntity> entities = query.getResultList();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (UserRoleMappingEntity entity : entities) {
            roles.add(new RoleAdapter(this, em, entity.getRole()));
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        if (user == null || role == null) return;

        TypedQuery<UserRoleMappingEntity> query = getUserRoleMappingEntityTypedQuery((UserAdapter) user, (RoleAdapter) role);
        List<UserRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (UserRoleMappingEntity entity : results) {
            em.remove(entity);
        }
        em.flush();
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings(ClientModel client) {
        Set<RoleModel> roleMappings = getScopeMappings(client);

        Set<RoleModel> appRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                if (((RealmModel) container).getId().equals(getId())) {
                    appRoles.add(role);
                }
            }
        }

        return appRoles;
    }


    @Override
    public Set<RoleModel> getScopeMappings(ClientModel client) {
        TypedQuery<ScopeMappingEntity> query = em.createNamedQuery("clientScopeMappings", ScopeMappingEntity.class);
        query.setParameter("client", ((ClientAdapter) client).getEntity());
        List<ScopeMappingEntity> entities = query.getResultList();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (ScopeMappingEntity entity : entities) {
            roles.add(new RoleAdapter(this, em, entity.getRole()));
        }
        return roles;
    }

    @Override
    public void addScopeMapping(ClientModel client, RoleModel role) {
        if (hasScope(client, role)) return;
        ScopeMappingEntity entity = new ScopeMappingEntity();
        entity.setClient(((ClientAdapter) client).getEntity());
        entity.setRole(((RoleAdapter) role).getRole());
        em.persist(entity);
    }

    @Override
    public void deleteScopeMapping(ClientModel client, RoleModel role) {
        TypedQuery<ScopeMappingEntity> query = getRealmScopeMappingQuery((ClientAdapter) client, (RoleAdapter) role);
        List<ScopeMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (ScopeMappingEntity entity : results) {
            em.remove(entity);
        }
    }

    protected TypedQuery<ScopeMappingEntity> getRealmScopeMappingQuery(ClientAdapter client, RoleAdapter role) {
        TypedQuery<ScopeMappingEntity> query = em.createNamedQuery("hasScope", ScopeMappingEntity.class);
        query.setParameter("client", client.getEntity());
        query.setParameter("role", ((RoleAdapter) role).getRole());
        return query;
    }

    @Override
    public boolean validatePassword(UserModel user, String password) {
        for (CredentialEntity cred : ((UserAdapter) user).getUser().getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return new Pbkdf2PasswordEncoder(cred.getSalt()).verify(password, cred.getValue());
            }
        }
        return false;
    }

    @Override
    public boolean validateTOTP(UserModel user, String password, String token) {
        if (!validatePassword(user, password)) return false;
        for (CredentialEntity cred : ((UserAdapter) user).getUser().getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.TOTP)) {
                return new TimeBasedOTP().validate(token, cred.getValue().getBytes());
            }
        }
        return false;
    }

    @Override
    public void updateCredential(UserModel user, UserCredentialModel cred) {
        UserEntity userEntity = ((UserAdapter) user).getUser();
        CredentialEntity credentialEntity = getCredentialEntity(userEntity, cred.getType());

        if (credentialEntity == null) {
            credentialEntity = new CredentialEntity();
            credentialEntity.setType(cred.getType());
            credentialEntity.setDevice(cred.getDevice());
            credentialEntity.setUser(userEntity);
            em.persist(credentialEntity);
            userEntity.getCredentials().add(credentialEntity);
        }
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            byte[] salt = getSalt();
            credentialEntity.setValue(new Pbkdf2PasswordEncoder(salt).encode(cred.getValue()));
            credentialEntity.setSalt(salt);
        } else {
            credentialEntity.setValue(cred.getValue());
        }
        credentialEntity.setDevice(cred.getDevice());
        em.flush();
    }

    private CredentialEntity getCredentialEntity(UserEntity userEntity, String credType) {
        for (CredentialEntity entity : userEntity.getCredentials()) {
            if (entity.getType().equals(credType)) {
                return entity;
            }
        }

        return null;
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly(UserModel user) {
        UserEntity userEntity = ((UserAdapter) user).getUser();
        List<CredentialEntity> credentials = new ArrayList<CredentialEntity>(userEntity.getCredentials());
        List<UserCredentialValueModel> result = new ArrayList<UserCredentialValueModel>();

        if (credentials != null) {
            for (CredentialEntity credEntity : credentials) {
                UserCredentialValueModel credModel = new UserCredentialValueModel();
                credModel.setType(credEntity.getType());
                credModel.setDevice(credEntity.getDevice());
                credModel.setValue(credEntity.getValue());
                credModel.setSalt(credEntity.getSalt());

                result.add(credModel);
            }
        }

        return result;
    }

    @Override
    public void updateCredentialDirectly(UserModel user, UserCredentialValueModel credModel) {
        UserEntity userEntity = ((UserAdapter) user).getUser();
        CredentialEntity credentialEntity = getCredentialEntity(userEntity, credModel.getType());

        if (credentialEntity == null) {
            credentialEntity = new CredentialEntity();
            credentialEntity.setType(credModel.getType());
            credentialEntity.setUser(userEntity);
            em.persist(credentialEntity);
            userEntity.getCredentials().add(credentialEntity);
        }

        credentialEntity.setValue(credModel.getValue());
        credentialEntity.setSalt(credModel.getSalt());
        credentialEntity.setDevice(credModel.getDevice());

        em.flush();
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
        UserSessionEntity entity = new UserSessionEntity();
        entity.setRealmId(realm.getId());
        entity.setUserId(user.getId());
        entity.setIpAddress(ipAddress);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        em.persist(entity);
        return new UserSessionAdapter(em, this, entity);
    }

    @Override
    public UserSessionModel getUserSession(String id) {
        UserSessionEntity entity = em.find(UserSessionEntity.class, id);
        return entity != null ? new UserSessionAdapter(em, this, entity) : null;
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user) {
        List<UserSessionModel> sessions = new LinkedList<UserSessionModel>();
        for (UserSessionEntity e : em.createNamedQuery("getUserSessionByUser", UserSessionEntity.class).setParameter("userId", user.getId()).getResultList()) {
            sessions.add(new UserSessionAdapter(em, this, e));
        }
        return sessions;
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        em.remove(((UserSessionAdapter) session).getEntity());
    }

    @Override
    public void removeUserSessions() {
        em.createNamedQuery("removeClientUserSessionByRealm").setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("removeRealmUserSessions").setParameter("realmId", realm.getId()).executeUpdate();

    }

    @Override
    public void removeUserSessions(UserModel user) {
        removeUserSessions(((UserAdapter) user).getUser());
    }

    private void removeUserSessions(UserEntity user) {
        em.createNamedQuery("removeClientUserSessionByUser").setParameter("userId", user.getId()).executeUpdate();
        em.createNamedQuery("removeUserSessionByUser").setParameter("userId", user.getId()).executeUpdate();
    }

    @Override
    public void removeExpiredUserSessions() {
        TypedQuery<UserSessionEntity> query = em.createNamedQuery("getUserSessionExpired", UserSessionEntity.class)
                .setParameter("maxTime", Time.currentTime() - getSsoSessionMaxLifespan())
                .setParameter("idleTime", Time.currentTime() - getSsoSessionIdleTimeout());
        List<UserSessionEntity> results = query.getResultList();
        for (UserSessionEntity entity : results) {
            em.remove(entity);
        }
    }

}
