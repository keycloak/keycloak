package org.keycloak.models.users;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface User {

    public static final String USERNAME = "username";
    public static final String LAST_NAME = "lastName";
    public static final String FIRST_NAME = "firstName";
    public static final String EMAIL = "email";

    String getId();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    String getUsername();

    void setUsername(String username);

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    String getEmail();

    void setEmail(String email);

    String getAttribute(String name);

    Map<String, String> getAttributes();

    void setAttribute(String name, String value);

    void removeAttribute(String name);

    List<Credentials> getCredentials();

    void updateCredential(Credentials cred);

    Set<String> getRoleMappings();

    void grantRole(String role);

    void deleteRoleMapping(String role);

}