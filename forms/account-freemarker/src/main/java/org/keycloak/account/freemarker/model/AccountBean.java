package org.keycloak.account.freemarker.model;

import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountBean {

    private final UserModel user;
    private final MultivaluedMap<String, String> profileFormData;
    private final Map<String, String> attributes = new HashMap<>();

    public AccountBean(UserModel user, MultivaluedMap<String, String> profileFormData) {
        this.user = user;
        this.profileFormData = profileFormData;
        attributes.putAll(user.getAttributes());
        if (profileFormData != null) {
            for (String key : profileFormData.keySet()) {
                if (key.startsWith("user.attributes.")) {
                    String attribute = key.substring("user.attributes.".length());
                    attributes.put(attribute, profileFormData.getFirst(key));
                }
            }
        }
    }

    public String getFirstName() {
        return profileFormData != null ?  profileFormData.getFirst("firstName") : user.getFirstName();
    }

    public String getLastName() {
        return profileFormData != null ?  profileFormData.getFirst("lastName") :user.getLastName();
    }

    public String getUsername() {
        return profileFormData != null ? profileFormData.getFirst("username") : user.getUsername();
    }

    public String getEmail() {
        return profileFormData != null ?  profileFormData.getFirst("email") :user.getEmail();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

}
