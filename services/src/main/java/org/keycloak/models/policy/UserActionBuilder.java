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

import java.time.Duration;

import org.keycloak.common.util.KeycloakUriBuilder;

public class UserActionBuilder {

    private final ResourceAction action;

    private UserActionBuilder(ResourceAction action) {
        this.action = action;
    }

    public static UserActionBuilder builder(String providerId) {
        ResourceAction action = new ResourceAction(providerId);
        return new UserActionBuilder(action);
    }

    public ResourceAction build() {
        return action;
    }

    public UserActionBuilder after(Duration duration) {
        action.setAfter(duration.toMillis());
        return this;
    }

    public UserActionBuilder withConfig(String key, String value) {
        action.setConfig(key, value);
        return this;
    }
}
