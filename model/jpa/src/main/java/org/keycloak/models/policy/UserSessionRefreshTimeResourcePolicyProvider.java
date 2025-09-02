/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.policy;

import static org.keycloak.models.policy.ResourceOperationType.CREATE;
import static org.keycloak.models.policy.ResourceOperationType.LOGIN;

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class UserSessionRefreshTimeResourcePolicyProvider extends AbstractUserResourcePolicyProvider {

    public UserSessionRefreshTimeResourcePolicyProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
    }

    @Override
    protected boolean isActivationEvent(ResourcePolicyEvent event) {
        return super.isActivationEvent(event) || List.of(CREATE, LOGIN).contains(event.getOperation());
    }

    @Override
    protected boolean isResetEvent(ResourcePolicyEvent event) {
        return LOGIN.equals(event.getOperation());
    }
}
