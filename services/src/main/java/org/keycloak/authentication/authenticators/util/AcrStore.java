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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.CredentialAction;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

import static org.keycloak.models.Constants.NO_LOA;

/**
 * CRUD data in the authentication session, which are related to step-up authentication
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AcrStore {

    private static final Logger logger = Logger.getLogger(AcrStore.class);

    private final KeycloakSession session;
    private final AuthenticationSessionModel authSession;

    public AcrStore(KeycloakSession session, AuthenticationSessionModel authSession) {
        this.session = session;
        this.authSession = authSession;
    }


    public boolean isLevelOfAuthenticationForced() {
        return Boolean.parseBoolean(authSession.getClientNote(Constants.FORCE_LEVEL_OF_AUTHENTICATION));
    }


    public int getRequestedLevelOfAuthentication(AuthenticationFlowModel executionModel) {
        String requiredLoa = authSession.getClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION);
        int requestedLoaByClient = requiredLoa == null ? NO_LOA : Integer.parseInt(requiredLoa);
        int requestedLoaByKcAction = getRequestedLevelOfAuthenticationByKcAction(executionModel);
        logger.tracef("Level requested by client: %d, level requested by kc_action parameter: %d", requestedLoaByClient, requestedLoaByKcAction);
        return Math.max(requestedLoaByClient, requestedLoaByKcAction);
    }

    //
    private int getRequestedLevelOfAuthenticationByKcAction(AuthenticationFlowModel topLevelFlow) {
        RealmModel realm = authSession.getRealm();
        UserModel user = authSession.getAuthenticatedUser();
        String kcAction = authSession.getClientNote(Constants.KC_ACTION);
        if (user != null && kcAction != null) {
            RequiredActionProvider reqAction = session.getProvider(RequiredActionProvider.class, kcAction);
            if (reqAction instanceof CredentialAction) {
                String credentialType = ((CredentialAction) reqAction).getCredentialType(session, authSession);
                if (credentialType != null) {
                    Map<String, Integer> credentialTypesToLoa = LoAUtil.getCredentialTypesToLoAMap(session, realm, topLevelFlow);

                    Integer credentialTypeLevel = credentialTypesToLoa.get(credentialType);
                    if (credentialTypeLevel != null) {
                        // We check if user has any credentials of given type available. For instance if user doesn't yet have any 2nd-factor configured, we don't request level2 from him
                        MultivaluedHashMap<Integer, String> loaToCredentialTypes = reverse(credentialTypesToLoa);
                        return getHighestLevelAvailableForUser(user, loaToCredentialTypes, credentialTypeLevel);
                    }
                }
            }
        }
        return NO_LOA;
    }

    private MultivaluedHashMap<Integer, String> reverse(Map<String, Integer> orig) {
        MultivaluedHashMap<Integer, String> reverse = new MultivaluedHashMap<>();
        orig.forEach((key, value) -> reverse.add(value, key));
        return reverse;
    }

    private Integer getHighestLevelAvailableForUser(UserModel user, MultivaluedHashMap<Integer, String> loaToCredentialTypes, int levelToTry) {
        if (levelToTry <= NO_LOA) return levelToTry;

        List<String> currentLevelCredentialTypes = loaToCredentialTypes.get(levelToTry);
        if (currentLevelCredentialTypes == null || currentLevelCredentialTypes.isEmpty()) {
            // No credentials required for authentication on this level
            return levelToTry;
        }

        boolean hasCredentialOfLevel = user.credentialManager().getStoredCredentialsStream()
                .anyMatch(credentialModel -> currentLevelCredentialTypes.contains(credentialModel.getType()));
        if (hasCredentialOfLevel) {
            logger.tracef("User %s has credential of level %d available", user.getUsername(), levelToTry);
            return levelToTry;
        } else {
            // Fallback to lower level
            return getHighestLevelAvailableForUser(user, loaToCredentialTypes, levelToTry - 1);
        }
    }

    public boolean isLevelOfAuthenticationSatisfiedFromCurrentAuthentication(AuthenticationFlowModel topFlow) {
        return getRequestedLevelOfAuthentication(topFlow)
                <= getAuthenticatedLevelCurrentAuthentication();
    }


    public static int getCurrentLevelOfAuthentication(AuthenticatedClientSessionModel clientSession) {
        String clientSessionLoaNote = clientSession.getNote(Constants.LEVEL_OF_AUTHENTICATION);
        return clientSessionLoaNote == null ? NO_LOA : Integer.parseInt(clientSessionLoaNote);
    }


    /**
     * @param level level of authentication
     * @param maxAge maxAge for which this level is considered valid
     * @return True if the particular level was already authenticated before in this userSession and is still valid
     */
    public boolean isLevelAuthenticatedInPreviousAuth(int level, int maxAge) {
        // In case of re-authentication requested from client (EG. by "prompt=login" or "max_age=0", the LoA from previous authentications are not
        // considered. User needs to re-authenticate all requested levels again.
        if (AuthenticatorUtil.isForcedReauthentication(authSession)) return false;

        Map<Integer, Integer> levels = getCurrentAuthenticatedLevelsMap();
        if (levels == null) return false;

        Integer levelAuthTime = levels.get(level);
        if (levelAuthTime == null) return false;

        int currentTime = Time.currentTime();
        return levelAuthTime + maxAge >= currentTime;
    }


    /**
     * return level, which was either:
     * - directly authenticated in current authentication
     * - or was already verified that can be re-used from previous authentication
     *
     * @return see above
     */
    public int getLevelOfAuthenticationFromCurrentAuthentication() {
        String authSessionLoaNote = authSession.getAuthNote(Constants.LEVEL_OF_AUTHENTICATION);
        return authSessionLoaNote == null ? NO_LOA : Integer.parseInt(authSessionLoaNote);
    }


    /**
     * Save authenticated level to authenticationSession (for current authentication) and loa map (for future authentications)
     *
     * @param level level to save
     */
    public void setLevelAuthenticated(int level) {
        setLevelAuthenticatedToCurrentRequest(level);
        setLevelAuthenticatedToMap(level);
    }

    /**
     * Set level to the current authentication session
     *
     * @param level, which was authenticated by user
     */
    public void setLevelAuthenticatedToCurrentRequest(int level) {
        authSession.setAuthNote(Constants.LEVEL_OF_AUTHENTICATION, String.valueOf(level));
    }

    /**
     * Set level to the current authentication session if an auth flow loa is present and is higher then the current loa
     */
    public void setAuthFlowLevelAuthenticatedToCurrentRequest() {
        if (authSession.getAuthNote(Constants.AUTHENTICATION_FLOW_LEVEL_OF_AUTHENTICATION) != null) {
            int authFlowLoa = Integer.parseInt(authSession.getAuthNote(Constants.AUTHENTICATION_FLOW_LEVEL_OF_AUTHENTICATION));
            if (getLevelOfAuthenticationFromCurrentAuthentication() < authFlowLoa) {
                setLevelAuthenticatedToCurrentRequest(authFlowLoa);
            }
        }
    }

    private void setLevelAuthenticatedToMap(int level) {
        Map<Integer, Integer> levels = getCurrentAuthenticatedLevelsMap();
        if (levels == null) levels = new HashMap<>();

        levels.put(level, Time.currentTime());

        saveCurrentAuthenticatedLevelsMap(levels);
    }


    private int getAuthenticatedLevelCurrentAuthentication() {
        String authSessionLoaNote = authSession.getAuthNote(Constants.LEVEL_OF_AUTHENTICATION);
        return authSessionLoaNote == null ? NO_LOA : Integer.parseInt(authSessionLoaNote);
    }

    /**
     * @return highest authenticated level from previous authentication, which is still valid (not yet expired)
     */
    public int getHighestAuthenticatedLevelFromPreviousAuthentication(String flowId) {
        // No map found. User was not yet authenticated in this session
        Map<Integer, Integer> levels = getCurrentAuthenticatedLevelsMap();
        if (levels == null || levels.isEmpty()) return NO_LOA;

        // Map was already saved, so it is SSO authentication at minimum. Using "0" level as the minimum level in this case
        int maxLevel = Constants.MINIMUM_LOA;
        int currentTime = Time.currentTime();

        Map<Integer, Integer> configuredMaxAges = LoAUtil.getLoaMaxAgesConfiguredInRealmFlow(authSession.getRealm(), flowId);
        levels = new TreeMap<>(levels);

        for (Map.Entry<Integer, Integer> entry : levels.entrySet()) {
            Integer levelMaxAge = configuredMaxAges.get(entry.getKey());
            if (levelMaxAge == null) {
                logger.warnf("No condition found for level '%d' in the authentication flow", entry.getKey());
                levelMaxAge = 0;
            }
            int levelAuthTime = entry.getValue();
            int levelExpiration = levelAuthTime + levelMaxAge;
            if (currentTime <= levelExpiration) {
                maxLevel = entry.getKey();
            } else {
                break;
            }
        }

        logger.tracef("Highest authenticated level from previous authentication of client '%s' in authentication '%s' was: %d",
                authSession.getClient().getClientId(), authSession.getParentSession().getId(), maxLevel);
        return maxLevel;
    }

    // Key is level number. Value is level authTime
    private Map<Integer, Integer> getCurrentAuthenticatedLevelsMap() {
        String loaMap = authSession.getAuthNote(Constants.LOA_MAP);
        if (loaMap == null) {
            return null;
        }
        try {
            return JsonSerialization.readValue(loaMap, new TypeReference<Map<Integer, Integer>>() {});
        } catch (IOException e) {
            logger.warnf("Invalid format of the LoA map. Saved value was: %s", loaMap);
            throw new IllegalStateException(e);
        }
    }

    private void saveCurrentAuthenticatedLevelsMap(Map<Integer, Integer> levelInfoMap) {
        try {
            String note = JsonSerialization.writeValueAsString(levelInfoMap);
            authSession.setAuthNote(Constants.LOA_MAP, note);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
