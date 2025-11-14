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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.provider.ProviderEvent;

import static org.keycloak.utils.StringUtil.isNotBlank;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserModel extends RoleMapperModel {
    String USERNAME = "username";
    String FIRST_NAME = "firstName";
    String LAST_NAME = "lastName";
    String EMAIL = "email";
    String EMAIL_PENDING = "kc.email.pending";
    String EMAIL_VERIFIED = "emailVerified";
    String LOCALE = "locale";
    String ENABLED = "enabled";
    String IDP_ALIAS = "keycloak.session.realm.users.query.idp_alias";
    String IDP_USER_ID = "keycloak.session.realm.users.query.idp_user_id";
    String INCLUDE_SERVICE_ACCOUNT = "keycloak.session.realm.users.query.include_service_account";
    String GROUPS = "keycloak.session.realm.users.query.groups";
    String SEARCH = "keycloak.session.realm.users.query.search";
    String EXACT = "keycloak.session.realm.users.query.exact";
    String DISABLED_REASON = "disabledReason";
    //attribute name used to mark a temporary admin user/service account as temporary
    String IS_TEMP_ADMIN_ATTR_NAME = "is_temporary_admin";

    Comparator<UserModel> COMPARE_BY_USERNAME = Comparator.comparing(UserModel::getUsername, String.CASE_INSENSITIVE_ORDER);

    interface UserRemovedEvent extends ProviderEvent {
        RealmModel getRealm();
        UserModel getUser();
        KeycloakSession getKeycloakSession();
    }

    interface UserPreRemovedEvent extends ProviderEvent {
        RealmModel getRealm();
        UserModel getUser();
        KeycloakSession getKeycloakSession();
    }

    String getId();

    // No default method here to allow Abstract subclasses where the username is provided in a different manner
    String getUsername();

    /**
     * Sets username for this user.
     *
     * No default method here to allow Abstract subclasses where the username is provided in a different manner
     *
     * @param username username string
     */
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
     * Obtains all values associated with the specified attribute name.
     *
     * @param name the name of the attribute.
     * @return a non-null {@link Stream} of attribute values.
     */
    Stream<String> getAttributeStream(final String name);

    Map<String, List<String>> getAttributes();

    /**
     * Obtains the aliases of required actions associated with the user.
     *
     * @return a non-null {@link Stream} of required action aliases.
     */
    Stream<String> getRequiredActionsStream();

    void addRequiredAction(String action);

    void removeRequiredAction(String action);

    default void addRequiredAction(RequiredAction action) {
        if (action == null) return;
        String actionName = action.name();
        addRequiredAction(actionName);
    }

    default void removeRequiredAction(RequiredAction action) {
        if (action == null) return;
        String actionName = action.name();
        removeRequiredAction(actionName);
    }

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    String getEmail();

    /**
     * Sets email for this user.
     *
     * @param email the email
     */
    void setEmail(String email);

    boolean isEmailVerified();

    void setEmailVerified(boolean verified);

    /**
     * Obtains the groups associated with the user.
     *
     * @return a non-null {@link Stream} of groups.
     */
    Stream<GroupModel> getGroupsStream();

    /**
     * Returns a paginated stream of groups within this realm with search in the name
     *
     * @param search Case insensitive string which will be searched for. Ignored if null.
     * @param first Index of first group to return. Ignored if negative or {@code null}.
     * @param max Maximum number of records to return. Ignored if negative or {@code null}.
     * @return Stream of desired groups. Never returns {@code null}.
     */
    default Stream<GroupModel> getGroupsStream(String search, Integer first, Integer max) {
        if (search != null) search = search.toLowerCase();
        final String finalSearch = search;
        Stream<GroupModel> groupModelStream = getGroupsStream()
                .filter(group -> finalSearch == null || group.getName().toLowerCase().contains(finalSearch));

        if (first != null && first > 0) {
            groupModelStream = groupModelStream.skip(first);
        }

        if (max != null && max >= 0) {
            groupModelStream = groupModelStream.limit(max);
        }

        return groupModelStream;
    }

    default long getGroupsCount() {
        return getGroupsCountByNameContaining(null);
    }

    default long getGroupsCountByNameContaining(String search) {
        if (search == null) {
            return getGroupsStream().count();
        }

        String s = search.toLowerCase();
        return getGroupsStream().filter(group -> group.getName().toLowerCase().contains(s)).count();
    }

    void joinGroup(GroupModel group);
    default void joinGroup(GroupModel group, MembershipMetadata metadata) {
        joinGroup(group);
    }
    void leaveGroup(GroupModel group);
    boolean isMemberOf(GroupModel group);

    String getFederationLink();
    void setFederationLink(String link);

    String getServiceAccountClientLink();
    void setServiceAccountClientLink(String clientInternalId);

    /**
     * Indicates if this {@link UserModel} maps to a local account or an account
     * federated from an external user storage.
     *
     * @return {@code true} if a federated account. Otherwise, {@code false}.
     */
    default boolean isFederated() {
        return isNotBlank(getFederationLink());
    }

    /**
     * Instance of a user credential manager to validate and update the credentials of this user.
     */
    SubjectCredentialManager credentialManager();

    enum RequiredAction {
        VERIFY_EMAIL,
        UPDATE_PROFILE,
        CONFIGURE_TOTP,
        CONFIGURE_RECOVERY_AUTHN_CODES,
        UPDATE_PASSWORD,
        TERMS_AND_CONDITIONS,
        VERIFY_PROFILE,
        UPDATE_EMAIL
    }
}
