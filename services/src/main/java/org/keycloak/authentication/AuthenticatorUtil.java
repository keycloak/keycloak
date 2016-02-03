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

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.RealmModel;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticatorUtil {

    public static List<AuthenticationExecutionModel> getEnabledExecutionsRecursively(RealmModel realm, String flowId) {
        List<AuthenticationExecutionModel> executions = new LinkedList<>();
        recurseExecutions(realm, flowId, executions);
        return executions;

    }

    public static void recurseExecutions(RealmModel realm, String flowId, List<AuthenticationExecutionModel> executions) {
        List<AuthenticationExecutionModel> authenticationExecutions = realm.getAuthenticationExecutions(flowId);
        if (authenticationExecutions == null) return;
        for (AuthenticationExecutionModel model : authenticationExecutions) {
            executions.add(model);
            if (model.isAuthenticatorFlow() && model.isEnabled()) {
                recurseExecutions(realm, model.getFlowId(), executions);
            }
        }
    }

    public static AuthenticationExecutionModel findExecutionByAuthenticator(RealmModel realm, String flowId, String authProviderId) {
        for (AuthenticationExecutionModel model : realm.getAuthenticationExecutions(flowId)) {
            if (model.isAuthenticatorFlow()) {
                AuthenticationExecutionModel recurse = findExecutionByAuthenticator(realm, model.getFlowId(), authProviderId);
                if (recurse != null) return recurse;

            }
            if (model.getAuthenticator().equals(authProviderId)) {
                return model;
            }
        }
        return null;
    }

    public static boolean isEnabled(RealmModel realm, String flowId, String authProviderId) {
        AuthenticationExecutionModel execution = findExecutionByAuthenticator(realm, flowId, authProviderId);
        if (execution == null) {
            return false;
        }
        return execution.isEnabled();
    }
    public static boolean isRequired(RealmModel realm, String flowId, String authProviderId) {
        AuthenticationExecutionModel execution = findExecutionByAuthenticator(realm, flowId, authProviderId);
        if (execution == null) {
            return false;
        }
        return execution.isRequired();
    }
}
