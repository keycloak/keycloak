package org.keycloak.services.resources;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AttributeFormDataProcessor {
    /**
     * Looks for "user.attributes." keys in the form data and sets the appropriate UserModel.attribute from it.
     *
     * @param formData
     * @param realm
     * @param user
     */
    public static void process(MultivaluedMap<String, String> formData, RealmModel realm, UserModel user) {
        for (String key : formData.keySet()) {
            if (!key.startsWith("user.attributes.")) continue;
            String attribute = key.substring("user.attributes.".length());
            user.setAttribute(attribute, formData.getFirst(key));
        }

    }
}
