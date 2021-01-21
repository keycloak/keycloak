/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.condition;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public abstract class AbstractClientPolicyConditionProvider implements ClientPolicyConditionProvider {

    protected final KeycloakSession session;
    protected final ComponentModel componentModel;

    public AbstractClientPolicyConditionProvider(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public boolean isNegativeLogic() {
        return Boolean.valueOf(componentModel.getConfig().getFirst(AbstractClientPolicyConditionProviderFactory.IS_NEGATIVE_LOGIC)).booleanValue();
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }
}
