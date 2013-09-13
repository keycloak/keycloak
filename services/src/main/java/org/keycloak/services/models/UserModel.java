package org.keycloak.services.models;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserModel {
    String getLoginName();

    boolean isEnabled();

    boolean isTotp();

    Status getStatus();

    void setStatus(Status status);

    void setAttribute(String name, String value);

    void removeAttribute(String name);

    String getAttribute(String name);

    Map<String, String> getAttributes();

    List<RequiredAction> getRequiredActions();
    
    void addRequiredAction(RequiredAction action);

    void removeRequiredAction(RequiredAction action);

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    String getEmail();

    void setEmail(String email);

    void setTotp(boolean totp);

    public static enum Status {
        ENABLED, DISABLED, ACTIONS_REQUIRED
    }

    public static enum RequiredAction {
        VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, RESET_PASSWORD
    }
}