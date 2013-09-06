package org.keycloak.services.models.nosql.keycloak.data;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;

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
    private boolean social;
    private boolean automaticRegistrationAfterSocialLogin;
    private int tokenLifespan;
    private int accessCodeLifespan;
    private String publicKeyPem;
    private String privateKeyPem;

    private String[] defaultRoles;
    private String[] realmAdmins;

    @NoSQLId
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    // TODO: Is ID really needed? It seems that it exists just to workaround picketlink...
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
    public String[] getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(String[] defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    @NoSQLField
    public String[] getRealmAdmins() {
        return realmAdmins;
    }

    public void setRealmAdmins(String[] realmAdmins) {
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
