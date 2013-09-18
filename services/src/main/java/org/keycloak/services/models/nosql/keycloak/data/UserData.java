package org.keycloak.services.models.nosql.keycloak.data;

import java.util.List;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.nosql.api.AbstractAttributedNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;
import org.keycloak.services.models.nosql.keycloak.data.credentials.PasswordData;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "users")
public class UserData extends AbstractAttributedNoSQLObject {

    private static final Logger logger = Logger.getLogger(UserData.class);

    private String id;
    private String loginName;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private boolean totp;
    private UserModel.Status status;

    private String realmId;

    private List<String> roleIds;
    private List<String> scopeIds;
    private List<UserModel.RequiredAction> requiredActions;

    @NoSQLId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NoSQLField
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    @NoSQLField
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @NoSQLField
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @NoSQLField
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @NoSQLField
    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isEnabled() {
        return !UserModel.Status.DISABLED.equals(getStatus());
    }

    @NoSQLField
    public boolean isTotp() {
        return totp;
    }

    public void setTotp(boolean totp) {
        this.totp = totp;
    }

    @NoSQLField
    public UserModel.Status getStatus() {
        return status;
    }

    public void setStatus(UserModel.Status status) {
        this.status = status;
    }

    @NoSQLField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @NoSQLField
    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }

    @NoSQLField
    public List<String> getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(List<String> scopeIds) {
        this.scopeIds = scopeIds;
    }

    @NoSQLField
    public List<UserModel.RequiredAction> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<UserModel.RequiredAction> requiredActions) {
        this.requiredActions = requiredActions;
    }

    @Override
    public void afterRemove(NoSQL noSQL) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("userId", id)
                .build();

        // Remove social links and passwords of this user
        noSQL.removeObjects(SocialLinkData.class, query);
        noSQL.removeObjects(PasswordData.class, query);

        // Remove this user from all realms, which have him as an admin
        NoSQLQuery realmQuery = noSQL.createQueryBuilder()
                .andCondition("realmAdmins", id)
                .build();

        List<RealmData> realms = noSQL.loadObjects(RealmData.class, realmQuery);
        for (RealmData realm : realms) {
            logger.info("Removing admin user " + getLoginName() + " from realm " + realm.getId());
            noSQL.pullItemFromList(realm, "realmAdmins", getId());
        }
    }
}
