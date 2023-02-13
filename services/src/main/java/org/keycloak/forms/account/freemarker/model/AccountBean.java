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

package org.keycloak.forms.account.freemarker.model;

import org.jboss.logging.Logger;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountBean {

    private static final Logger logger = Logger.getLogger(AccountBean.class);

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
                logger.warnf("There are more values for attribute '%s' of user '%s' . Will display just first value", attr.getKey(), user.getUsername());
            }
        }

        if (profileFormData != null) {
            for (String key : profileFormData.keySet()) {
                if (key.startsWith(Constants.USER_ATTRIBUTES_PREFIX)) {
                    String attribute = key.substring(Constants.USER_ATTRIBUTES_PREFIX.length());
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
        if (profileFormData != null && profileFormData.containsKey("username")) {
            return profileFormData.getFirst("username");
        } else {
            return user.getUsername();
        }
    }

    public String getEmail() {
        return profileFormData != null ?  profileFormData.getFirst("email") :user.getEmail();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

}
