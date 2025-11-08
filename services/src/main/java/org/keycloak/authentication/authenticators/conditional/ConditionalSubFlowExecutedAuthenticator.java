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

package org.keycloak.authentication.authenticators.conditional;

import java.util.stream.Stream;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

/**
 * <p>Conditional authenticator to know if a sub-flow was executed successfully in the authentication flow.</p>
 *
 * @author rmartinc
 */
public class ConditionalSubFlowExecutedAuthenticator implements ConditionalAuthenticator {

    protected static final ConditionalSubFlowExecutedAuthenticator SINGLETON = new ConditionalSubFlowExecutedAuthenticator();
    private static final Logger logger = Logger.getLogger(ConditionalSubFlowExecutedAuthenticator.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        final AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null || configModel.getConfig() == null) {
            logger.warnf("No configuration defined for the conditional flow executed. Nothing executed.");
            return false;
        }

        final String flowAlias = configModel.getConfig().get(ConditionalSubFlowExecutedAuthenticatorFactory.FLOW_TO_CHECK);
        final boolean executed = !ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_NOT_EXECUTED.equals(
                configModel.getConfig().get(ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT));
        if (flowAlias == null) {
            logger.warnf("No flow configured in the option '%s'. Nothing executed.", ConditionalSubFlowExecutedAuthenticatorFactory.FLOW_TO_CHECK);
            return !executed;
        }

        final RealmModel realm = context.getRealm();
        final AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.warnf("No flow '%s' defined in the realm. Nothing executed.", flowAlias);
            return !executed;
        }

        final AuthenticationExecutionModel exec = locateExecutionFlowToCheck(realm, context.getTopLevelFlow().getId(), flow.getId());
        if (exec == null) {
            logger.warnf("Cannot locate execution for flow '%s' in the top level flow '%s'. Nothing executed.", flowAlias, context.getTopLevelFlow().getAlias());
            return !executed;
        }

        AuthenticationSessionModel.ExecutionStatus status = context.getAuthenticationSession().getExecutionStatus().get(exec.getId());
        logger.tracef("The authentication status for the flow '%s' is %s", flowAlias, status);
        return executed
                ? AuthenticationSessionModel.ExecutionStatus.SUCCESS.equals(status)
                : !AuthenticationSessionModel.ExecutionStatus.SUCCESS.equals(status);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // no-op
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    private Stream<AuthenticationExecutionModel> flattened(RealmModel realm, AuthenticationExecutionModel flowExec) {
        // flatten the execution model recursively only for flows
        return Stream.concat(Stream.of(flowExec),
                realm.getAuthenticationExecutionsStream(flowExec.getFlowId())
                        .filter(AuthenticationExecutionModel::isAuthenticatorFlow)
                        .flatMap(exec -> flattened(realm, exec)));
    }

    private AuthenticationExecutionModel locateExecutionFlowToCheck(RealmModel realm, String topFlowId, String flowId) {
        return realm.getAuthenticationExecutionsStream(topFlowId)
                .filter(AuthenticationExecutionModel::isAuthenticatorFlow)
                .flatMap(exec -> flattened(realm, exec))
                .filter(exec -> flowId.equals(exec.getFlowId()))
                .findAny()
                .orElse(null);
    }
}
