package org.keycloak.services.models.nosql.keycloak.data;

import org.keycloak.services.models.nosql.api.AbstractAttributedNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "users")
public class UserData extends AbstractAttributedNoSQLObject {

    private String id;
    private String loginName;
    private String firstName;
    private String lastName;
    private String email;
    private boolean enabled;

    private String realmId;

    private String[] roleIds;
    private String[] scopeIds;

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
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NoSQLField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @NoSQLField
    public String[] getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(String[] roleIds) {
        this.roleIds = roleIds;
    }

    @NoSQLField
    public String[] getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(String[] scopeIds) {
        this.scopeIds = scopeIds;
    }
}
