/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CachedRealmModel;

import org.jboss.logging.Logger;

import static org.keycloak.models.Constants.NO_LOA;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoAUtil {

    private static final Logger logger = Logger.getLogger(LoAUtil.class);

    /**
     * @param clientSession
     * @return current level from client session
     */
    public static int getCurrentLevelOfAuthentication(AuthenticatedClientSessionModel clientSession) {
        String clientSessionLoaNote = clientSession.getNote(Constants.LEVEL_OF_AUTHENTICATION);
        return clientSessionLoaNote == null ? NO_LOA : Integer.parseInt(clientSessionLoaNote);
    }


    /**
     * @param realm
     * @return All LoA numbers configured in the conditions in the realm browser flow
     */
    public static Stream<Integer> getLoAConfiguredInRealmBrowserFlow(RealmModel realm) {
        Map<Integer, Integer> loaMaxAges = getLoaMaxAgesConfiguredInRealmBrowserFlow(realm);
        if (loaMaxAges.isEmpty()) {
            // Default values used when step-up conditions not used in the browser authentication flow.
            // This is used for backwards compatibility and in case when step-up is not configured in the authentication flow (returning 1 in case of "normal" authentication, 0 for SSO authentication)
            return Stream.of(Constants.MINIMUM_LOA, 1);
        } else {
            // Add 0 as a level used for SSO cookie
            return Stream.concat(Stream.of(Constants.MINIMUM_LOA), loaMaxAges.keySet().stream());
        }
    }


    /**
     * @param realm
     * @return All LoA numbers configured in the conditions in the realm browser flow. Key is level, Value is maxAge for particular level
     */
    public static Map<Integer, Integer> getLoaMaxAgesConfiguredInRealmBrowserFlow(RealmModel realm) {
      return getLoaMaxAgesConfiguredInRealmFlow(realm, realm.getBrowserFlow().getId());
    }

    /**
     * @param realm
     * @param flowId
     * @return All LoA numbers configured in the conditions in the realm flow @{param flowId}. Key is level, Vaue is maxAge for particular level
     */
    public static Map<Integer, Integer> getLoaMaxAgesConfiguredInRealmFlow(RealmModel realm, String flowId) {
        List<AuthenticationExecutionModel> loaConditions = AuthenticatorUtil.getExecutionsByType(realm, flowId, ConditionalLoaAuthenticatorFactory.PROVIDER_ID);
        if (loaConditions.isEmpty()) {
            // Default values used when step-up conditions not used in the browser authentication flow.
            // This is used for backwards compatibility and in case when step-up is not configured in the authentication flow (returning 1 in case of "normal" authentication, 0 for SSO authentication)
            return Collections.emptyMap();
        } else {
            Map<Integer, Integer> loas = loaConditions.stream()
                    .map(authExecution -> realm.getAuthenticatorConfigById(authExecution.getAuthenticatorConfig()))
                    .filter(Objects::nonNull)
                    .filter(authConfig -> getLevelFromLoaConditionConfiguration(authConfig) != null)
                    .collect(Collectors.toMap(LoAUtil::getLevelFromLoaConditionConfiguration, LoAUtil::getMaxAgeFromLoaConditionConfiguration));
            return loas;
        }
    }


    public static Integer getLevelFromLoaConditionConfiguration(AuthenticatorConfigModel loaConditionConfig) {
        String levelAsStr = loaConditionConfig.getConfig().get(ConditionalLoaAuthenticator.LEVEL);
        try {
            // Check it can be cast to number
            return Integer.parseInt(levelAsStr);
        } catch (NullPointerException | NumberFormatException e) {
            logger.warnf("Invalid level '%s' configured for the configuration of LoA condition with alias '%s'. Level should be number.", levelAsStr, loaConditionConfig.getAlias());
            return null;
        }
    }


    public static int getMaxAgeFromLoaConditionConfiguration(AuthenticatorConfigModel loaConditionConfig) {
        try {
            return Integer.parseInt(loaConditionConfig.getConfig().get(ConditionalLoaAuthenticator.MAX_AGE));
        } catch (NullPointerException | NumberFormatException e) {
            // Backwards compatibility with Keycloak 17
            String storeLoaInUserSession = loaConditionConfig.getConfig().get(ConditionalLoaAuthenticator.STORE_IN_USER_SESSION);
            if (storeLoaInUserSession != null) {
                int maxAge = Boolean.parseBoolean(storeLoaInUserSession) ? ConditionalLoaAuthenticator.DEFAULT_MAX_AGE : 0;
                logger.warnf("Max age not configured for condition '%s' in the authentication flow. Fallback to %d based on the configuration option %s from previous version",
                        loaConditionConfig.getAlias(), maxAge, ConditionalLoaAuthenticator.STORE_IN_USER_SESSION);
                return maxAge;
            }

            logger.errorf("Invalid max age configured for condition '%s'. Fallback to 0", loaConditionConfig.getAlias());
            return 0;
        }
    }

    /**
     * Return map where:
     *  - keys are credential types corresponding to authenticators available in given authentication flow
     *  - values are LoA levels of those credentials in the given flow (If not step-up authentication is used, values will be always Constants.NO_LOA)
     *
     *  For instance if we have password as level1 and OTP or WebAuthn as available level2 authenticators it can return map like:
     *   { "password" -> 1,
     *     "otp" -> 2
     *     "webauthn" -> 2
     *   }
     *
     * @param session
     * @param realm
     * @param topFlow
     * @return map as described above. Never returns null, but can return empty map.
     */
    public static Map<String, Integer> getCredentialTypesToLoAMap(KeycloakSession session, RealmModel realm, AuthenticationFlowModel topFlow) {
        // Attempt to cache mapping, so it is not needed to compute it multiple times at every authentication
        String cacheKey = "flow:" + topFlow.getId();
        if (realm instanceof CachedRealmModel) {
            ConcurrentHashMap cachedWith = ((CachedRealmModel) realm).getCachedWith();
            Map<String, Integer> result = (Map<String, Integer>) cachedWith.get(cacheKey);
            if (result != null) return result;
        }

        Map<String, Integer> result = new HashMap<>();
        AtomicReference<Integer> currentLevel = new AtomicReference<>(NO_LOA);
        Set<String> availableCredentialTypes = AuthenticatorUtil.getCredentialProviders(session)
                .map(CredentialProvider::getType)
                .collect(Collectors.toSet());

        fillCredentialsToLoAMap(session, realm, topFlow, availableCredentialTypes, currentLevel, result);

        logger.tracef("Computed credential types to LoA map for authentication flow '%s' in realm '%s'. Mapping: %s", topFlow.getAlias(), realm.getName(), result);

        if (realm instanceof CachedRealmModel) {
            ConcurrentHashMap cachedWith = ((CachedRealmModel) realm).getCachedWith();
            cachedWith.put(cacheKey, result);
        }

        return result;
    }

    private static void fillCredentialsToLoAMap(KeycloakSession session, RealmModel realm, AuthenticationFlowModel authFlow, Set<String> availableCredentialTypes, AtomicReference<Integer> currentLevel, Map<String, Integer> result) {
        realm.getAuthenticationExecutionsStream(authFlow.getId()).forEachOrdered(execution -> {
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());

                int levelWhenExecuted = currentLevel.get();
                fillCredentialsToLoAMap(session, realm, subFlow, availableCredentialTypes, currentLevel, result);
                currentLevel.set(levelWhenExecuted); // Subflow is finished. We should "reset" current level and set it to the same value before we started to process the subflow
            } else {
                if (ConditionalLoaAuthenticatorFactory.PROVIDER_ID.equals(execution.getAuthenticator())) {
                    AuthenticatorConfigModel loaConditionConfig = realm.getAuthenticatorConfigById(execution.getAuthenticatorConfig());
                    Integer level = getLevelFromLoaConditionConfiguration(loaConditionConfig);
                    if (level != null) {
                        currentLevel.set(level);
                    }
                } else {
                    AuthenticatorFactory factory = (AuthenticatorFactory) session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, execution.getAuthenticator());
                    if (factory == null) return;
                    // reference-category points to the credentialType
                    if (factory.getReferenceCategory() != null && availableCredentialTypes.contains(factory.getReferenceCategory())) {
                        result.put(factory.getReferenceCategory(), currentLevel.get());
                    }
                }
            }
        });
    }

}
