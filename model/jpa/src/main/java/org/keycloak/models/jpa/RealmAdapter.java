package org.keycloak.models.jpa;

import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.jpa.entities.ApplicationEntity;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.OAuthClientEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RealmRoleEntity;
import org.keycloak.models.jpa.entities.RequiredCredentialEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.SocialLinkEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.models.jpa.entities.UserScopeMappingEntity;
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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
        em.flush();
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
    public int getTokenLifespan() {
        return realm.getTokenLifespan();
    }

    @Override
    public void setTokenLifespan(int tokenLifespan) {
        realm.setTokenLifespan(tokenLifespan);
        em.flush();
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
    public List<RequiredCredentialModel> getRequiredApplicationCredentials() {
        List<RequiredCredentialModel> requiredCredentialModels = new ArrayList<RequiredCredentialModel>();
        Collection<RequiredCredentialEntity> entities = realm.getRequiredAppCredentials();
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
    public List<RequiredCredentialModel> getRequiredOAuthClientCredentials() {
        List<RequiredCredentialModel> requiredCredentialModels = new ArrayList<RequiredCredentialModel>();
        Collection<RequiredCredentialEntity> entities = realm.getRequiredOAuthClCredentials();
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

    public void addRequiredOAuthClientCredential(RequiredCredentialModel model) {
        RequiredCredentialEntity entity = new RequiredCredentialEntity();
        entity.setInput(model.isInput());
        entity.setSecret(model.isSecret());
        entity.setType(model.getType());
        entity.setFormLabel(model.getFormLabel());
        em.persist(entity);
        realm.getRequiredOAuthClCredentials().add(entity);
        em.flush();
    }

    @Override
    public void addRequiredOAuthClientCredential(String type) {
        RequiredCredentialModel model = initRequiredCredentialModel(type);
        addRequiredOAuthClientCredential(model);
        em.flush();
    }

    public void addRequiredResourceCredential(RequiredCredentialModel model) {
        RequiredCredentialEntity entity = new RequiredCredentialEntity();
        entity.setInput(model.isInput());
        entity.setSecret(model.isSecret());
        entity.setType(model.getType());
        entity.setFormLabel(model.getFormLabel());
        em.persist(entity);
        realm.getRequiredAppCredentials().add(entity);
        em.flush();
    }

    @Override
    public void addRequiredResourceCredential(String type) {
        RequiredCredentialModel model = initRequiredCredentialModel(type);
        addRequiredResourceCredential(model);
        em.flush();
    }

    @Override
    public void updateRequiredOAuthClientCredentials(Set<String> creds) {
        Collection<RequiredCredentialEntity> relationships = realm.getRequiredOAuthClCredentials();
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
                addRequiredOAuthClientCredential(cred);
            }
        }
        em.flush();
    }

    @Override
    public void updateRequiredApplicationCredentials(Set<String> creds) {
        Collection<RequiredCredentialEntity> relationships = realm.getRequiredAppCredentials();
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
                addRequiredResourceCredential(cred);
            }
        }
        em.flush();
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
    public UserModel getUserByEmail(String email) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        return results.isEmpty()? null : new UserAdapter(results.get(0));
    }

    @Override
    public UserModel getUserById(String id) {
        return new UserAdapter(em.find(UserEntity.class, id));
    }

    @Override
    public UserModel addUser(String username) {
        UserEntity entity = new UserEntity();
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
        em.createQuery("delete from " + UserScopeMappingEntity.class.getSimpleName() + " where user = :user").setParameter("user", user).executeUpdate();
        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where user = :user").setParameter("user", user).executeUpdate();
        em.createQuery("delete from " + SocialLinkEntity.class.getSimpleName() + " where user = :user").setParameter("user", user).executeUpdate();
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
        ApplicationEntity applicationData = new ApplicationEntity();
        UserEntity user = new UserEntity();
        user.setLoginName(name);
        user.setRealm(realm);
        user.setEnabled(true);
        em.persist(user);
        applicationData.setApplicationUser(user);
        applicationData.setName(name);
        applicationData.setEnabled(true);
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

        for (RoleModel role : application.getRoles()) {
            application.removeRoleById(role.getId());
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
        removeUser(applicationEntity.getApplicationUser());
        return true;
    }

    @Override
    public ApplicationModel getApplicationById(String id) {
        ApplicationEntity app = em.find(ApplicationEntity.class, id);
        if (app == null) return null;
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
        query.setParameter("socialUsername", socialLink.getSocialUsername());
        List<UserEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More results found for socialProvider=" + socialLink.getSocialProvider() +
                    ", socialUsername=" + socialLink.getSocialUsername() + ", results=" + results);
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
            set.add(new SocialLinkModel(entity.getSocialProvider(), entity.getSocialUsername()));
        }
        return set;
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        SocialLinkEntity entity = new SocialLinkEntity();
        entity.setRealm(realm);
        entity.setSocialProvider(socialLink.getSocialProvider());
        entity.setSocialUsername(socialLink.getSocialUsername());
        entity.setUser(((UserAdapter) user).getUser());
        em.persist(entity);
        em.flush();
    }

    @Override
    public void removeSocialLink(UserModel user, SocialLinkModel socialLink) {
        TypedQuery<SocialLinkEntity> query = em.createNamedQuery("findSocialLinkByAll", SocialLinkEntity.class);
        query.setParameter("realm", realm);
        query.setParameter("user", ((UserAdapter) user).getUser());
        query.setParameter("socialProvider", socialLink.getSocialProvider());
        query.setParameter("socialUsername", socialLink.getSocialUsername());
        List<SocialLinkEntity> results = query.getResultList();
        for (SocialLinkEntity entity : results) em.remove(entity);
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
        OAuthClientEntity data = new OAuthClientEntity();
        UserEntity user = new UserEntity();
        user.setLoginName(name);
        user.setRealm(realm);
        user.setEnabled(true);
        em.persist(user);
        data.setAgent(user);
        data.setName(name);
        data.setRealm(realm);
        em.persist(data);
        em.flush();
        return new OAuthClientAdapter(data);
    }

    @Override
    public boolean removeOAuthClient(String id) {
        OAuthClientEntity client = em.find(OAuthClientEntity.class, id);
        em.createQuery("delete from " + UserScopeMappingEntity.class.getSimpleName() + " where user = :user").setParameter("user", client.getAgent()).executeUpdate();
        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where user = :user").setParameter("user", client.getAgent()).executeUpdate();
        removeUser(client.getAgent());
        em.remove(client);
        return true;
    }


    @Override
    public OAuthClientModel getOAuthClient(String name) {
        TypedQuery<OAuthClientEntity> query = em.createNamedQuery("findOAuthClientByUser", OAuthClientEntity.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm);
        List<OAuthClientEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        return new OAuthClientAdapter(entities.get(0));
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        OAuthClientEntity client = em.find(OAuthClientEntity.class, id);
        if (client == null) return null;
        return new OAuthClientAdapter(client);
    }


    @Override
    public List<OAuthClientModel> getOAuthClients() {
        TypedQuery<OAuthClientEntity> query = em.createNamedQuery("findOAuthClientByRealm", OAuthClientEntity.class);
        query.setParameter("realm", realm);
        List<OAuthClientEntity> entities = query.getResultList();
        List<OAuthClientModel> list = new ArrayList<OAuthClientModel>();
        for (OAuthClientEntity entity : entities) list.add(new OAuthClientAdapter(entity));
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
        RoleModel role = getRole(name);
        if (role != null) return role;
        RealmRoleEntity entity = new RealmRoleEntity();
        entity.setName(name);
        entity.setRealm(realm);
        realm.getRoles().add(entity);
        em.persist(entity);
        em.flush();
        return new RoleAdapter(this, em, entity);
    }

    @Override
    public boolean removeRoleById(String id) {
        RoleModel role = getRoleById(id);
        if (role == null) return false;

        if (role == null) {
            return false;
        }
        RoleEntity roleEntity = ((RoleAdapter)role).getRole();
        realm.getRoles().remove(role);
        realm.getDefaultRoles().remove(role);

        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", roleEntity).executeUpdate();
        em.createQuery("delete from " + UserScopeMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", roleEntity).executeUpdate();

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
        return new RoleAdapter(this, em, entity);
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

    protected TypedQuery<UserRoleMappingEntity> getUserRoleMappingEntityTypedQuery(UserAdapter user, RoleAdapter role) {
        TypedQuery<UserRoleMappingEntity> query = em.createNamedQuery("userHasRole", UserRoleMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("role", ((RoleAdapter) role).getRole());
        return query;
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        if (hasRole(user, role)) return;
        UserRoleMappingEntity entity = new UserRoleMappingEntity();
        entity.setUser(((UserAdapter) user).getUser());
        entity.setRole(((RoleAdapter)role).getRole());
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
        query.setParameter("user", ((UserAdapter)user).getUser());
        List<UserRoleMappingEntity> entities = query.getResultList();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (UserRoleMappingEntity entity : entities) {
            roles.add(new RoleAdapter(this, em, entity.getRole()));
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        TypedQuery<UserRoleMappingEntity> query = getUserRoleMappingEntityTypedQuery((UserAdapter) user, (RoleAdapter) role);
        List<UserRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (UserRoleMappingEntity entity : results) {
            em.remove(entity);
        }
        em.flush();
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings(UserModel user) {
        Set<RoleModel> roleMappings = getScopeMappings(user);

        Set<RoleModel> appRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                if (((RealmModel)container).getId().equals(getId())) {
                    appRoles.add(role);
                }
            }
        }

        return appRoles;
    }


    @Override
    public Set<RoleModel> getScopeMappings(UserModel agent) {
        TypedQuery<UserScopeMappingEntity> query = em.createNamedQuery("userScopeMappings", UserScopeMappingEntity.class);
        query.setParameter("user", ((UserAdapter)agent).getUser());
        List<UserScopeMappingEntity> entities = query.getResultList();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (UserScopeMappingEntity entity : entities) {
            roles.add(new RoleAdapter(this, em, entity.getRole()));
        }
        return roles;
    }

    @Override
    public void addScopeMapping(UserModel agent, RoleModel role) {
        if (hasScope(agent, role)) return;
        UserScopeMappingEntity entity = new UserScopeMappingEntity();
        entity.setUser(((UserAdapter) agent).getUser());
        entity.setRole(((RoleAdapter)role).getRole());
        em.persist(entity);
    }

    @Override
    public void deleteScopeMapping(UserModel user, RoleModel role) {
        TypedQuery<UserScopeMappingEntity> query = getRealmScopeMappingQuery((UserAdapter) user, (RoleAdapter) role);
        List<UserScopeMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (UserScopeMappingEntity entity : results) {
            em.remove(entity);
        }
    }

    public boolean hasScope(UserModel user, RoleModel role) {
        TypedQuery<UserScopeMappingEntity> query = getRealmScopeMappingQuery((UserAdapter) user, (RoleAdapter) role);
        return query.getResultList().size() > 0;
    }


    protected TypedQuery<UserScopeMappingEntity> getRealmScopeMappingQuery(UserAdapter user, RoleAdapter role) {
        TypedQuery<UserScopeMappingEntity> query = em.createNamedQuery("userHasScope", UserScopeMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("role", ((RoleAdapter)role).getRole());
        return query;
    }

    @Override
    public UserCredentialModel getSecret(UserModel user) {
        for (CredentialEntity cred : ((UserAdapter)user).getUser().getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.SECRET)) {
                return UserCredentialModel.secret(cred.getValue());
            }
        }
        return null;

    }

    @Override
    public boolean validatePassword(UserModel user, String password) {
        for (CredentialEntity cred : ((UserAdapter)user).getUser().getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return new Pbkdf2PasswordEncoder(cred.getSalt()).verify(password, cred.getValue());
            }
        }
        return false;
    }

    @Override
    public boolean validateSecret(UserModel user, String secret) {
        for (CredentialEntity cred : ((UserAdapter)user).getUser().getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.SECRET)) {
                return secret.equals(cred.getValue());
            }
        }
        return false;
    }



    @Override
    public boolean validateTOTP(UserModel user, String password, String token) {
        if (!validatePassword(user, password)) return false;
        for (CredentialEntity cred : ((UserAdapter)user).getUser().getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.TOTP)) {
                return new TimeBasedOTP().validate(token, cred.getValue().getBytes());
            }
        }
        return false;
    }

    @Override
    public void updateCredential(UserModel user, UserCredentialModel cred) {
        CredentialEntity credentialEntity = null;
        UserEntity userEntity = ((UserAdapter) user).getUser();
        for (CredentialEntity entity : userEntity.getCredentials()) {
            if (entity.getType().equals(cred.getType())) {
               credentialEntity = entity;
            }
        }
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
        RealmAdapter r = (RealmAdapter)o;
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
}
