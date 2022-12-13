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

import org.keycloak.models.search.SearchQueryJson;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.storage.SearchableModelField;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        public static final SearchableModelField<UserModel> FIRST_NAME      = new SearchableModelField<>("firstName", String.class);
        public static final SearchableModelField<UserModel> LAST_NAME       = new SearchableModelField<>("lastName", String.class);
        public static final SearchableModelField<UserModel> EMAIL           = new SearchableModelField<>("email", String.class);
        public static final SearchableModelField<UserModel> ENABLED         = new SearchableModelField<>("enabled", Boolean.class);
        public static final SearchableModelField<UserModel> EMAIL_VERIFIED  = new SearchableModelField<>("emailVerified", Boolean.class);
        public static final SearchableModelField<UserModel> FEDERATION_LINK = new SearchableModelField<>("federationLink", String.class);

        /**
         * Search for user's username in case sensitive mode.
         */
        public static final SearchableModelField<UserModel> USERNAME        = new SearchableModelField<>("username", String.class);
        /**
         * Search for user's username in case insensitive mode.
         */
        public static final SearchableModelField<UserModel> USERNAME_CASE_INSENSITIVE = new SearchableModelField<>("usernameCaseInsensitive", String.class);

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

        public static final SearchableModelField<UserModel> QUERY       = new SearchableModelField<>("query", String.class); // SearchQueryJson.class);
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
     * Obtains all values associated with the specified attribute name.
     *
     * @param name the name of the attribute.
     * @return a non-null {@link Stream} of attribute values.
     */
    Stream<String> getAttributeStream(final String name);

    Map<String, List<String>> getAttributes();

    /**
     * Obtains the names of required actions associated with the user.
     *
     * @return a non-null {@link Stream} of required action names.
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
    void leaveGroup(GroupModel group);
    boolean isMemberOf(GroupModel group);

    String getFederationLink();
    void setFederationLink(String link);

    String getServiceAccountClientLink();
    void setServiceAccountClientLink(String clientInternalId);

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

    /**
     * @deprecated This interface is no longer necessary, collection-based methods were removed from the parent interface
     * and therefore the parent interface can be used directly
     */
    @Deprecated
    interface Streams extends UserModel, RoleMapperModel {
    }
}
