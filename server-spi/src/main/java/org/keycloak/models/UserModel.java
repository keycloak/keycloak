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

package org.keycloak.models;

import org.keycloak.provider.ProviderEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserModel extends RoleMapperModel {
    String USERNAME = "username";
    String LAST_NAME = "lastName";
    String FIRST_NAME = "firstName";
    String EMAIL = "email";
    String LOCALE = "locale";
    String INCLUDE_SERVICE_ACCOUNT = "keycloak.session.realm.users.query.include_service_account";
    String GROUPS = "keycloak.session.realm.users.query.groups";

    interface UserRemovedEvent extends ProviderEvent {
        RealmModel getRealm();
        UserModel getUser();
        KeycloakSession getKeycloakSession();
    }

    String getId();

    String getUsername();

    void setUsername(String username);
    
    /**
     * Get timestamp of user creation. May be null for old users created before this feature introduction.
     */
    Long getCreatedTimestamp();
    
    void setCreatedTimestamp(Long timestamp);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Set single value of specified attribute. Remove all other existing values of this attribute
     *
     * @param name
     * @param value
     */
    void setSingleAttribute(String name, String value);

    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);

    /**
     * @param name
     * @return null if there is not any value of specified attribute or first value otherwise. Don't throw exception if there are more values of the attribute
     */
    String getFirstAttribute(String name);

    /**
     * @param name
     * @return list of all attribute values or empty list if there are not any values. Never return null
     */
    List<String> getAttribute(String name);

    Map<String, List<String>> getAttributes();

    Set<String> getRequiredActions();

    void addRequiredAction(String action);

    void removeRequiredAction(String action);

    void addRequiredAction(RequiredAction action);

    void removeRequiredAction(RequiredAction action);

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    String getEmail();

    void setEmail(String email);

    boolean isEmailVerified();

    void setEmailVerified(boolean verified);

    Set<GroupModel> getGroups();

    default Set<GroupModel> getGroups(int first, int max) {
        return getGroups(null, first, max);
    }

    default Set<GroupModel> getGroups(String search, int first, int max) {
        return getGroups().stream()
                .filter(group -> search == null || group.getName().toLowerCase().contains(search.toLowerCase()))
                .skip(first)
                .limit(max)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default long getGroupsCount() {
        return getGroupsCountByNameContaining(null);
    }
    
    default long getGroupsCountByNameContaining(String search) {
        if (search == null) {
            return getGroups().size();
        }

        String s = search.toLowerCase();
        return getGroups().stream().filter(group -> group.getName().toLowerCase().contains(s)).count();
    }

    void joinGroup(GroupModel group);
    void leaveGroup(GroupModel group);
    boolean isMemberOf(GroupModel group);

    String getFederationLink();
    void setFederationLink(String link);

    String getServiceAccountClientLink();
    void setServiceAccountClientLink(String clientInternalId);

    enum RequiredAction {
        VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD, TERMS_AND_CONDITIONS
    }
}
