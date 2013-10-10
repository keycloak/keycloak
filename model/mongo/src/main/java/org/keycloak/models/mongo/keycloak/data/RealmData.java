package org.keycloak.models.mongo.keycloak.data;

import java.util.List;

import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.NoSQLCollection;
import org.keycloak.models.mongo.api.NoSQLField;
import org.keycloak.models.mongo.api.NoSQLId;
import org.keycloak.models.mongo.api.NoSQLObject;
import org.keycloak.models.mongo.api.query.NoSQLQuery;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "realms")
public class RealmData implements NoSQLObject {

    private String oid;

    private String id;
    private String name;
    private boolean enabled;
    private boolean sslNotRequired;
    private boolean cookieLoginAllowed;
    private boolean registrationAllowed;
    private boolean verifyEmail;
    private boolean resetPasswordAllowed;
    private boolean social;
    private boolean automaticRegistrationAfterSocialLogin;
    private int tokenLifespan;
    private int accessCodeLifespan;
    private int accessCodeLifespanUserAction;
    private String publicKeyPem;
    private String privateKeyPem;

    private List<String> defaultRoles;
    private List<String> realmAdmins;

    @NoSQLId
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    @NoSQLField
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NoSQLField
    public String getName() {
        return name;
    }

    public void setName(String realmName) {
        this.name = realmName;
    }

    @NoSQLField
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NoSQLField
    public boolean isSslNotRequired() {
        return sslNotRequired;
    }

    public void setSslNotRequired(boolean sslNotRequired) {
        this.sslNotRequired = sslNotRequired;
    }

    @NoSQLField
    public boolean isCookieLoginAllowed() {
        return cookieLoginAllowed;
    }

    public void setCookieLoginAllowed(boolean cookieLoginAllowed) {
        this.cookieLoginAllowed = cookieLoginAllowed;
    }

    @NoSQLField
    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    @NoSQLField
    public boolean isVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    @NoSQLField
    public boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    @NoSQLField
    public boolean isSocial() {
        return social;
    }

    public void setSocial(boolean social) {
        this.social = social;
    }

    @NoSQLField
    public boolean isAutomaticRegistrationAfterSocialLogin() {
        return automaticRegistrationAfterSocialLogin;
    }

    public void setAutomaticRegistrationAfterSocialLogin(boolean automaticRegistrationAfterSocialLogin) {
        this.automaticRegistrationAfterSocialLogin = automaticRegistrationAfterSocialLogin;
    }

    @NoSQLField
    public int getTokenLifespan() {
        return tokenLifespan;
    }

    public void setTokenLifespan(int tokenLifespan) {
        this.tokenLifespan = tokenLifespan;
    }

    @NoSQLField
    public int getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(int accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    @NoSQLField
    public int getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        this.accessCodeLifespanUserAction = accessCodeLifespanUserAction;
    }

    @NoSQLField
    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    @NoSQLField
    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    @NoSQLField
    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    @NoSQLField
    public List<String> getRealmAdmins() {
        return realmAdmins;
    }

    public void setRealmAdmins(List<String> realmAdmins) {
        this.realmAdmins = realmAdmins;
    }

    @Override
    public void afterRemove(NoSQL noSQL) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("realmId", oid)
                .build();

        // Remove all users of this realm
        noSQL.removeObjects(UserData.class, query);

        // Remove all requiredCredentials of this realm
        noSQL.removeObjects(RequiredCredentialData.class, query);

        // Remove all roles of this realm
        noSQL.removeObjects(RoleData.class, query);

        // Remove all applications of this realm
        noSQL.removeObjects(ApplicationData.class, query);
    }
}
