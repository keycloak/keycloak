package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.MongoIndex;
import org.keycloak.models.mongo.api.MongoIndexes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "users")
@MongoIndexes({
        @MongoIndex(fields = { "realmId", "loginName" }, unique = true),
        @MongoIndex(fields = { "emailIndex" }, unique = true, sparse = true),
})
public class UserEntity extends AbstractMongoIdentifiableEntity implements MongoEntity {

    private String loginName;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private boolean totp;
    private boolean enabled;
    private int notBefore;
    private int failedLoginNotBefore;
    private int numFailures;
    private long lastFailure;
    private String lastIPFailure;


    private String realmId;

    private List<String> roleIds;

    private Map<String, String> attributes;
    private List<UserModel.RequiredAction> requiredActions;
    private List<CredentialEntity> credentials = new ArrayList<CredentialEntity>();
    private List<SocialLinkEntity> socialLinks;
    private AuthenticationLinkEntity authenticationLink;

    @MongoField
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    @MongoField
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @MongoField
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @MongoField
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @MongoField
    // TODO This is required as Mongo doesn't support sparse indexes with compound keys (see https://jira.mongodb.org/browse/SERVER-2193)
    public String getEmailIndex() {
        return email != null ? realmId + "//" + email : null;
    }

    public void setEmailIndex(String ignored) {
    }

    @MongoField
    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @MongoField
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @MongoField
    public boolean isTotp() {
        return totp;
    }

    public void setTotp(boolean totp) {
        this.totp = totp;
    }

    @MongoField
    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    @MongoField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @MongoField
    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }


    @MongoField
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @MongoField
    public List<UserModel.RequiredAction> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<UserModel.RequiredAction> requiredActions) {
        this.requiredActions = requiredActions;
    }

    @MongoField
    public List<CredentialEntity> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialEntity> credentials) {
        this.credentials = credentials;
    }

    @MongoField
    public List<SocialLinkEntity> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(List<SocialLinkEntity> socialLinks) {
        this.socialLinks = socialLinks;
    }

    @MongoField
    public AuthenticationLinkEntity getAuthenticationLink() {
        return authenticationLink;
    }

    public void setAuthenticationLink(AuthenticationLinkEntity authenticationLink) {
        this.authenticationLink = authenticationLink;
    }
}
