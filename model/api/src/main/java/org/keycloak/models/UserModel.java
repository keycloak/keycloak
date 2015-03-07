package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserModel {
    public static final String USERNAME = "username";
    public static final String LAST_NAME = "lastName";
    public static final String FIRST_NAME = "firstName";
    public static final String EMAIL = "email";
    public static final String LOCALE = "locale";

    String getId();

    String getUsername();

    void setUsername(String username);

    boolean isEnabled();

    boolean isTotp();

    void setEnabled(boolean enabled);

    void setAttribute(String name, String value);

    void removeAttribute(String name);

    String getAttribute(String name);

    Map<String, String> getAttributes();

    Set<RequiredAction> getRequiredActions();
    
    void addRequiredAction(RequiredAction action);

    void removeRequiredAction(RequiredAction action);

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    String getEmail();

    void setEmail(String email);

    boolean isEmailVerified();

    void setEmailVerified(boolean verified);

    void setTotp(boolean totp);

    void updateCredential(UserCredentialModel cred);

    List<UserCredentialValueModel> getCredentialsDirectly();

    void updateCredentialDirectly(UserCredentialValueModel cred);

    Set<RoleModel> getRealmRoleMappings();
    Set<RoleModel> getApplicationRoleMappings(ApplicationModel app);
    boolean hasRole(RoleModel role);
    void grantRole(RoleModel role);
    Set<RoleModel> getRoleMappings();
    void deleteRoleMapping(RoleModel role);

    String getFederationLink();
    void setFederationLink(String link);





    public static enum RequiredAction {
        VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD
    }
}