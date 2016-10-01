/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.authorization;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakAuthorizationServerRule extends AbstractKeycloakRule {

    private final String realmName;

    KeycloakAuthorizationServerRule(String realmName) {
        this.realmName = realmName;
    }

    @Override
    protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
        server.importRealm(getClass().getResourceAsStream("/authorization-test/test-" + realmName + "-realm.json"));
    }

    @Override
    protected String[] getTestRealms() {
        return new String[] {this.realmName};
    }
}
