package org.keycloak.models;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserModel {
    public static final String LOGIN_NAME = "username";
    public static final String LAST_NAME = "lastName";
    public static final String FIRST_NAME = "firstName";
    public static final String EMAIL = "email";

    String getLoginName();

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

    Set<String> getWebOrigins();

    void setWebOrigins(Set<String> webOrigins);

    void addWebOrigin(String webOrigin);

    void removeWebOrigin(String webOrigin);

    Set<String> getRedirectUris();

    void setRedirectUris(Set<String> redirectUris);

    void addRedirectUri(String redirectUri);

    void removeRedirectUri(String redirectUri);

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    String getEmail();

    void setEmail(String email);

    boolean isEmailVerified();

    void setEmailVerified(boolean verified);

    void setTotp(boolean totp);

    public static enum RequiredAction {
        VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD
    }
}