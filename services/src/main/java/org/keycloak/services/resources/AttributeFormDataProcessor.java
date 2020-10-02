/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.services.resources;

import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.profile.representations.AttributeUserProfile;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AttributeFormDataProcessor {


    public static AttributeUserProfile process(MultivaluedMap<String, String> formData) {
        Map<String, List<String>> attributes= new HashMap<>();
        for (String key : formData.keySet()) {
            if (!key.startsWith(Constants.USER_ATTRIBUTES_PREFIX)) continue;
            String attribute = key.substring(Constants.USER_ATTRIBUTES_PREFIX.length());

            // Need to handle case when attribute has multiple values, but in UI was displayed just first value
            List<String> modelValue = new ArrayList<String>();

            int index = 0;
            for (String value : formData.get(key)) {
                addOrSetValue(modelValue, index, value);
                index++;
            }

            attributes.put(attribute, modelValue);
        }
        return new AttributeUserProfile(attributes);
    }

    public static AttributeUserProfile toUserProfile(MultivaluedMap<String, String> formData) {
        AttributeUserProfile profile = process(formData);

        copyAttribute(UserModel.USERNAME, formData, profile);
        copyAttribute(UserModel.FIRST_NAME, formData, profile);
        copyAttribute(UserModel.LAST_NAME, formData, profile);
        copyAttribute(UserModel.EMAIL, formData, profile);


        return profile;
    }

    private static void copyAttribute(String key, MultivaluedMap<String, String> formData, AttributeUserProfile rep) {
        if (formData.getFirst(key) != null)
            rep.getAttributes().setSingleAttribute(key, formData.getFirst(key));
    }


    private static void addOrSetValue(List<String> list, int index, String value) {
        if (list.size() > index) {
            list.set(index, value);
        } else {
            list.add(value);
        }
    }
}
