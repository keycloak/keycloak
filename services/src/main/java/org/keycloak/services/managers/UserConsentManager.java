/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.managers;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrderedAuthorizationDetails;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.rar.AuthorizationDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentManager {

    public static boolean SHOW_CONSENT_SCREEN_TEXT = true;
    public static boolean SHOW_SCOPE_NAME = false;

    /**
     * Revoke consent of given user to given client
     *
     * @param session
     * @param client
     * @param user
     * @return true if either consent or offlineToken was revoked
     */
    public static boolean revokeConsentToClient(KeycloakSession session, ClientModel client, UserModel user) {
        RealmModel realm = session.getContext().getRealm();
        UserConsentModel consent = session.users().getConsentByClient(realm, user.getId(), client.getId());
        boolean revokedConsent = session.users().revokeConsentForClient(realm, user.getId(), client.getId());
        if(consent != null) {
            for(ClientScopeModel clientScopeModel : consent.getGrantedClientScopes()) {
                String consentAttributeKey = buildUserConsentAttributeKey(client, clientScopeModel.getId());
                user.setAttribute(consentAttributeKey, null);
            }
        }
        boolean revokedOfflineToken = new UserSessionManager(session).revokeOfflineToken(user, client);
        if (revokedConsent) {
            // Logout clientSessions for this user and client
            AuthenticationManager.backchannelLogoutUserFromClient(session, realm, user, client, session.getContext().getUri(), session.getContext().getRequestHeaders());
        }

        return revokedConsent || revokedOfflineToken;
    }

    /**
     * Get all the consented scopès for a specific client, with their Dynamic Scope parameter appended as a suffix if present
     * The scopes would be returned either as their name, or as their consent screen text, based on the value of the {@param showConsentScreenText}
     *
     * @param session
     * @param user
     * @param client
     * @param showConsentScreenText
     * @return the list of the consented scopes, with their dynamic scope param appended, if needed.
     */
    public static List<String> getProcessedConsentedScopesWithDynamicParam(KeycloakSession session, UserModel user, ClientModel client, boolean showConsentScreenText) {
        Stream<AuthorizationDetails> authorizationDetailsStream = getConsentedScopesStream(session, user, client)
                .map(s -> scopeIdToAuthorizationDetails(client, s))
                .filter(Objects::nonNull)
                .sorted(OrderedAuthorizationDetails.OrderedAuthorizationDetailsComparator.getInstance());
        if (showConsentScreenText) {
            return authorizationDetailsStream.map(AuthorizationDetails::getScopeConsentScreenTextWithParamIfSet)
                    .collect(Collectors.toList());
        } else {
            return authorizationDetailsStream.map(AuthorizationDetails::getScopeNameWithParamIfSet)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Transforms a scopeId string which may or may not have a dynamic scope parameter appended as a suffix
     * to an {@link AuthorizationDetails} object
     *
     * @param client
     * @param scopeId
     * @return the {@link AuthorizationDetails} object representation of the given scopeId
     */
    private static AuthorizationDetails scopeIdToAuthorizationDetails(ClientModel client, String scopeId) {
        String id = scopeId;
        String param = "";
        if (scopeId.contains(":")) {
            String[] scopeParts = scopeId.split(":");
            id = scopeParts[0];
            param = scopeParts[1];
        }
        ClientScopeModel clientScopeModel = KeycloakModelUtils.findClientScopeById(client.getRealm(), client, id);
        if (clientScopeModel == null) {
            return null;
        }
        return new AuthorizationDetails(clientScopeModel, param);
    }

    /**
     * Transforms a {@link ClientScopeModel} to a list of ScopeIds with or without the Dynamic Scope parameter as
     * a suffix.
     *
     * These parameters are stored as a User Attribute, and these will be obtained by querying the provided user's attributes
     *
     * @param user
     * @param client
     * @param scope
     * @return a list of ScopeIds with or without a dynamic scope parameter appended as a suffix.
     */
    private static List<String> mapToScopeWithParam(UserModel user, ClientModel client, ClientScopeModel scope) {
        List<String> mappedScopes = new ArrayList<>();
        if (scope.isDynamicScope()) {
            mappedScopes.addAll(user.getAttributeStream(buildUserConsentAttributeKey(client, scope.getId()))
                    .map(s -> scope.getId() + ":" + s).collect(Collectors.toList()));
        } else {
            mappedScopes.add(scope.getId());
        }
        return mappedScopes;
    }

    /**
     * Obtains the consent for the given user and checks one by one whether the user has an attribute of the form:
     * {@code "consent.<clientId>.<scopeId>"} to retrieve any possible Dynamic Scopes parameters for the specific scope.
     *
     * A Client Scope may have more than one parameter attached. This results in adding an entry per parameter to the
     * returned Stream with the same scopeId.
     *
     * @param session
     * @param user
     * @param client
     * @return a stream of the consented scopeIds with any possible dynamic scopes parameters attached to them.
     */
    public static Stream<String> getConsentedScopesStream(KeycloakSession session, UserModel user, ClientModel client) {
        // we use the entity's UUID instead of the client Id because client ID can change, invalidating all the consents
        UserConsentModel consent = session.users().getConsentByClient(client.getRealm(), user.getId(), client.getId());
        if (consent == null) {
            return Stream.empty();
        }
        List<String> computedScopes = new LinkedList<>();
        for (ClientScopeModel clientScopeModel : consent.getGrantedClientScopes()) {
            computedScopes.addAll(mapToScopeWithParam(user, client, clientScopeModel));
        }
        return computedScopes.stream();
    }

    /**
     * Stores the given {@link UserConsentModel} and stores it into the database.
     *
     * Any scopes with a Dynamic Scope parameter will be stored as a single User Attribute with a list of dynamic scope
     * parameters.
     *
     * This allows us to store the dynamic scopes parameters only in the User profile.
     *
     * @param session
     * @param user
     * @param grantedConsent
     */
    public static void storeConsentAsUserAttribute(KeycloakSession session, UserModel user, UserConsentModel grantedConsent) {
        // for now, until it's deprecated, we keep updating the old consent as usual
        session.users().updateConsent(session.getContext().getRealm(), user.getId(), grantedConsent);
        Multimap<String, String> dynamicScopesParamMap = LinkedListMultimap.create();
        for (String scope : grantedConsent.getConsentedScopesFromAuthorizationDetails()) {
            if (scope.contains(":")) {
                String[] scopeParts = scope.split(":");
                dynamicScopesParamMap.put(scopeParts[0], scopeParts[1]);
            }
        }
        for (Map.Entry<String, Collection<String>> entry : dynamicScopesParamMap.asMap().entrySet()) {
            String consentAttributeKey = buildUserConsentAttributeKey(grantedConsent.getClient(), entry.getKey());
            List<String> mergedConsentedAndNewScopes =  Stream.concat(user.getAttributeStream(consentAttributeKey), entry.getValue().stream()).collect(Collectors.toList());
            user.setAttribute(consentAttributeKey, mergedConsentedAndNewScopes);
        }
    }

    public static String buildUserConsentAttributeKey(ClientModel client, String clientScopeId) {
        return String.format(UserModel.CONSENT_ATTR, client.getId(), clientScopeId);
    }
}
