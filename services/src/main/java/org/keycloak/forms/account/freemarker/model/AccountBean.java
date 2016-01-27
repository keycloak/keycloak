package org.keycloak.forms.account.freemarker.model;

import org.jboss.logging.Logger;

import org.keycloak.logging.KeycloakLogger;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountBean {

    private static final KeycloakLogger logger = Logger.getMessageLogger(KeycloakLogger.class, AccountBean.class.getName());

    private final UserModel user;
    private final MultivaluedMap<String, String> profileFormData;

    // TODO: More proper multi-value attribute support
    private final Map<String, String> attributes = new HashMap<>();

    public AccountBean(UserModel user, MultivaluedMap<String, String> profileFormData) {
        this.user = user;
        this.profileFormData = profileFormData;

        for (Map.Entry<String, List<String>> attr : user.getAttributes().entrySet()) {
            List<String> attrValue = attr.getValue();
            if (attrValue.size() > 0) {
                attributes.put(attr.getKey(), attrValue.get(0));
            }

            if (attrValue.size() > 1) {
                logger.USER.moreValuesForAttribute(attr.getKey(), user.getUsername());
            }
        }

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
