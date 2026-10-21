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
package org.keycloak.forms.login.freemarker.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.authentication.requiredactions.util.UpdateProfileContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProfileBean {

    private static final Logger logger = Logger.getLogger(ProfileBean.class);

    private UpdateProfileContext user;
    private MultivaluedMap<String, String> formData;

    private final Map<String, String> attributes = new HashMap<>();

    public ProfileBean(UpdateProfileContext user, MultivaluedMap<String, String> formData) {
        this.user = user;
        this.formData = formData;

        Map<String, List<String>> modelAttrs = user.getAttributes();
        if (modelAttrs != null) {
            for (Map.Entry<String, List<String>> attr : modelAttrs.entrySet()) {
                List<String> attrValue = attr.getValue();
                if (attrValue != null && attrValue.size() > 0) {
                    attributes.put(attr.getKey(), attrValue.get(0));
                }

                if (attrValue != null && attrValue.size() > 1) {
                    logger.warnf("There are more values for attribute '%s' of user '%s' . Will display just first value", attr.getKey(), user.getUsername());
                }
            }
        }
        if (formData != null) {
            for (String key : formData.keySet()) {
                if (key.startsWith("user.attributes.")) {
                    String attribute = key.substring("user.attributes.".length());
                    attributes.put(attribute, formData.getFirst(key));
                }
            }
        }

    }

    public boolean isEditUsernameAllowed() {
        return user.isEditUsernameAllowed();
    }

    public boolean isEditEmailAllowed() {
        return user.isEditEmailAllowed();
    }

    public String getUsername() { return formData != null ? formData.getFirst("username") : user.getUsername(); }

    public String getFirstName() {
        return formData != null ? formData.getFirst("firstName") : user.getFirstName();
    }

    public String getLastName() {
        return formData != null ? formData.getFirst("lastName") : user.getLastName();
    }

    public String getEmail() {
        return formData != null ? formData.getFirst("email") : user.getEmail();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
