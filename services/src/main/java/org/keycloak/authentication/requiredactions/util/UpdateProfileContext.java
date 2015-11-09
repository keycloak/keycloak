package org.keycloak.authentication.requiredactions.util;

import java.util.List;
import java.util.Map;

/**
 * Abstraction, which allows to display updateProfile page in various contexts (Required action of already existing user, or first identity provider
 * login when user doesn't yet exists in Keycloak DB)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UpdateProfileContext {

    boolean isEditUsernameAllowed();

    String getUsername();

    void setUsername(String username);

    String getEmail();

    void setEmail(String email);

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    Map<String, List<String>> getAttributes();

    void setAttribute(String key, List<String> value);

    List<String> getAttribute(String key);

}
