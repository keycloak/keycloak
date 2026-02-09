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
 */

package org.keycloak.quarkus.runtime.integration.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;

import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler;
import org.keycloak.utils.KeycloakSessionUtil;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class KeycloakBeanProducer implements TransactionalSessionHandler {

    @RequestScoped
    public KeycloakSession getKeycloakSession() {
        return create();
    }

    void dispose(@Disposes KeycloakSession session) {
        KeycloakSessionUtil.setKeycloakSession(null);
        close(session);
    }
}
