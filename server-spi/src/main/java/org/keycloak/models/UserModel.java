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

import org.keycloak.storage.SearchableModelField;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserModel extends RoleMapperModel {
    String USERNAME = "username";
    String FIRST_NAME = "firstName";
    String LAST_NAME = "lastName";
    String EMAIL = "email";
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

    Comparator<UserModel> COMPARE_BY_USERNAME = Comparator.comparing(UserModel::getUsername, String.CASE_INSENSITIVE_ORDER);

    public static class SearchableFields {
        public static final SearchableModelField<UserModel> ID              = new SearchableModelField<>("id", String.class);
        public static final SearchableModelField<UserModel> REALM_ID        = new SearchableModelField<>("realmId", String.class);
        public static final SearchableModelField<UserModel> USERNAME        = new SearchableModelField<>("username", String.class);
        public static final SearchableModelField<UserModel> FIRST_NAME      = new SearchableModelField<>("firstName", String.class);
        public static final SearchableModelField<UserModel> LAST_NAME       = new SearchableModelField<>("lastName", String.class);
        public static final SearchableModelField<UserModel> EMAIL           = new SearchableModelField<>("email", String.class);
        public static final SearchableModelField<UserModel> ENABLED         = new SearchableModelField<>("enabled", Boolean.class);
        public static final SearchableModelField<UserModel> EMAIL_VERIFIED  = new SearchableModelField<>("emailVerified", Boolean.class);
        public static final SearchableModelField<UserModel> FEDERATION_LINK = new SearchableModelField<>("federationLink", String.class);

        /**
         * This field can only searched either for users coming from an IDP, then the operand is (idp_alias),
         * or as user coming from a particular IDP with given username there, then the operand is a pair (idp_alias, idp_user_id).
         * It is also possible to search regardless of {@code idp_alias}, then the pair is {@code (null, idp_user_id)}.
         */
        public static final SearchableModelField<UserModel> IDP_AND_USER    = new SearchableModelField<>("idpAlias:idpUserId", String.class);

        public static final SearchableModelField<UserModel> ASSIGNED_ROLE   = new SearchableModelField<>("assignedRole", String.class);
        public static final SearchableModelField<UserModel> ASSIGNED_GROUP  = new SearchableModelField<>("assignedGroup", String.class);
        /**
         * Search for users that have consent set for a particular client.
         */
        public static final SearchableModelField<UserModel> CONSENT_FOR_CLIENT = new SearchableModelField<>("clientConsent", String.class);
        /**
         * Search for users that have consent set for a particular client that originates in the given client provider.
         */
        public static final SearchableModelField<UserModel> CONSENT_CLIENT_FEDERATION_LINK = new SearchableModelField<>("clientConsentFederationLink", String.class);
        /**
         * Search for users that have consent that has given client scope.
         */
        public static final SearchableModelField<UserModel> CONSENT_WITH_CLIENT_SCOPE = new SearchableModelField<>("consentWithClientScope", String.class);
        /**
         * ID of the client corresponding to the service account
         */
        public static final SearchableModelField<UserModel> SERVICE_ACCOUNT_CLIENT = new SearchableModelField<>("serviceAccountClientId", String.class);
        /**
         * Search for attribute value. The parameters is a pair {@code (attribute_name, values...)} where {@code attribute_name}
         * is always checked for equality, and the value (which can be any numbert of values, none for operators like EXISTS
         * or potentially many for e.g. IN) is checked per the operator.
         */
        public static final SearchableModelField<UserModel> ATTRIBUTE       = new SearchableModelField<>("attribute", String[].class);
    }

    interface UserRemovedEvent extends ProviderEvent {
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
     * @param name
     * @return list of all attribute values or empty list if there are not any values. Never return null
     * @deprecated Use {@link #getAttributeStream(String) getAttributeStream} instead.
     */
    @Deprecated
    List<String> getAttribute(String name);

    /**
     * Obtains all values associated with the specified attribute name.
     *
     * @param name the name of the attribute.
     * @return a non-null {@link Stream} of attribute values.
     */
    default Stream<String> getAttributeStream(final String name) {
        List<String> value = this.getAttribute(name);
        return value != null ? value.stream() : Stream.empty();
    }

    Map<String, List<String>> getAttributes();

    /**
     * @deprecated Use {@link #getRequiredActionsStream() getRequiredActionsStream} instead.
     */
    @Deprecated
    Set<String> getRequiredActions();

    /**
     * Obtains the names of required actions associated with the user.
     *
     * @return a non-null {@link Stream} of required action names.
     */
    default Stream<String> getRequiredActionsStream() {
        Set<String> value = this.getRequiredActions();
        return value != null ? value.stream() : Stream.empty();
    }

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
     * @deprecated Use {@link #getGroupsStream() getGroupsStream} instead.
     */
    @Deprecated
    Set<GroupModel> getGroups();

    /**
     * Obtains the groups associated with the user.
     *
     * @return a non-null {@link Stream} of groups.
     */
    default Stream<GroupModel> getGroupsStream() {
        Set<GroupModel> value = this.getGroups();
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * @deprecated Use {@link #getGroupsStream(String, Integer, Integer) getGroupsStream} instead.
     */
    @Deprecated
    default Set<GroupModel> getGroups(int first, int max) {
        return getGroupsStream(null, first, max).collect(Collectors.toSet());
    }

    /**
     * @deprecated Use {@link #getGroupsStream(String, Integer, Integer) getGroupsStream} instead.
     */
    @Deprecated
    default Set<GroupModel> getGroups(String search, int first, int max) {
        return getGroupsStream(search, first, max)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

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
    void leaveGroup(GroupModel group);
    boolean isMemberOf(GroupModel group);

    String getFederationLink();
    void setFederationLink(String link);

    String getServiceAccountClientLink();
    void setServiceAccountClientLink(String clientInternalId);

    enum RequiredAction {
        VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD, TERMS_AND_CONDITIONS,
        VERIFY_PROFILE
    }

    /**
     * The {@link UserModel.Streams} interface makes all collection-based methods in {@link UserModel} default by providing
     * implementations that delegate to the {@link Stream}-based variants instead of the other way around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends UserModel, RoleMapperModel.Streams {
        @Override
        default List<String> getAttribute(String name) {
            return this.getAttributeStream(name).collect(Collectors.toList());
        }

        @Override
        Stream<String> getAttributeStream(final String name);

        @Override
        default Set<String> getRequiredActions() {
            return this.getRequiredActionsStream().collect(Collectors.toSet());
        }

        @Override
        Stream<String> getRequiredActionsStream();

        @Override
        default Set<GroupModel> getGroups() {
            return this.getGroupsStream().collect(Collectors.toSet());
        }

        @Override
        Stream<GroupModel> getGroupsStream();
    }
}
