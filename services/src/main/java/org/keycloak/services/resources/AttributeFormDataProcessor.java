package org.keycloak.services.resources;

import java.util.ArrayList;
import java.util.List;

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

            // Need to handle case when attribute has multiple values, but in UI was displayed just first value
            List<String> modelValue = new ArrayList<>(user.getAttribute(attribute));

            int index = 0;
            for (String value : formData.get(key)) {
                addOrSetValue(modelValue, index, value);
                index++;
            }

            user.setAttribute(attribute, modelValue);
        }

    }

    private static void addOrSetValue(List<String> list, int index, String value) {
        if (list.size() > index) {
            list.set(index, value);
        } else {
            list.add(value);
        }
    }
}
