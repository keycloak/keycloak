package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "realms")
public class RealmEntity extends AbstractMongoIdentifiableEntity implements MongoEntity {

    private String name;
    private boolean enabled;
    private boolean sslNotRequired;
    private boolean registrationAllowed;
    private boolean verifyEmail;
    private boolean resetPasswordAllowed;
    private boolean social;
    private boolean updateProfileOnInitialSocialLogin;
    private String passwordPolicy;

    private int tokenLifespan;
    private int accessCodeLifespan;
    private int accessCodeLifespanUserAction;

    private String publicKeyPem;
    private String privateKeyPem;

    private String loginTheme;
    private String accountTheme;

    // We are using names of defaultRoles (not ids)
    private List<String> defaultRoles = new ArrayList<String>();

    private List<RequiredCredentialEntity> requiredCredentials = new ArrayList<RequiredCredentialEntity>();
    private List<RequiredCredentialEntity> requiredApplicationCredentials = new ArrayList<RequiredCredentialEntity>();
    private List<RequiredCredentialEntity> requiredOAuthClientCredentials = new ArrayList<RequiredCredentialEntity>();

    private Map<String, String> smtpConfig = new HashMap<String, String>();
    private Map<String, String> socialConfig = new HashMap<String, String>();

    @MongoField
    public String getName() {
        return name;
    }

    public void setName(String realmName) {
        this.name = realmName;
    }

    @MongoField
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @MongoField
    public boolean isSslNotRequired() {
        return sslNotRequired;
    }

    public void setSslNotRequired(boolean sslNotRequired) {
        this.sslNotRequired = sslNotRequired;
    }

    @MongoField
    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    @MongoField
    public boolean isVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    @MongoField
    public boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    @MongoField
    public boolean isSocial() {
        return social;
    }

    public void setSocial(boolean social) {
        this.social = social;
    }

    @MongoField
    public boolean isUpdateProfileOnInitialSocialLogin() {
        return updateProfileOnInitialSocialLogin;
    }

    public void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin) {
        this.updateProfileOnInitialSocialLogin = updateProfileOnInitialSocialLogin;
    }

    @MongoField
    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    @MongoField
    public int getTokenLifespan() {
        return tokenLifespan;
    }

    public void setTokenLifespan(int tokenLifespan) {
        this.tokenLifespan = tokenLifespan;
    }

    @MongoField
    public int getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(int accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    @MongoField
    public int getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        this.accessCodeLifespanUserAction = accessCodeLifespanUserAction;
    }

    @MongoField
    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    @MongoField
    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    @MongoField
    public String getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(String loginTheme) {
        this.loginTheme = loginTheme;
    }

    @MongoField
    public String getAccountTheme() {
        return accountTheme;
    }

    public void setAccountTheme(String accountTheme) {
        this.accountTheme = accountTheme;
    }

    @MongoField
    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    @MongoField
    public List<RequiredCredentialEntity> getRequiredCredentials() {
        return requiredCredentials;
    }

    public void setRequiredCredentials(List<RequiredCredentialEntity> requiredCredentials) {
        this.requiredCredentials = requiredCredentials;
    }

    @MongoField
    public List<RequiredCredentialEntity> getRequiredApplicationCredentials() {
        return requiredApplicationCredentials;
    }

    public void setRequiredApplicationCredentials(List<RequiredCredentialEntity> requiredApplicationCredentials) {
        this.requiredApplicationCredentials = requiredApplicationCredentials;
    }

    @MongoField
    public List<RequiredCredentialEntity> getRequiredOAuthClientCredentials() {
        return requiredOAuthClientCredentials;
    }

    public void setRequiredOAuthClientCredentials(List<RequiredCredentialEntity> requiredOAuthClientCredentials) {
        this.requiredOAuthClientCredentials = requiredOAuthClientCredentials;
    }

    @MongoField
    public Map<String, String> getSmtpConfig() {
        return smtpConfig;
    }

    public void setSmtpConfig(Map<String, String> smptConfig) {
        this.smtpConfig = smptConfig;
    }

    @MongoField
    public Map<String, String> getSocialConfig() {
        return socialConfig;
    }

    public void setSocialConfig(Map<String, String> socialConfig) {
        this.socialConfig = socialConfig;
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();

        // Remove all users of this realm
        context.getMongoStore().removeEntities(UserEntity.class, query, context);

        // Remove all roles of this realm
        context.getMongoStore().removeEntities(RoleEntity.class, query, context);

        // Remove all applications of this realm
        context.getMongoStore().removeEntities(ApplicationEntity.class, query, context);
    }
}
