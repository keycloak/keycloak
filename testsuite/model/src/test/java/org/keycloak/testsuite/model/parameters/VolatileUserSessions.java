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
 */
package org.keycloak.testsuite.model.parameters;

import java.util.Collections;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.KeycloakModelParameters;

public class VolatileUserSessions extends KeycloakModelParameters {

    public VolatileUserSessions() {
        super(Collections.emptySet(), Collections.emptySet());
    }

    @Override
    public void updateConfig(Config cf) {
        updateConfigForJpa(cf);
    }

    public static void updateConfigForJpa(Config cf) {
        System.getProperties().put(PropertiesProfileConfigResolver.getPropertyKey(Profile.Feature.PERSISTENT_USER_SESSIONS), "disabled");
    }
}
