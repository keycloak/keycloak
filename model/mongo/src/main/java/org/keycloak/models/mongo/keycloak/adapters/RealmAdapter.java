package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.jboss.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.ApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.AuthenticationLinkEntity;
import org.keycloak.models.mongo.keycloak.entities.AuthenticationProviderEntity;
import org.keycloak.models.mongo.keycloak.entities.CredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.OAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.RealmEntity;
import org.keycloak.models.mongo.keycloak.entities.RequiredCredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.RoleEntity;
import org.keycloak.models.mongo.keycloak.entities.SocialLinkEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;
import org.keycloak.models.mongo.keycloak.entities.UsernameLoginFailureEntity;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmAdapter extends AbstractMongoAdapter<RealmEntity> implements RealmModel {

    private static final Logger logger = Logger.getLogger(RealmAdapter.class);

    private final RealmEntity realm;

    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;

    private volatile transient PasswordPolicy passwordPolicy;

    public RealmAdapter(RealmEntity realmEntity, MongoStoreInvocationContext invocationContext) {
        super(invocationContext);
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
    public int getNotBefore() {
        return realm.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        realm.setNotBefore(notBefore);
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
    public int getCentralLoginLifespan() {
        return realm.getCentralLoginLifespan();
    }

    @Override
    public void setCentralLoginLifespan(int lifespan) {
        realm.setCentralLoginLifespan(lifespan);
        updateRealm();
    }


    @Override
    public int getRefreshTokenLifespan() {
        return realm.getRefreshTokenLifespan();
    }

    @Override
    public void setRefreshTokenLifespan(int tokenLifespan) {
        realm.setRefreshTokenLifespan(tokenLifespan);
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
        UserEntity user = getMongoStore().loadSingleEntity(UserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(user, invocationContext);
        }
    }

    @Override
    public UsernameLoginFailureAdapter getUserLoginFailure(String name) {
        DBObject query = new QueryBuilder()
                .and("username").is(name)
                .and("realmId").is(getId())
                .get();
        UsernameLoginFailureEntity user = getMongoStore().loadSingleEntity(UsernameLoginFailureEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UsernameLoginFailureAdapter(invocationContext, user);
        }
    }

    @Override
    public UsernameLoginFailureAdapter addUserLoginFailure(String username) {
        UsernameLoginFailureAdapter userLoginFailure = getUserLoginFailure(username);
        if (userLoginFailure != null) {
            return userLoginFailure;
        }

        UsernameLoginFailureEntity userEntity = new UsernameLoginFailureEntity();
        userEntity.setUsername(username);
        // Compatibility with JPA model, which has user disabled by default
        // userEntity.setEnabled(true);
        userEntity.setRealmId(getId());

        getMongoStore().insertEntity(userEntity, invocationContext);
        return new UsernameLoginFailureAdapter(invocationContext, userEntity);
    }

    @Override
    public UserModel getUserByEmail(String email) {
        DBObject query = new QueryBuilder()
                .and("email").is(email)
                .and("realmId").is(getId())
                .get();
        UserEntity user = getMongoStore().loadSingleEntity(UserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(user, invocationContext);
        }
    }

    @Override
    public UserModel getUserById(String id) {
        UserEntity user = getMongoStore().loadEntity(UserEntity.class, id, invocationContext);

        // Check that it's user from this realm
        if (user == null || !getId().equals(user.getRealmId())) {
            return null;
        } else {
            return new UserAdapter(user, invocationContext);
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
        // Compatibility with JPA model, which has user disabled by default
        // userEntity.setEnabled(true);
        userEntity.setRealmId(getId());

        getMongoStore().insertEntity(userEntity, invocationContext);
        return new UserAdapter(userEntity, invocationContext);
    }

    @Override
    public boolean removeUser(String name) {
        DBObject query = new QueryBuilder()
                .and("loginName").is(name)
                .and("realmId").is(getId())
                .get();
        return getMongoStore().removeEntities(UserEntity.class, query, invocationContext);
    }

    @Override
    public RoleAdapter getRole(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .and("realmId").is(getId())
                .get();
        RoleEntity role = getMongoStore().loadSingleEntity(RoleEntity.class, query, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(this, role, this, invocationContext);
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

        getMongoStore().insertEntity(roleEntity, invocationContext);
        return new RoleAdapter(this, roleEntity, this, invocationContext);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return removeRoleById(role.getId());
    }

    @Override
    public boolean removeRoleById(String id) {
        return getMongoStore().removeEntity(RoleEntity.class, id, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoles() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<RoleEntity> roles = getMongoStore().loadEntities(RoleEntity.class, query, invocationContext);

        Set<RoleModel> result = new HashSet<RoleModel>();

        if (roles == null) return result;
        for (RoleEntity role : roles) {
            result.add(new RoleAdapter(this, role, this, invocationContext));
        }

        return result;
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleEntity role = getMongoStore().loadEntity(RoleEntity.class, id, invocationContext);
        if (role == null) return null;
        if (role.getRealmId() != null) {
            if (!role.getRealmId().equals(this.getId())) return null;
        } else {
            ApplicationModel app = getApplicationById(role.getApplicationId());
            if (app == null) return null;
        }
        return new RoleAdapter(this, role, null, invocationContext);
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
    public ApplicationModel getApplicationById(String id) {
        ApplicationEntity appData = getMongoStore().loadEntity(ApplicationEntity.class, id, invocationContext);

        // Check if application belongs to this realm
        if (appData == null || !getId().equals(appData.getRealmId())) {
            return null;
        }

        return new ApplicationAdapter(this, appData, invocationContext);
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .and("name").is(name)
                .get();
        ApplicationEntity appEntity = getMongoStore().loadSingleEntity(ApplicationEntity.class, query, invocationContext);
        return appEntity==null ? null : new ApplicationAdapter(this, appEntity, invocationContext);
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
        List<ApplicationEntity> appDatas = getMongoStore().loadEntities(ApplicationEntity.class, query, invocationContext);

        List<ApplicationModel> result = new ArrayList<ApplicationModel>();
        for (ApplicationEntity appData : appDatas) {
            result.add(new ApplicationAdapter(this, appData, invocationContext));
        }
        return result;
    }

    @Override
    public ApplicationModel addApplication(String name) {
        ApplicationEntity appData = new ApplicationEntity();
        appData.setName(name);
        appData.setRealmId(getId());
        appData.setEnabled(true);
        getMongoStore().insertEntity(appData, invocationContext);

        return new ApplicationAdapter(this, appData, invocationContext);
    }

    @Override
    public boolean removeApplication(String id) {
        return getMongoStore().removeEntity(ApplicationEntity.class, id, invocationContext);
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
        getMongoStore().pushItemToList(userEntity, "roleIds", role.getId(), true, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoleMappings(UserModel user) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllRolesOfUser(user, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getRealmId())) {
                result.add(new RoleAdapter(this, role, this, invocationContext));
            } else {
                // Likely applicationRole, but we don't have this application yet
                result.add(new RoleAdapter(this, role, invocationContext));
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
        if (user == null || role == null) return;

        UserEntity userEntity = ((UserAdapter)user).getUser();
        getMongoStore().pullItemFromList(userEntity, "roleIds", role.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getScopeMappings(ClientModel client) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllScopesOfClient(client, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getRealmId())) {
                result.add(new RoleAdapter(this, role, this, invocationContext));
            } else {
                // Likely applicationRole, but we don't have this application yet
                result.add(new RoleAdapter(this, role, invocationContext));
            }
        }
        return result;
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings(ClientModel client) {
        Set<RoleModel> allScopes = getScopeMappings(client);

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
    public boolean hasScope(ClientModel client, RoleModel role) {
        Set<RoleModel> roles = getScopeMappings(client);
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }


    @Override
    public void addScopeMapping(ClientModel client, RoleModel role) {
        getMongoStore().pushItemToList(((AbstractMongoAdapter)client).getMongoEntity(), "scopeIds", role.getId(), true, invocationContext);
    }

    @Override
    public void deleteScopeMapping(ClientModel client, RoleModel role) {
        getMongoStore().pullItemFromList(((AbstractMongoAdapter)client).getMongoEntity(), "scopeIds", role.getId(), invocationContext);
    }

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        OAuthClientEntity oauthClient = new OAuthClientEntity();
        oauthClient.setRealmId(getId());
        oauthClient.setName(name);
        getMongoStore().insertEntity(oauthClient, invocationContext);

        return new OAuthClientAdapter(this, oauthClient, invocationContext);
    }

    @Override
    public boolean removeOAuthClient(String id) {
        return getMongoStore().removeEntity(OAuthClientEntity.class, id, invocationContext);
    }

    @Override
    public OAuthClientModel getOAuthClient(String name) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .and("name").is(name)
                .get();
        OAuthClientEntity oauthClient = getMongoStore().loadSingleEntity(OAuthClientEntity.class, query, invocationContext);
        return oauthClient == null ? null : new OAuthClientAdapter(this, oauthClient, invocationContext);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        OAuthClientEntity clientEntity = getMongoStore().loadEntity(OAuthClientEntity.class, id, invocationContext);

        // Check if client belongs to this realm
        if (clientEntity == null || !getId().equals(clientEntity.getRealmId())) return null;

        return new OAuthClientAdapter(this, clientEntity, invocationContext);
    }

    @Override
    public List<OAuthClientModel> getOAuthClients() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<OAuthClientEntity> results = getMongoStore().loadEntities(OAuthClientEntity.class, query, invocationContext);
        List<OAuthClientModel> list = new ArrayList<OAuthClientModel>();
        for (OAuthClientEntity data : results) {
            list.add(new OAuthClientAdapter(this, data, invocationContext));
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
            logger.info("updating cred: " + cred);
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

        getMongoStore().updateEntity(userEntity, invocationContext);
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        DBObject query = new QueryBuilder()
                .and("socialLinks.socialProvider").is(socialLink.getSocialProvider())
                .and("socialLinks.socialUserId").is(socialLink.getSocialUserId())
                .and("realmId").is(getId())
                .get();
        UserEntity userEntity = getMongoStore().loadSingleEntity(UserEntity.class, query, invocationContext);
        return userEntity==null ? null : new UserAdapter(userEntity, invocationContext);
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
            SocialLinkModel model = new SocialLinkModel(socialLinkEntity.getSocialProvider(), socialLinkEntity.getSocialUserId(), socialLinkEntity.getSocialUsername());
            result.add(model);
        }
        return result;
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider) {
        SocialLinkEntity socialLinkEntity = findSocialLink(user, socialProvider);
        return socialLinkEntity!=null ? new SocialLinkModel(socialLinkEntity.getSocialProvider(), socialLinkEntity.getSocialUserId(), socialLinkEntity.getSocialUsername()) : null;
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        SocialLinkEntity socialLinkEntity = new SocialLinkEntity();
        socialLinkEntity.setSocialProvider(socialLink.getSocialProvider());
        socialLinkEntity.setSocialUserId(socialLink.getSocialUserId());
        socialLinkEntity.setSocialUsername(socialLink.getSocialUsername());

        getMongoStore().pushItemToList(userEntity, "socialLinks", socialLinkEntity, true, invocationContext);
    }

    @Override
    public boolean removeSocialLink(UserModel user,String socialProvider) {
        SocialLinkEntity socialLinkEntity = findSocialLink(user, socialProvider);
        if (socialLinkEntity == null) {
            return false;
        }
        UserEntity userEntity = ((UserAdapter)user).getUser();

        return getMongoStore().pullItemFromList(userEntity, "socialLinks", socialLinkEntity, invocationContext);
    }

    private SocialLinkEntity findSocialLink(UserModel user, String socialProvider) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        List<SocialLinkEntity> linkEntities = userEntity.getSocialLinks();
        if (linkEntities == null) {
            return null;
        }

        for (SocialLinkEntity socialLinkEntity : linkEntities) {
            if (socialLinkEntity.getSocialProvider().equals(socialProvider)) {
                return socialLinkEntity;
            }
        }
        return null;
    }

    @Override
    public AuthenticationLinkModel getAuthenticationLink(UserModel user) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        AuthenticationLinkEntity authLinkEntity = userEntity.getAuthenticationLink();

        if (authLinkEntity == null) {
            return null;
        }  else {
            return new AuthenticationLinkModel(authLinkEntity.getAuthProvider(), authLinkEntity.getAuthUserId());
        }
    }

    @Override
    public void setAuthenticationLink(UserModel user, AuthenticationLinkModel authenticationLink) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        AuthenticationLinkEntity authLinkEntity = new AuthenticationLinkEntity();
        authLinkEntity.setAuthProvider(authenticationLink.getAuthProvider());
        authLinkEntity.setAuthUserId(authenticationLink.getAuthUserId());
        userEntity.setAuthenticationLink(authLinkEntity);

        getMongoStore().updateEntity(userEntity, invocationContext);
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
    public List<UserModel> getUsers() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();
        List<UserEntity> users = getMongoStore().loadEntities(UserEntity.class, query, invocationContext);
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

        List<UserEntity> users = getMongoStore().loadEntities(UserEntity.class, builder.get(), invocationContext);
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
        List<UserEntity> users = getMongoStore().loadEntities(UserEntity.class, queryBuilder.get(), invocationContext);
        return convertUserEntities(users);
    }

    protected List<UserModel> convertUserEntities(List<UserEntity> userEntities) {
        List<UserModel> userModels = new ArrayList<UserModel>();
        for (UserEntity user : userEntities) {
            userModels.add(new UserAdapter(user, invocationContext));
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
    public Map<String, String> getLdapServerConfig() {
        return realm.getLdapServerConfig();
    }

    @Override
    public void setLdapServerConfig(Map<String, String> ldapServerConfig) {
        realm.setLdapServerConfig(ldapServerConfig);
        updateRealm();
    }

    @Override
    public List<AuthenticationProviderModel> getAuthenticationProviders() {
        List<AuthenticationProviderEntity> entities = realm.getAuthenticationProviders();
        List<AuthenticationProviderModel> result = new ArrayList<AuthenticationProviderModel>();
        for (AuthenticationProviderEntity entity : entities) {
            result.add(new AuthenticationProviderModel(entity.getProviderName(), entity.isPasswordUpdateSupported(), entity.getConfig()));
        }

        return result;
    }

    @Override
    public void setAuthenticationProviders(List<AuthenticationProviderModel> authenticationProviders) {
        List<AuthenticationProviderEntity> entities = new ArrayList<AuthenticationProviderEntity>();
        for (AuthenticationProviderModel model : authenticationProviders) {
            AuthenticationProviderEntity entity = new AuthenticationProviderEntity();
            entity.setProviderName(model.getProviderName());
            entity.setPasswordUpdateSupported(model.isPasswordUpdateSupported());
            entity.setConfig(model.getConfig());
            entities.add(entity);
        }

        realm.setAuthenticationProviders(entities);
        updateRealm();
    }

    @Override
    public Set<String> getAuditListeners() {
        return realm.getAuditListeners() != null ? new HashSet<String>(realm.getAuditListeners()) : null;
    }

    @Override
    public void setAuditListeners(Set<String> listeners) {
         if (listeners != null) {
             realm.setAuditListeners(new LinkedList<String>(listeners));
         } else {
             realm.setAuditListeners(null);
         }
    }

    @Override
    public RealmEntity getMongoEntity() {
        return realm;
    }
}
