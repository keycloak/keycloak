/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;

public class RiskScoreAuthenticator extends OTPFormAuthenticator {

    public static final int DEFAULT_FAILURE_THRESHOLD = 3;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        UserLoginFailureModel failures = context.getSession().loginFailures()
                .getUserLoginFailure(context.getRealm(), user.getId());

        if (failures == null || failures.getNumFailures() < getFailureThreshold(context.getAuthenticatorConfig())) {
            context.success();
            return;
        }

        if (!super.configuredFor(context.getSession(), context.getRealm(), user)) {
            setRequiredActions(context.getSession(), context.getRealm(), user);
            context.success();
            return;
        }

        super.authenticate(context);
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    private int getFailureThreshold(AuthenticatorConfigModel config) {
        if (config == null) {
            return DEFAULT_FAILURE_THRESHOLD;
        }

        try {
            int threshold = Integer.parseInt(config.getConfig().get(RiskScoreAuthenticatorFactory.FAILURE_THRESHOLD));
            return threshold > 0 ? threshold : DEFAULT_FAILURE_THRESHOLD;
        } catch (NullPointerException | NumberFormatException e) {
            return DEFAULT_FAILURE_THRESHOLD;
        }
    }
}
