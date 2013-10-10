package org.keycloak.models.mongo.keycloak.adapters;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.keycloak.models.mongo.api.query.NoSQLQueryBuilder;
import org.keycloak.models.mongo.keycloak.data.OAuthClientData;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.query.NoSQLQuery;
import org.keycloak.models.mongo.keycloak.credentials.PasswordCredentialHandler;
import org.keycloak.models.mongo.keycloak.credentials.TOTPCredentialHandler;
import org.keycloak.models.mongo.keycloak.data.ApplicationData;
import org.keycloak.models.mongo.keycloak.data.RealmData;
import org.keycloak.models.mongo.keycloak.data.RequiredCredentialData;
import org.keycloak.models.mongo.keycloak.data.RoleData;
import org.keycloak.models.mongo.keycloak.data.SocialLinkData;
import org.keycloak.models.mongo.keycloak.data.UserData;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.model.sample.User;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmAdapter implements RealmModel {

    private final RealmData realm;
    private final NoSQL noSQL;

    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;

    // TODO: likely shouldn't be static. And ATM, just empty map is passed -> It's not possible to configure stuff like PasswordEncoder etc.
    private static PasswordCredentialHandler passwordCredentialHandler = new PasswordCredentialHandler(new HashMap<String, Object>());
    private static TOTPCredentialHandler totpCredentialHandler = new TOTPCredentialHandler(new HashMap<String, Object>());

    public RealmAdapter(RealmData realmData, NoSQL noSQL) {
        this.realm = realmData;
        this.noSQL = noSQL;
    }

    protected String getOid() {
        return realm.getOid();
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
    public boolean isSocial() {
        return realm.isSocial();
    }

    @Override
    public void setSocial(boolean social) {
        realm.setSocial(social);
        updateRealm();
    }

    @Override
    public boolean isAutomaticRegistrationAfterSocialLogin() {
        return realm.isAutomaticRegistrationAfterSocialLogin();
    }

    @Override
    public void setAutomaticRegistrationAfterSocialLogin(boolean automaticRegistrationAfterSocialLogin) {
        realm.setAutomaticRegistrationAfterSocialLogin(automaticRegistrationAfterSocialLogin);
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
    public boolean isCookieLoginAllowed() {
        return realm.isCookieLoginAllowed();
    }

    @Override
    public void setCookieLoginAllowed(boolean cookieLoginAllowed) {
        realm.setCookieLoginAllowed(cookieLoginAllowed);
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

    @Override
    public UserAdapter getUser(String name) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("loginName", name)
                .andCondition("realmId", getOid())
                .build();
        UserData user = noSQL.loadSingleObject(UserData.class, query);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(user, noSQL);
        }
    }

    @Override
    public UserAdapter addUser(String username) {
        if (getUser(username) != null) {
            throw new IllegalArgumentException("User " + username + " already exists");
        }

        UserData userData = new UserData();
        userData.setLoginName(username);
        userData.setEnabled(true);
        userData.setRealmId(getOid());

        noSQL.saveObject(userData);
        return new UserAdapter(userData, noSQL);
    }

    // This method doesn't exists on interface actually
    public void removeUser(String name) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("loginName", name)
                .andCondition("realmId", getOid())
                .build();
        noSQL.removeObjects(UserData.class, query);
    }

    @Override
    public RoleAdapter getRole(String name) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("name", name)
                .andCondition("realmId", getOid())
                .build();
        RoleData role = noSQL.loadSingleObject(RoleData.class, query);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, noSQL);
        }
    }

    @Override
    public RoleModel addRole(String name) {
        if (getRole(name) != null) {
            throw new IllegalArgumentException("Role " + name + " already exists");
        }

        RoleData roleData = new RoleData();
        roleData.setName(name);
        roleData.setRealmId(getOid());

        noSQL.saveObject(roleData);
        return new RoleAdapter(roleData, noSQL);
    }

    @Override
    public List<RoleModel> getRoles() {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("realmId", getOid())
                .build();
        List<RoleData> roles = noSQL.loadObjects(RoleData.class, query);

        List<RoleModel> result = new ArrayList<RoleModel>();
        for (RoleData role : roles) {
            result.add(new RoleAdapter(role, noSQL));
        }

        return result;
    }

    @Override
    public List<RoleModel> getDefaultRoles() {
        List<String> defaultRoles = realm.getDefaultRoles();

        NoSQLQuery query = noSQL.createQueryBuilder()
                .inCondition("_id", defaultRoles)
                .build();
        List<RoleData> defaultRolesData = noSQL.loadObjects(RoleData.class, query);

        List<RoleModel> defaultRoleModels = new ArrayList<RoleModel>();
        for (RoleData roleData : defaultRolesData) {
            defaultRoleModels.add(new RoleAdapter(roleData, noSQL));
        }
        return defaultRoleModels;
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            role = addRole(name);
        }

        noSQL.pushItemToList(realm, "defaultRoles", role.getId());
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        // defaultRoles is array with names of roles. So we need to convert to array of ids
        List<String> roleIds = new ArrayList<String>();
        for (String roleName : defaultRoles) {
            RoleModel role = getRole(roleName);
            if (role == null) {
                role = addRole(roleName);
            }

            roleIds.add(role.getId());
        }

        realm.setDefaultRoles(roleIds);
        updateRealm();
    }

    @Override
    public ApplicationModel getApplicationById(String id) {
        ApplicationData appData = noSQL.loadObject(ApplicationData.class, id);

        // Check if application belongs to this realm
        if (appData == null || !getOid().equals(appData.getRealmId())) {
            return null;
        }

        ApplicationModel model = new ApplicationAdapter(appData, noSQL);
        return model;
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
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("realmId", getOid())
                .build();
        List<ApplicationData> appDatas = noSQL.loadObjects(ApplicationData.class, query);

        List<ApplicationModel> result = new ArrayList<ApplicationModel>();
        for (ApplicationData appData : appDatas) {
            result.add(new ApplicationAdapter(appData, noSQL));
        }
        return result;
    }

    @Override
    public ApplicationModel addApplication(String name) {
        UserAdapter resourceUser = addUser(name);

        ApplicationData appData = new ApplicationData();
        appData.setName(name);
        appData.setRealmId(getOid());
        appData.setResourceUserId(resourceUser.getUser().getId());
        noSQL.saveObject(appData);

        ApplicationModel resource = new ApplicationAdapter(appData, noSQL);
        resource.addRole("*");
        resource.addScopeMapping(resourceUser, "*");
        return resource;
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        UserData userData = ((UserAdapter)user).getUser();

        List<String> roleIds = userData.getRoleIds();
        String roleId = role.getId();
        if (roleIds != null) {
            for (String currentId : roleIds) {
                if (roleId.equals(currentId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        UserData userData = ((UserAdapter)user).getUser();
        noSQL.pushItemToList(userData, "roleIds", role.getId());
    }

    @Override
    public List<RoleModel> getRoleMappings(UserModel user) {
        List<RoleModel> result = new ArrayList<RoleModel>();
        List<RoleData> roles = ApplicationAdapter.getAllRolesOfUser(user, noSQL);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getOid().equals(role.getRealmId())) {
                result.add(new RoleAdapter(role, noSQL));
            }
        }
        return result;
    }

    @Override
    public Set<String> getRoleMappingValues(UserModel user) {
        Set<String> result = new HashSet<String>();
        List<RoleData> roles = ApplicationAdapter.getAllRolesOfUser(user, noSQL);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getOid().equals(role.getRealmId())) {
                result.add(role.getName());
            }
        }
        return result;
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        UserData userData = ((UserAdapter)user).getUser();
        noSQL.pullItemFromList(userData, "roleIds", role.getId());
    }

    @Override
    public void addScopeMapping(UserModel agent, String roleName) {
        RoleAdapter role = getRole(roleName);
        if (role == null) {
            throw new RuntimeException("Role not found");
        }

        addScopeMapping(agent, role);
    }

    @Override
    public void addScopeMapping(UserModel agent, RoleModel role) {
        UserData userData = ((UserAdapter)agent).getUser();
        noSQL.pushItemToList(userData, "scopeIds", role.getId());
    }

    @Override
    public void deleteScopeMapping(UserModel user, RoleModel role) {
        UserData userData = ((UserAdapter)user).getUser();
        noSQL.pullItemFromList(userData, "scopeIds", role.getId());
    }

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        UserAdapter oauthAgent = addUser(name);

        OAuthClientData oauthClient = new OAuthClientData();
        oauthClient.setOauthAgentId(oauthAgent.getUser().getId());
        oauthClient.setRealmId(getOid());
        noSQL.saveObject(oauthClient);

        return new OAuthClientAdapter(oauthClient, oauthAgent, noSQL);
    }

    @Override
    public OAuthClientModel getOAuthClient(String name) {
        UserAdapter user = getUser(name);
        if (user == null) return null;
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("realmId", getOid())
                .andCondition("oauthAgentId", user.getUser().getId())
                .build();
        OAuthClientData oauthClient = noSQL.loadSingleObject(OAuthClientData.class, query);
        return oauthClient == null ? null : new OAuthClientAdapter(oauthClient, user, noSQL);
    }

    @Override
    public List<OAuthClientModel> getOAuthClients() {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("realmId", getOid())
                .build();
        List<OAuthClientData> results = noSQL.loadObjects(OAuthClientData.class, query);
        List<OAuthClientModel> list = new ArrayList<OAuthClientModel>();
        for (OAuthClientData data : results) {
            list.add(new OAuthClientAdapter(data, noSQL));
        }
        return list;
    }

    @Override
    public List<RoleModel> getScopeMappings(UserModel agent) {
        List<RoleModel> result = new ArrayList<RoleModel>();
        List<RoleData> roles = ApplicationAdapter.getAllScopesOfUser(agent, noSQL);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getOid().equals(role.getRealmId())) {
                result.add(new RoleAdapter(role, noSQL));
            }
        }
        return result;
    }

    @Override
    public Set<String> getScopeMappingValues(UserModel agent) {
        Set<String> result = new HashSet<String>();
        List<RoleData> roles = ApplicationAdapter.getAllScopesOfUser(agent, noSQL);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getOid().equals(role.getRealmId())) {
                result.add(role.getName());
            }
        }
        return result;
    }

    @Override
    public boolean isRealmAdmin(UserModel agent) {
        List<String> realmAdmins = realm.getRealmAdmins();
        String userId = ((UserAdapter)agent).getUser().getId();
        return realmAdmins.contains(userId);
    }

    @Override
    public void addRealmAdmin(UserModel agent) {
        UserData userData = ((UserAdapter)agent).getUser();

        noSQL.pushItemToList(realm, "realmAdmins", userData.getId());
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleData role = noSQL.loadObject(RoleData.class, id);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, noSQL);
        }
    }

    @Override
    public boolean hasRole(UserModel user, String role) {
        RoleModel roleModel = getRole(role);
        return hasRole(user, roleModel);
    }

    @Override
    public void addRequiredCredential(String cred) {
        RequiredCredentialModel credentialModel = initRequiredCredentialModel(cred);
        addRequiredCredential(credentialModel, RequiredCredentialData.CLIENT_TYPE_USER);
    }

    @Override
    public void addRequiredResourceCredential(String type) {
        RequiredCredentialModel credentialModel = initRequiredCredentialModel(type);
        addRequiredCredential(credentialModel, RequiredCredentialData.CLIENT_TYPE_RESOURCE);
    }

    @Override
    public void addRequiredOAuthClientCredential(String type) {
        RequiredCredentialModel credentialModel = initRequiredCredentialModel(type);
        addRequiredCredential(credentialModel, RequiredCredentialData.CLIENT_TYPE_OAUTH_RESOURCE);
    }

    protected void addRequiredCredential(RequiredCredentialModel credentialModel, int clientType) {
        RequiredCredentialData credData = new RequiredCredentialData();
        credData.setType(credentialModel.getType());
        credData.setFormLabel(credentialModel.getFormLabel());
        credData.setInput(credentialModel.isInput());
        credData.setSecret(credentialModel.isSecret());

        credData.setRealmId(getOid());
        credData.setClientType(clientType);

        noSQL.saveObject(credData);
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        List<RequiredCredentialData> credsData = getRequiredCredentialsData(RequiredCredentialData.CLIENT_TYPE_USER);
        updateRequiredCredentials(creds, credsData);
    }

    @Override
    public void updateRequiredApplicationCredentials(Set<String> creds) {
        List<RequiredCredentialData> credsData = getRequiredCredentialsData(RequiredCredentialData.CLIENT_TYPE_RESOURCE);
        updateRequiredCredentials(creds, credsData);
    }

    @Override
    public void updateRequiredOAuthClientCredentials(Set<String> creds) {
        List<RequiredCredentialData> credsData = getRequiredCredentialsData(RequiredCredentialData.CLIENT_TYPE_OAUTH_RESOURCE);
        updateRequiredCredentials(creds, credsData);
    }

    protected void updateRequiredCredentials(Set<String> creds, List<RequiredCredentialData> credsData) {
        Set<String> already = new HashSet<String>();
        for (RequiredCredentialData data : credsData) {
            if (!creds.contains(data.getType())) {
                noSQL.removeObject(data);
            } else {
                already.add(data.getType());
            }
        }
        for (String cred : creds) {
            // TODO
            System.out.println("updating cred: " + cred);
            // logger.info("updating cred: " + cred);
            if (!already.contains(cred)) {
                addRequiredCredential(cred);
            }
        }
    }

    @Override
    public List<RequiredCredentialModel> getRequiredCredentials() {
        return getRequiredCredentials(RequiredCredentialData.CLIENT_TYPE_USER);
    }

    @Override
    public List<RequiredCredentialModel> getRequiredApplicationCredentials() {
        return getRequiredCredentials(RequiredCredentialData.CLIENT_TYPE_RESOURCE);
    }

    @Override
    public List<RequiredCredentialModel> getRequiredOAuthClientCredentials() {
        return getRequiredCredentials(RequiredCredentialData.CLIENT_TYPE_OAUTH_RESOURCE);
    }

    protected List<RequiredCredentialModel> getRequiredCredentials(int credentialType) {
        List<RequiredCredentialData> credsData = getRequiredCredentialsData(credentialType);

        List<RequiredCredentialModel> result = new ArrayList<RequiredCredentialModel>();
        for (RequiredCredentialData data : credsData) {
            RequiredCredentialModel model = new RequiredCredentialModel();
            model.setFormLabel(data.getFormLabel());
            model.setInput(data.isInput());
            model.setSecret(data.isSecret());
            model.setType(data.getType());

            result.add(model);
        }
        return result;
    }

    protected List<RequiredCredentialData> getRequiredCredentialsData(int credentialType) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("realmId", getOid())
                .andCondition("clientType", credentialType)
                .build();
        return noSQL.loadObjects(RequiredCredentialData.class, query);
    }

    @Override
    public boolean validatePassword(UserModel user, String password) {
        Credentials.Status status = passwordCredentialHandler.validate(noSQL, ((UserAdapter)user).getUser(), password);
        return status == Credentials.Status.VALID;
    }

    @Override
    public boolean validateTOTP(UserModel user, String password, String token) {
        Credentials.Status status = totpCredentialHandler.validate(noSQL, ((UserAdapter)user).getUser(), password, token, null);
        return status == Credentials.Status.VALID;
    }

    @Override
    public void updateCredential(UserModel user, UserCredentialModel cred) {
        if (cred.getType().equals(CredentialRepresentation.PASSWORD)) {
            passwordCredentialHandler.update(noSQL, ((UserAdapter)user).getUser(), cred.getValue(), null, null);
        } else if (cred.getType().equals(CredentialRepresentation.TOTP)) {
            totpCredentialHandler.update(noSQL, ((UserAdapter)user).getUser(), cred.getValue(), cred.getDevice(), null, null);
        } else if (cred.getType().equals(CredentialRepresentation.CLIENT_CERT)) {
            // TODO
//            X509Certificate cert = null;
//            try {
//                cert = org.keycloak.PemUtils.decodeCertificate(cred.getValue());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            X509CertificateCredentials creds = new X509CertificateCredentials(cert);
//            idm.updateCredential(((UserAdapter)user).getUser(), creds);
        }
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("socialProvider", socialLink.getSocialProvider())
                .andCondition("socialUsername", socialLink.getSocialUsername())
                .andCondition("realmId", getOid())
                .build();
        SocialLinkData socialLinkData = noSQL.loadSingleObject(SocialLinkData.class, query);

        if (socialLinkData == null) {
            return null;
        } else {
            UserData userData = noSQL.loadObject(UserData.class, socialLinkData.getUserId());
            // TODO: Add some checking if userData exists and programmatically remove binding if it doesn't? (There are more similar places where this should be handled)
            return new UserAdapter(userData, noSQL);
        }
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user) {
        UserData userData = ((UserAdapter)user).getUser();
        String userId = userData.getId();

        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("userId", userId)
                .build();
        List<SocialLinkData> dbSocialLinks = noSQL.loadObjects(SocialLinkData.class, query);

        Set<SocialLinkModel> result = new HashSet<SocialLinkModel>();
        for (SocialLinkData socialLinkData : dbSocialLinks) {
            SocialLinkModel model = new SocialLinkModel(socialLinkData.getSocialProvider(), socialLinkData.getSocialUsername());
            result.add(model);
        }
        return result;
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        UserData userData = ((UserAdapter)user).getUser();
        SocialLinkData socialLinkData = new SocialLinkData();
        socialLinkData.setSocialProvider(socialLink.getSocialProvider());
        socialLinkData.setSocialUsername(socialLink.getSocialUsername());
        socialLinkData.setUserId(userData.getId());
        socialLinkData.setRealmId(getOid());

        noSQL.saveObject(socialLinkData);
    }

    @Override
    public void removeSocialLink(UserModel user, SocialLinkModel socialLink) {
        UserData userData = ((UserAdapter)user).getUser();
        String userId = userData.getId();
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("socialProvider", socialLink.getSocialProvider())
                .andCondition("socialUsername", socialLink.getSocialUsername())
                .andCondition("userId", userId)
                .build();
        noSQL.removeObjects(SocialLinkData.class, query);
    }

    protected void updateRealm() {
        noSQL.saveObject(realm);
    }

    protected RequiredCredentialModel initRequiredCredentialModel(String type) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(type);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + type);
        }
        return model;
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes) {
        NoSQLQueryBuilder queryBuilder = noSQL.createQueryBuilder();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equals(UserModel.LOGIN_NAME)) {
                queryBuilder.andCondition("loginName", entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                queryBuilder.andCondition(UserModel.FIRST_NAME, entry.getValue());

            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                queryBuilder.andCondition(UserModel.LAST_NAME, entry.getValue());

            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                queryBuilder.andCondition(UserModel.EMAIL, entry.getValue());
            }
        }
        List<UserData> users = noSQL.loadObjects(UserData.class, queryBuilder.build());
        List<UserModel> userModels = new ArrayList<UserModel>();
        for (UserData user : users) {
            userModels.add(new UserAdapter(user, noSQL));
        }
        return userModels;
    }
}
