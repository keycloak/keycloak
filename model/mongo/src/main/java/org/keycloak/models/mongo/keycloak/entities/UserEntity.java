package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.MongoId;
import org.keycloak.models.mongo.api.MongoStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "users")
public class UserEntity implements MongoEntity {

    private String id;
    private String loginName;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private boolean totp;
    private boolean enabled;

    private String realmId;

    private List<String> roleIds;
    private List<String> scopeIds;

    private Map<String, String> attributes;
    private List<String> webOrigins;
    private List<String> redirectUris;
    private List<UserModel.RequiredAction> requiredActions;
    private List<CredentialEntity> credentials = new ArrayList<CredentialEntity>();

    @MongoId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
    public List<String> getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(List<String> scopeIds) {
        this.scopeIds = scopeIds;
    }

    @MongoField
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @MongoField
    public List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(List<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    @MongoField
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
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

    @Override
    public void afterRemove(MongoStore mongoStore) {
        DBObject query = new QueryBuilder()
                .and("userId").is(id)
                .get();

        // Remove social links of this user
        mongoStore.removeObjects(SocialLinkEntity.class, query);
    }
}
