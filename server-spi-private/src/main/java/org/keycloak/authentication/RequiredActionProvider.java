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

package org.keycloak.authentication;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.provider.Provider;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.RequiredActionHelper;



/**
 * RequiredAction provider.  Required actions are one-time actions that a user must perform before they are logged in.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionProvider extends Provider {

    /**
     * Determines what type of support is provided for application-initiated
     * actions.
     * 
     * @return InititatedActionsSupport
     */
    default InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.NOT_SUPPORTED;
    }

    /**
     * Callback to let the action know that an application-initiated action
     * was canceled.
     *
     * @param session The Keycloak session.
     * @param authSession The authentication session.
     *
     */
    default void initiatedActionCanceled(KeycloakSession session, AuthenticationSessionModel authSession) {
        return;
    }
    
    /**
     * Called every time a user authenticates.  This checks to see if this required action should be triggered.
     * The implementation of this method is responsible for setting the required action on the UserModel.
     *
     * For example, the UpdatePassword required actions checks the password policies to see if the password has expired.
     *
     * @param context
     */
    void evaluateTriggers(RequiredActionContext context);

    /**
     * If the user has a required action set, this method will be the initial call to obtain what to display to the
     * user's browser.  Return null if no action should be done.
     *
     * @param context
     * @return
     */
    void requiredActionChallenge(RequiredActionContext context);

    /**
     * Called when a required action has form input you want to process.
     *
     * @param context
     */
    void processAction(RequiredActionContext context);


    /**
     * @deprecated in favor of {@link #getMaxAuthAge(KeycloakSession)} to support individual configuration of max auth age for all required actions. This method has no effect anymore.
     *
     * Defines the max time after a user login, after which re-authentication is requested for an AIA. 0 means that re-authentication is always requested.
     * On default uses configured max_auth_age value from the required action config. If not configured, it uses the default max_auth_age value from the KeycloakConstants class.
     */
    @Deprecated(since = "26.3.0", forRemoval = true)
    default int getMaxAuthAge() {
        return Constants.KC_ACTION_MAX_AGE;
    }

    /**
     * Defines the max time after a user login, after which re-authentication is requested for an AIA. 0 means that re-authentication is always requested.
     * On default uses configured max_auth_age value from the required action config. If not configured, it uses the default max_auth_age value from the KeycloakConstants class.
     */
    default int getMaxAuthAge(KeycloakSession session) {
        if (session == null) {
            // session is null, support for legacy implementation, fallback to default maxAuthAge
            return Constants.KC_ACTION_MAX_AGE;
        }

        KeycloakContext keycloakContext = session.getContext();
        RealmModel realm = keycloakContext.getRealm();
        int maxAge;

        // try required action config
        AuthenticationSessionModel authSession = keycloakContext.getAuthenticationSession();
        if (authSession != null) {

            // we need to figure out the alias for the current required action
            String providerId = authSession.getClientNote(Constants.KC_ACTION);
            RequiredActionProviderModel requiredAction = RequiredActionHelper.getRequiredActionByProviderId(realm, providerId);

            if (requiredAction != null) {
                RequiredActionConfigModel configModel = realm.getRequiredActionConfigByAlias(requiredAction.getAlias());
                if (configModel != null && configModel.containsConfigKey(Constants.MAX_AUTH_AGE_KEY)) {
                    maxAge = RequiredActionFactory.parseMaxAuthAge(configModel);
                    if (maxAge >= 0) {
                        return maxAge;
                    }
                }
            }
        }

        // fallback to default
        return Constants.KC_ACTION_MAX_AGE;
    }

}
