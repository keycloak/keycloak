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
package org.keycloak.email.freemarker.beans;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.forms.login.freemarker.model.OrganizationBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.userprofile.UserProfileProvider;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProfileBean {

    private static final Logger logger = Logger.getLogger(ProfileBean.class);

    private final UserModel user;
    private final KeycloakSession session;
    private final Map<String, String> attributes = new HashMap<>();
    private List<OrganizationBean> organizations;

    public ProfileBean(UserModel user, KeycloakSession session) {
        this.user = user;
        this.session = session;

        if (user.getAttributes() != null) {
            //TODO: there is no need to set only a single value for attributes but changing this might break existing
            // deployments using email templates, if we change the contract to return multiple values for attributes
            UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
            UPConfig configuration = provider.getConfiguration();

            for (Map.Entry<String, List<String>> attr : user.getAttributes().entrySet()) {
                List<String> attrValue = attr.getValue();
                if (attrValue != null && attrValue.size() > 0) {
                    attributes.put(attr.getKey(), attrValue.get(0));
                }

                UPAttribute attribute = configuration.getAttribute(attr.getKey());
                boolean multivalued = attribute != null && attribute.isMultivalued();

                if (!multivalued && attrValue != null && attrValue.size() > 1) {
                    logger.warnf("There are more values for attribute '%s' of user '%s' . Will display just first value", attr.getKey(), user.getUsername());
                }
            }
        }
    }

    public String getUsername() { return user.getUsername(); }

    public String getFirstName() {
        return user.getFirstName();
    }

    public String getLastName() {
        return user.getLastName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<OrganizationBean> getOrganizations() {
        if (organizations == null) {
            final var organizationsProvider = session.getProvider(OrganizationProvider.class);
            if (organizationsProvider == null) {
                organizations = Collections.emptyList();
            }
            else {
                organizations = organizationsProvider.getByMember(user)
                        .map(o -> new OrganizationBean(o, user))
                        .toList();
            }
        }
        return organizations;
    }
}
