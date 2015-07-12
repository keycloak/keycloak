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
    
    /**
     * Get timestamp of user creation. May be null for old users created before this feature introduction.
     */
    Long getCreatedTimestamp();
    
    void setCreatedTimestamp(Long timestamp);

    boolean isEnabled();

    boolean isTotp();

    void setEnabled(boolean enabled);

    /**
     * Set single value of specified attribute. Remove all other existing values
     *
     * @param name
     * @param value
     */
    void setSingleAttribute(String name, String value);

    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);

    /**
     * @param name
     * @return null if there is not any value of specified attribute or first value otherwise. Don't throw exception if there are more values of the attribute
     */
    String getFirstAttribute(String name);

    /**
     * @param name
     * @return list of all attribute values or empty list if there are not any values. Never return null
     */
    List<String> getAttribute(String name);

    Map<String, List<String>> getAttributes();

    Set<String> getRequiredActions();

    void addRequiredAction(String action);

    void removeRequiredAction(String action);

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
    Set<RoleModel> getClientRoleMappings(ClientModel app);
    boolean hasRole(RoleModel role);
    void grantRole(RoleModel role);
    Set<RoleModel> getRoleMappings();
    void deleteRoleMapping(RoleModel role);

    String getFederationLink();
    void setFederationLink(String link);

    void addConsent(UserConsentModel consent);
    UserConsentModel getConsentByClient(String clientInternalId);
    List<UserConsentModel> getConsents();
    void updateConsent(UserConsentModel consent);
    boolean revokeConsentForClient(String clientInternalId);

    public static enum RequiredAction {
        VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD
    }
}
