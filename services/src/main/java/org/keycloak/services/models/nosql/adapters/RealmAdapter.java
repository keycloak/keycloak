package org.keycloak.services.models.nosql.adapters;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.openssl.PEMWriter;
import org.jboss.resteasy.security.PemUtils;
import org.keycloak.services.models.ApplicationModel;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.SocialLinkModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLQuery;
import org.keycloak.services.models.nosql.data.RealmData;
import org.keycloak.services.models.nosql.data.RoleData;
import org.keycloak.services.models.nosql.data.UserData;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmAdapter implements RealmModel {

    private final RealmData realm;
    private final NoSQL noSQL;

    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;

    public RealmAdapter(RealmData realmData, NoSQL noSQL) {
        this.realm = realmData;
        this.noSQL = noSQL;
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
    public List<RequiredCredentialModel> getRequiredCredentials() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addRequiredCredential(String cred) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean validatePassword(UserModel user, String password) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean validateTOTP(UserModel user, String password, String token) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateCredential(UserModel user, UserCredentialModel cred) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel getUser(String name) {
        NoSQLQuery query = NoSQLQuery.create().put("loginName", name).put("realmId", getId());
        UserData user = noSQL.loadSingleObject(UserData.class, query);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(user, noSQL);
        }
    }

    @Override
    public UserModel addUser(String username) {
        if (getUser(username) != null) {
            throw new IllegalArgumentException("User " + username + " already exists");
        }

        UserData userData = new UserData();
        userData.setLoginName(username);
        userData.setEnabled(true);
        userData.setRealmId(getId());

        noSQL.saveObject(userData);
        return new UserAdapter(userData, noSQL);
    }

    @Override
    public RoleAdapter getRole(String name) {
        NoSQLQuery query = NoSQLQuery.create().put("name", name).put("realmId", getId());
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
        roleData.setRealmId(getId());

        noSQL.saveObject(roleData);
        return new RoleAdapter(roleData, noSQL);
    }

    @Override
    public List<RoleModel> getRoles() {
        NoSQLQuery query = NoSQLQuery.create().put("realmId", getId());
        List<RoleData> roles = noSQL.loadObjects(RoleData.class, query);

        List<RoleModel> result = new ArrayList<RoleModel>();
        for (RoleData role : roles) {
            result.add(new RoleAdapter(role, noSQL));
        }

        return result;
    }

    @Override
    public List<RoleModel> getDefaultRoles() {
        List<RoleModel> defaultRoleModels = new ArrayList<RoleModel>();
        if (realm.getDefaultRoles() != null) {
            for (String name : realm.getDefaultRoles()) {
                RoleAdapter role = getRole(name);
                if (role != null) {
                    defaultRoleModels.add(role);
                }
            }
        }
        return defaultRoleModels;
    }

    @Override
    public void addDefaultRole(String name) {
        if (getRole(name) == null) {
            addRole(name);
        }

        String[] defaultRoles = realm.getDefaultRoles();
        if (defaultRoles == null) {
            defaultRoles = new String[1];
        } else {
            defaultRoles = Arrays.copyOf(defaultRoles, defaultRoles.length + 1);
        }
        defaultRoles[defaultRoles.length - 1] = name;

        realm.setDefaultRoles(defaultRoles);
        updateRealm();
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        for (String name : defaultRoles) {
            if (getRole(name) == null) {
                addRole(name);
            }
        }

        realm.setDefaultRoles(defaultRoles);
        updateRealm();
    }

    @Override
    public Map<String, ApplicationModel> getResourceNameMap() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ApplicationModel> getApplications() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel addApplication(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> getRoleMappings(UserModel user) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addScope(UserModel agent, String roleName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> getScope(UserModel agent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRealmAdmin(UserModel agent) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addRealmAdmin(UserModel agent) {
        //To change body of implemented methods use File | Settings | File Templates.
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
    public List<RequiredCredentialModel> getRequiredApplicationCredentials() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<RequiredCredentialModel> getRequiredOAuthClientCredentials() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasRole(UserModel user, String role) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel getApplicationById(String id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addRequiredOAuthClientCredential(String type) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addRequiredResourceCredential(String type) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateRequiredOAuthClientCredentials(Set<String> creds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateRequiredApplicationCredentials(Set<String> creds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeSocialLink(UserModel user, SocialLinkModel socialLink) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void updateRealm() {
        noSQL.saveObject(realm);
    }
}
