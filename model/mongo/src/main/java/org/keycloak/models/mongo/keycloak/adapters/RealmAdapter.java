package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.jboss.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.ApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.CredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.OAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.RealmEntity;
import org.keycloak.models.mongo.keycloak.entities.RequiredCredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.RoleEntity;
import org.keycloak.models.mongo.keycloak.entities.SocialLinkEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;
import org.keycloak.models.utils.TimeBasedOTP;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmAdapter extends AbstractAdapter implements RealmModel {

    private static final Logger logger = Logger.getLogger(RealmAdapter.class);

    private final RealmEntity realm;

    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;

    private volatile transient PasswordPolicy passwordPolicy;

    public RealmAdapter(RealmEntity realmEntity, MongoStore mongoStore, MongoStoreInvocationContext invocationContext) {
        super(mongoStore, invocationContext);
        this.realm = realmEntity;
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
    public boolean isSslNotRequired() {
        return realm.isSslNotRequired();
    }

    @Override
    public void setSslNotRequired(boolean sslNotRequired) {
        realm.setSslNotRequired(sslNotRequired);
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
    public boolean isSocial() {
        return realm.isSocial();
    }

    @Override
    public void setSocial(boolean social) {
        realm.setSocial(social);
        updateRealm();
    }

    @Override
    public boolean isUpdateProfileOnInitialSocialLogin() {
        return realm.isUpdateProfileOnInitialSocialLogin();
    }

    @Override
    public void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin) {
        realm.setUpdateProfileOnInitialSocialLogin(updateProfileOnInitialSocialLogin);
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
    public int getTokenLifespan() {
        return realm.getTokenLifespan();
    }

    @Override
    public void setTokenLifespan(int tokenLifespan) {
        realm.setTokenLifespan(tokenLifespan);
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
    public UserAdapter getUser(String name) {
        DBObject query = new QueryBuilder()
                .and("loginName").is(name)
                .and("realmId").is(getId())
                .get();
        UserEntity user = mongoStore.loadSingleObject(UserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(user, mongoStore, invocationContext);
        }
    }

    @Override
    public UserModel getUserByEmail(String email) {
        DBObject query = new QueryBuilder()
                .and("email").is(email)
                .and("realmId").is(getId())
                .get();
        UserEntity user = mongoStore.loadSingleObject(UserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(user, mongoStore, invocationContext);
        }
    }

    @Override
    public UserAdapter addUser(String username) {
        UserAdapter userModel = addUserEntity(username);

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

    // Add just user entity without defaultRoles
    protected UserAdapter addUserEntity(String username) {
        if (getUser(username) != null) {
            throw new IllegalArgumentException("User " + username + " already exists");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setLoginName(username);
        userEntity.setEnabled(true);
        userEntity.setRealmId(getId());

        mongoStore.insertObject(userEntity, invocationContext);
        return new UserAdapter(userEntity, mongoStore, invocationContext);
    }

    @Override
    public boolean removeUser(String name) {
        DBObject query = new QueryBuilder()
                .and("loginName").is(name)
                .and("realmId").is(getId())
                .get();
        return mongoStore.removeObjects(UserEntity.class, query, invocationContext);
    }

    @Override
    public RoleAdapter getRole(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .and("realmId").is(getId())
                .get();
        RoleEntity role = mongoStore.loadSingleObject(RoleEntity.class, query, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, this, mongoStore, invocationContext);
        }
    }

    @Override
    public RoleModel addRole(String name) {
        RoleAdapter role = getRole(name);
        if (role != null) {
            // Compatibility with JPA model
            return role;
            // throw new IllegalArgumentException("Role " + name + " already exists");
        }

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(name);
        roleEntity.setRealmId(getId());

        mongoStore.insertObject(roleEntity, invocationContext);
        return new RoleAdapter(roleEntity, this, mongoStore, invocationContext);
    }

    @Override
    public boolean removeRoleById(String id) {
        return mongoStore.removeObject(RoleEntity.class ,id, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoles() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<RoleEntity> roles = mongoStore.loadObjects(RoleEntity.class, query, invocationContext);

        Set<RoleModel> result = new HashSet<RoleModel>();

        if (roles == null) return result;
        for (RoleEntity role : roles) {
            result.add(new RoleAdapter(role, this, mongoStore, invocationContext));
        }

        return result;
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleEntity role = mongoStore.loadObject(RoleEntity.class, id, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, this, mongoStore, invocationContext);
        }
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

        mongoStore.pushItemToList(realm, "defaultRoles", name, true, invocationContext);
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
    public ApplicationModel getApplicationById(String id) {
        ApplicationEntity appData = mongoStore.loadObject(ApplicationEntity.class, id, invocationContext);

        // Check if application belongs to this realm
        if (appData == null || !getId().equals(appData.getRealmId())) {
            return null;
        }

        return new ApplicationAdapter(appData, mongoStore, invocationContext);
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .and("name").is(name)
                .get();
        ApplicationEntity appEntity = mongoStore.loadSingleObject(ApplicationEntity.class, query, invocationContext);
        return appEntity==null ? null : new ApplicationAdapter(appEntity, mongoStore, invocationContext);
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
        List<ApplicationEntity> appDatas = mongoStore.loadObjects(ApplicationEntity.class, query, invocationContext);

        List<ApplicationModel> result = new ArrayList<ApplicationModel>();
        for (ApplicationEntity appData : appDatas) {
            result.add(new ApplicationAdapter(appData, mongoStore, invocationContext));
        }
        return result;
    }

    @Override
    public ApplicationModel addApplication(String name) {
        UserAdapter resourceUser = addUserEntity(name);

        ApplicationEntity appData = new ApplicationEntity();
        appData.setName(name);
        appData.setRealmId(getId());
        appData.setEnabled(true);
        appData.setResourceUserId(resourceUser.getUser().getId());
        mongoStore.insertObject(appData, invocationContext);

        return new ApplicationAdapter(appData, resourceUser, mongoStore, invocationContext);
    }

    @Override
    public boolean removeApplication(String id) {
        return mongoStore.removeObject(ApplicationEntity.class, id, invocationContext);
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
    public void grantRole(UserModel user, RoleModel role) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        mongoStore.pushItemToList(userEntity, "roleIds", role.getId(), true, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoleMappings(UserModel user) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllRolesOfUser(user, mongoStore, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getRealmId())) {
                result.add(new RoleAdapter(role, this, mongoStore, invocationContext));
            } else {
                // Likely applicationRole, but we don't have this application yet
                result.add(new RoleAdapter(role, mongoStore, invocationContext));
            }
        }
        return result;
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings(UserModel user) {
        Set<RoleModel> allRoles = getRoleMappings(user);

        // Filter to retrieve just realm roles TODO: Maybe improve to avoid filter programmatically... Maybe have separate fields for realmRoles and appRoles on user?
        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allRoles) {
            RoleEntity roleEntity = ((RoleAdapter)role).getRole();

            if (getId().equals(roleEntity.getRealmId())) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        mongoStore.pullItemFromList(userEntity, "roleIds", role.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getScopeMappings(UserModel user) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllScopesOfUser(user, mongoStore, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getRealmId())) {
                result.add(new RoleAdapter(role, this, mongoStore, invocationContext));
            } else {
                // Likely applicationRole, but we don't have this application yet
                result.add(new RoleAdapter(role, mongoStore, invocationContext));
            }
        }
        return result;
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings(UserModel user) {
        Set<RoleModel> allScopes = getScopeMappings(user);

        // Filter to retrieve just realm roles TODO: Maybe improve to avoid filter programmatically... Maybe have separate fields for realmRoles and appRoles on user?
        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allScopes) {
            RoleEntity roleEntity = ((RoleAdapter)role).getRole();

            if (getId().equals(roleEntity.getRealmId())) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public void addScopeMapping(UserModel agent, RoleModel role) {
        UserEntity userEntity = ((UserAdapter)agent).getUser();
        mongoStore.pushItemToList(userEntity, "scopeIds", role.getId(), true, invocationContext);
    }

    @Override
    public void deleteScopeMapping(UserModel user, RoleModel role) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        mongoStore.pullItemFromList(userEntity, "scopeIds", role.getId(), invocationContext);
    }

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        UserAdapter oauthAgent = addUserEntity(name);

        OAuthClientEntity oauthClient = new OAuthClientEntity();
        oauthClient.setOauthAgentId(oauthAgent.getUser().getId());
        oauthClient.setRealmId(getId());
        oauthClient.setName(name);
        mongoStore.insertObject(oauthClient, invocationContext);

        return new OAuthClientAdapter(oauthClient, oauthAgent, mongoStore, invocationContext);
    }

    @Override
    public boolean removeOAuthClient(String id) {
        return mongoStore.removeObject(OAuthClientEntity.class, id, invocationContext);
    }

    @Override
    public OAuthClientModel getOAuthClient(String name) {
        UserAdapter user = getUser(name);
        if (user == null) return null;
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .and("oauthAgentId").is(user.getUser().getId())
                .get();
        OAuthClientEntity oauthClient = mongoStore.loadSingleObject(OAuthClientEntity.class, query, invocationContext);
        return oauthClient == null ? null : new OAuthClientAdapter(oauthClient, user, mongoStore, invocationContext);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        OAuthClientEntity clientEntity = mongoStore.loadObject(OAuthClientEntity.class, id, invocationContext);
        if (clientEntity == null) return null;
        return new OAuthClientAdapter(clientEntity, mongoStore, invocationContext);
    }

    @Override
    public List<OAuthClientModel> getOAuthClients() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<OAuthClientEntity> results = mongoStore.loadObjects(OAuthClientEntity.class, query, invocationContext);
        List<OAuthClientModel> list = new ArrayList<OAuthClientModel>();
        for (OAuthClientEntity data : results) {
            list.add(new OAuthClientAdapter(data, mongoStore, invocationContext));
        }
        return list;
    }

    @Override
    public void addRequiredCredential(String type) {
        RequiredCredentialModel credentialModel = initRequiredCredentialModel(type);
        addRequiredCredential(credentialModel, realm.getRequiredCredentials());
    }

    @Override
    public void addRequiredResourceCredential(String type) {
        RequiredCredentialModel credentialModel = initRequiredCredentialModel(type);
        addRequiredCredential(credentialModel, realm.getRequiredApplicationCredentials());
    }

    @Override
    public void addRequiredOAuthClientCredential(String type) {
        RequiredCredentialModel credentialModel = initRequiredCredentialModel(type);
        addRequiredCredential(credentialModel, realm.getRequiredOAuthClientCredentials());
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

    @Override
    public void updateRequiredApplicationCredentials(Set<String> creds) {
        updateRequiredCredentials(creds, realm.getRequiredApplicationCredentials());
    }

    @Override
    public void updateRequiredOAuthClientCredentials(Set<String> creds) {
        updateRequiredCredentials(creds, realm.getRequiredOAuthClientCredentials());
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
            logger.info("updating cred: " + cred);
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

    @Override
    public List<RequiredCredentialModel> getRequiredApplicationCredentials() {
        return convertRequiredCredentialEntities(realm.getRequiredApplicationCredentials());
    }

    @Override
    public List<RequiredCredentialModel> getRequiredOAuthClientCredentials() {
        return convertRequiredCredentialEntities(realm.getRequiredOAuthClientCredentials());
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
            userEntity.getCredentials().add(credentialEntity);
        }
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            byte[] salt = Pbkdf2PasswordEncoder.getSalt();
            credentialEntity.setValue(new Pbkdf2PasswordEncoder(salt).encode(cred.getValue()));
            credentialEntity.setSalt(salt);
        } else {
            credentialEntity.setValue(cred.getValue());
        }
        credentialEntity.setDevice(cred.getDevice());

        mongoStore.updateObject(userEntity, invocationContext);
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        DBObject query = new QueryBuilder()
                .and("socialLinks.socialProvider").is(socialLink.getSocialProvider())
                .and("socialLinks.socialUsername").is(socialLink.getSocialUsername())
                .and("realmId").is(getId())
                .get();
        UserEntity userEntity = mongoStore.loadSingleObject(UserEntity.class, query, invocationContext);
        return userEntity==null ? null : new UserAdapter(userEntity, mongoStore, invocationContext);
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        List<SocialLinkEntity> linkEntities = userEntity.getSocialLinks();

        if (linkEntities == null) {
            return Collections.EMPTY_SET;
        }

        Set<SocialLinkModel> result = new HashSet<SocialLinkModel>();
        for (SocialLinkEntity socialLinkEntity : linkEntities) {
            SocialLinkModel model = new SocialLinkModel(socialLinkEntity.getSocialProvider(), socialLinkEntity.getSocialUsername());
            result.add(model);
        }
        return result;
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        SocialLinkEntity socialLinkEntity = new SocialLinkEntity();
        socialLinkEntity.setSocialProvider(socialLink.getSocialProvider());
        socialLinkEntity.setSocialUsername(socialLink.getSocialUsername());

        mongoStore.pushItemToList(userEntity, "socialLinks", socialLinkEntity, true, invocationContext);
    }

    @Override
    public void removeSocialLink(UserModel user, SocialLinkModel socialLink) {
        SocialLinkEntity socialLinkEntity = new SocialLinkEntity();
        socialLinkEntity.setSocialProvider(socialLink.getSocialProvider());
        socialLinkEntity.setSocialUsername(socialLink.getSocialUsername());

        UserEntity userEntity = ((UserAdapter)user).getUser();
        mongoStore.pullItemFromList(userEntity, "socialLinks", socialLinkEntity, invocationContext);
    }

    protected void updateRealm() {
        mongoStore.updateObject(realm, invocationContext);
    }

    protected RequiredCredentialModel initRequiredCredentialModel(String type) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(type);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + type);
        }
        return model;
    }

    @Override
    public List<UserModel> getUsers() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<UserEntity> users = mongoStore.loadObjects(UserEntity.class, query, invocationContext);
        return convertUserEntities(users);
    }

    @Override
    public List<UserModel> searchForUser(String search) {
        search = search.trim();
        Pattern caseInsensitivePattern = Pattern.compile("(?i:" + search + ")");

        QueryBuilder nameBuilder;
        int spaceInd = search.lastIndexOf(" ");

        // Case when we have search string like "ohn Bow". Then firstName must end with "ohn" AND lastName must start with "bow" (everything case-insensitive)
        if (spaceInd != -1) {
            String firstName = search.substring(0, spaceInd);
            String lastName = search.substring(spaceInd + 1);
            Pattern firstNamePattern =  Pattern.compile("(?i:" + firstName + "$)");
            Pattern lastNamePattern =  Pattern.compile("(?i:^" + lastName + ")");
            nameBuilder = new QueryBuilder().and(
                    new QueryBuilder().put("firstName").regex(firstNamePattern).get(),
                    new QueryBuilder().put("lastName").regex(lastNamePattern).get()
            );
        } else {
            // Case when we have search without spaces like "foo". The firstName OR lastName could be "foo" (everything case-insensitive)
            nameBuilder = new QueryBuilder().or(
                    new QueryBuilder().put("firstName").regex(caseInsensitivePattern).get(),
                    new QueryBuilder().put("lastName").regex(caseInsensitivePattern).get()
            );
        }

        QueryBuilder builder = new QueryBuilder().and(
                new QueryBuilder().and("realmId").is(getId()).get(),
                new QueryBuilder().or(
                        new QueryBuilder().put("loginName").regex(caseInsensitivePattern).get(),
                        new QueryBuilder().put("email").regex(caseInsensitivePattern).get(),
                        nameBuilder.get()

                ).get()
        );

        List<UserEntity> users = mongoStore.loadObjects(UserEntity.class, builder.get(), invocationContext);
        return convertUserEntities(users);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(getId());

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equals(UserModel.LOGIN_NAME)) {
                queryBuilder.and("loginName").regex(Pattern.compile("(?i:" + entry.getValue() + "$)"));
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                queryBuilder.and(UserModel.FIRST_NAME).regex(Pattern.compile("(?i:" + entry.getValue() + "$)"));

            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                queryBuilder.and(UserModel.LAST_NAME).regex(Pattern.compile("(?i:" + entry.getValue() + "$)"));

            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                queryBuilder.and(UserModel.EMAIL).regex(Pattern.compile("(?i:" + entry.getValue() + "$)"));
            }
        }
        List<UserEntity> users = mongoStore.loadObjects(UserEntity.class, queryBuilder.get(), invocationContext);
        return convertUserEntities(users);
    }

    protected List<UserModel> convertUserEntities(List<UserEntity> userEntities) {
        List<UserModel> userModels = new ArrayList<UserModel>();
        for (UserEntity user : userEntities) {
            userModels.add(new UserAdapter(user, mongoStore, invocationContext));
        }
        return userModels;
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
    public Map<String, String> getSocialConfig() {
        return realm.getSocialConfig();
    }

    @Override
    public void setSocialConfig(Map<String, String> socialConfig) {
        realm.setSocialConfig(socialConfig);
        updateRealm();
    }

    @Override
    public AbstractMongoIdentifiableEntity getMongoEntity() {
        return realm;
    }
}
