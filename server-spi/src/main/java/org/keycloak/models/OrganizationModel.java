/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.provider.ProviderEvent;

public interface OrganizationModel {

    String ORGANIZATION_ATTRIBUTE = "kc.org";
    String ORGANIZATION_NAME_ATTRIBUTE = "kc.org.name";
    String ORGANIZATION_DOMAIN_ATTRIBUTE = "kc.org.domain";
    String ALIAS = "alias";

    enum IdentityProviderRedirectMode {
        EMAIL_MATCH("kc.org.broker.redirect.mode.email-matches");

        private final String key;

        IdentityProviderRedirectMode(String key) {
            this.key = key;
        }

        public boolean isSet(IdentityProviderModel broker) {
            return Boolean.parseBoolean(broker.getConfig().get(key));
        }

        public String getKey() {
            return key;
        }
    }

    interface OrganizationMembershipEvent extends ProviderEvent {
        OrganizationModel getOrganization();
        UserModel getUser();
        KeycloakSession getSession();
    }

    interface OrganizationMemberJoinEvent extends OrganizationMembershipEvent {
        static void fire(OrganizationModel organization, UserModel user, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new OrganizationModel.OrganizationMemberJoinEvent() {
                @Override
                public UserModel getUser() {
                    return user;
                }

                @Override
                public OrganizationModel getOrganization() {
                    return organization;
                }

                @Override
                public KeycloakSession getSession() {
                    return session;
                }
            });
        }
    }

    interface OrganizationMemberLeaveEvent extends OrganizationMembershipEvent {
        static void fire(OrganizationModel organization, UserModel user, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new OrganizationModel.OrganizationMemberLeaveEvent() {
                @Override
                public UserModel getUser() {
                    return user;
                }

                @Override
                public OrganizationModel getOrganization() {
                    return organization;
                }

                @Override
                public KeycloakSession getSession() {
                    return session;
                }
            });
        }
    }

    String getId();

    void setName(String name);

    String getName();

    String getAlias();

    void setAlias(String alias);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    String getDescription();

    void setDescription(String description);

    String getRedirectUrl();

    void setRedirectUrl(String redirectUrl);

    Map<String, List<String>> getAttributes();

    void setAttributes(Map<String, List<String>> attributes);

    Stream<OrganizationDomainModel> getDomains();

    void setDomains(Set<OrganizationDomainModel> domains);

    Stream<IdentityProviderModel> getIdentityProviders();

    boolean isManaged(UserModel user);

    boolean isMember(UserModel user);
}
