package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserModel extends RoleMapperModel {
    String USERNAME = "username";
    String LAST_NAME = "lastName";
    String FIRST_NAME = "firstName";
    String EMAIL = "email";
    String LOCALE = "locale";

    String getId();

    String getUsername();

    void setUsername(String username);
    
    /**
     * Get timestamp of user creation. May be null for old users created before this feature introduction.
     */
    Long getCreatedTimestamp();
    
    void setCreatedTimestamp(Long timestamp);

    boolean isEnabled();

    boolean isOtpEnabled();

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

    void setOtpEnabled(boolean totp);

    void updateCredential(UserCredentialModel cred);

    List<UserCredentialValueModel> getCredentialsDirectly();

    void updateCredentialDirectly(UserCredentialValueModel cred);

    Set<GroupModel> getGroups();
    void joinGroup(GroupModel group);
    void leaveGroup(GroupModel group);
    boolean isMemberOf(GroupModel group);

    String getFederationLink();
    void setFederationLink(String link);

    String getServiceAccountClientLink();
    void setServiceAccountClientLink(String clientInternalId);

    void addConsent(UserConsentModel consent);
    UserConsentModel getConsentByClient(String clientInternalId);
    List<UserConsentModel> getConsents();
    void updateConsent(UserConsentModel consent);
    boolean revokeConsentForClient(String clientInternalId);

    public static enum RequiredAction {
        VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD
    }
}
