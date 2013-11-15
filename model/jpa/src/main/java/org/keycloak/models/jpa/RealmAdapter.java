package org.keycloak.models.jpa;

import org.bouncycastle.openssl.PEMWriter;
import org.keycloak.PemUtils;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.ApplicationEntity;
import org.keycloak.models.jpa.entities.ApplicationUserRoleMappingEntity;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.OAuthClientEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RealmScopeMappingEntity;
import org.keycloak.models.jpa.entities.RealmUserRoleMappingEntity;
import org.keycloak.models.jpa.entities.RequiredCredentialEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.SocialLinkEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.SHAPasswordEncoder;
import org.keycloak.models.utils.TimeBasedOTP;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
    public boolean isCookieLoginAllowed() {
        return realm.isCookieLoginAllowed();
    }

    @Override
    public void setCookieLoginAllowed(boolean cookieLoginAllowed) {
        realm.setCookieLoginAllowed(cookieLoginAllowed);
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
        String pem = getPublicKeyPem();
        if (pem != null) {
            try {
                publicKey = PemUtils.decodePublicKey(pem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return publicKey;
    }

    @Override
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        try {
            pemWriter.writeObject(publicKey);
            pemWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s = writer.toString();
        setPublicKeyPem(PemUtils.removeBeginEnd(s));
    }

    @Override
    public PrivateKey getPrivateKey() {
        if (privateKey != null) return privateKey;
        String pem = getPrivateKeyPem();
        if (pem != null) {
            try {
                privateKey = PemUtils.decodePrivateKey(pem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return privateKey;
    }

    @Override
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        try {
            pemWriter.writeObject(privateKey);
            pemWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s = writer.toString();
        setPrivateKeyPem(PemUtils.removeBeginEnd(s));
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
        Collection<RequiredCredentialEntity> entities = realm.getRequiredApplicationCredentials();
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
        Collection<RequiredCredentialEntity> entities = realm.getRequiredOAuthClientCredentials();
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
        realm.getRequiredOAuthClientCredentials().add(entity);
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
        realm.getRequiredApplicationCredentials().add(entity);
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
        Collection<RequiredCredentialEntity> relationships = realm.getRequiredOAuthClientCredentials();
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
        Collection<RequiredCredentialEntity> relationships = realm.getRequiredApplicationCredentials();
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
    public UserModel addUser(String username) {
        UserEntity entity = new UserEntity();
        entity.setLoginName(username);
        entity.setRealm(realm);
        em.persist(entity);
        em.flush();
        return new UserAdapter(entity);
    }

    @Override
    public boolean deleteUser(String name) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByLoginName", UserEntity.class);
        query.setParameter("loginName", name);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return false;

        UserEntity user = results.get(0);

        for (Class r : UserEntity.RELATIONSHIPS) {
            em.createQuery("delete from " + r.getSimpleName() + " where user = :user").setParameter("user", user).executeUpdate();
        }

        em.remove(user);

        return true;
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
            list.add(new ApplicationAdapter(em, entity));
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
        ApplicationModel resource = new ApplicationAdapter(em, applicationData);
        resource.addRole("*");
        resource.addScopeMapping(new UserAdapter(user), "*");
        em.flush();
        return resource;
    }

    @Override
    public ApplicationModel getApplicationById(String id) {
        ApplicationEntity app = em.find(ApplicationEntity.class, id);
        if (app == null) return null;
        return new ApplicationAdapter(em, app);
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
    public boolean isAutomaticRegistrationAfterSocialLogin() {
        return realm.isAutomaticRegistrationAfterSocialLogin();
    }

    @Override
    public void setAutomaticRegistrationAfterSocialLogin(boolean automaticRegistrationAfterSocialLogin) {
        realm.setAutomaticRegistrationAfterSocialLogin(automaticRegistrationAfterSocialLogin);
        em.flush();
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
    public OAuthClientModel getOAuthClient(String name) {
        TypedQuery<OAuthClientEntity> query = em.createNamedQuery("findOAuthClientByUser", OAuthClientEntity.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm);
        List<OAuthClientEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        return new OAuthClientAdapter(entities.get(0));
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
        Collection<RoleEntity> roles = realm.getRoles();
        if (roles == null) return null;
        for (RoleEntity role : roles) {
            if (role.getName().equals(name)) {
                return new RoleAdapter(role);
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleModel addRole(String name) {
        RoleModel role = getRole(name);
        if (role != null) return role;
        RoleEntity entity = new RoleEntity();
        entity.setName(name);
        em.persist(entity);
        realm.getRoles().add(entity);
        em.flush();
        return new RoleAdapter(entity);
    }

    @Override
    public List<RoleModel> getRoles() {
        ArrayList<RoleModel> list = new ArrayList<RoleModel>();
        Collection<RoleEntity> roles = realm.getRoles();
        if (roles == null) return list;
        for (RoleEntity entity : roles) {
            list.add(new RoleAdapter(entity));
        }
        return list;
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        return new RoleAdapter(entity);
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        TypedQuery<RealmUserRoleMappingEntity> query = getRealmUserRoleMappingEntityTypedQuery((UserAdapter) user, (RoleAdapter) role);
        return query.getResultList().size() > 0;
    }

    protected TypedQuery<RealmUserRoleMappingEntity> getRealmUserRoleMappingEntityTypedQuery(UserAdapter user, RoleAdapter role) {
        TypedQuery<RealmUserRoleMappingEntity> query = em.createNamedQuery("userHasRealmRole", RealmUserRoleMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("role", ((RoleAdapter)role).getRole());
        query.setParameter("realm", realm);
        return query;
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        if (hasRole(user, role)) return;
        RealmUserRoleMappingEntity entity = new RealmUserRoleMappingEntity();
        entity.setRealm(realm);
        entity.setUser(((UserAdapter) user).getUser());
        entity.setRole(((RoleAdapter)role).getRole());
        em.persist(entity);
        em.flush();
    }

    @Override
    public List<RoleModel> getRoleMappings(UserModel user) {
        TypedQuery<RealmUserRoleMappingEntity> query = em.createNamedQuery("userRealmMappings", RealmUserRoleMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("realm", realm);
        List<RealmUserRoleMappingEntity> entities = query.getResultList();
        List<RoleModel> roles = new ArrayList<RoleModel>();
        for (RealmUserRoleMappingEntity entity : entities) {
            roles.add(new RoleAdapter(entity.getRole()));
        }
        return roles;
    }

    @Override
    public Set<String> getRoleMappingValues(UserModel user) {
        TypedQuery<RealmUserRoleMappingEntity> query = em.createNamedQuery("userRealmMappings", RealmUserRoleMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("realm", realm);
        List<RealmUserRoleMappingEntity> entities = query.getResultList();
        Set<String> roles = new HashSet<String>();
        for (RealmUserRoleMappingEntity entity : entities) {
            roles.add(entity.getRole().getName());
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        TypedQuery<RealmUserRoleMappingEntity> query = getRealmUserRoleMappingEntityTypedQuery((UserAdapter) user, (RoleAdapter) role);
        List<RealmUserRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (RealmUserRoleMappingEntity entity : results) {
            em.remove(entity);
        }
        em.flush();
    }

    @Override
    public boolean hasRole(UserModel user, String roleName) {
        RoleModel role = getRole(roleName);
        if (role == null) return false;
        return hasRole(user, role);
    }

    @Override
    public void addScopeMapping(UserModel agent, String roleName) {
        RoleModel role = getRole(roleName);
        if (role == null) throw new RuntimeException("role does not exist");
        addScopeMapping(agent, role);
        em.flush();
    }

    @Override
    public Set<String> getScopeMappingValues(UserModel agent) {
        TypedQuery<RealmScopeMappingEntity> query = em.createNamedQuery("userRealmScopeMappings", RealmScopeMappingEntity.class);
        query.setParameter("user", ((UserAdapter)agent).getUser());
        query.setParameter("realm", realm);
        List<RealmScopeMappingEntity> entities = query.getResultList();
        Set<String> roles = new HashSet<String>();
        for (RealmScopeMappingEntity entity : entities) {
            roles.add(entity.getRole().getName());
        }
        return roles;
    }

    @Override
    public List<RoleModel> getScopeMappings(UserModel agent) {
        TypedQuery<RealmScopeMappingEntity> query = em.createNamedQuery("userRealmScopeMappings", RealmScopeMappingEntity.class);
        query.setParameter("user", ((UserAdapter)agent).getUser());
        query.setParameter("realm", realm);
        List<RealmScopeMappingEntity> entities = query.getResultList();
        List<RoleModel> roles = new ArrayList<RoleModel>();
        for (RealmScopeMappingEntity entity : entities) {
            roles.add(new RoleAdapter(entity.getRole()));
        }
        return roles;
    }

    @Override
    public void addScopeMapping(UserModel agent, RoleModel role) {
        if (hasScope(agent, role)) return;
        RealmScopeMappingEntity entity = new RealmScopeMappingEntity();
        entity.setRealm(realm);
        entity.setUser(((UserAdapter) agent).getUser());
        entity.setRole(((RoleAdapter)role).getRole());
        em.persist(entity);
        em.flush();
    }

    @Override
    public void deleteScopeMapping(UserModel user, RoleModel role) {
        TypedQuery<RealmScopeMappingEntity> query = getRealmScopeMappingQuery((UserAdapter) user, (RoleAdapter) role);
        List<RealmScopeMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (RealmScopeMappingEntity entity : results) {
            em.remove(entity);
        }
    }

    public boolean hasScope(UserModel user, RoleModel role) {
        TypedQuery<RealmScopeMappingEntity> query = getRealmScopeMappingQuery((UserAdapter) user, (RoleAdapter) role);
        return query.getResultList().size() > 0;
    }


    protected TypedQuery<RealmScopeMappingEntity> getRealmScopeMappingQuery(UserAdapter user, RoleAdapter role) {
        TypedQuery<RealmScopeMappingEntity> query = em.createNamedQuery("userHasRealmScope", RealmScopeMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("role", ((RoleAdapter)role).getRole());
        query.setParameter("realm", realm);
        return query;
    }

    @Override
    public boolean validatePassword(UserModel user, String password) {
        for (CredentialEntity cred : ((UserAdapter)user).getUser().getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return new SHAPasswordEncoder(512).verify(password, cred.getValue());
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
            credentialEntity.setValue(new SHAPasswordEncoder(512).encode(cred.getValue()));
        } else {
            credentialEntity.setValue(cred.getValue());
        }
        credentialEntity.setDevice(cred.getDevice());
        em.flush();
    }

}
