/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.testsuite.broker;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;

import java.net.URL;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testutils.KeycloakServer;

/**
 * @author pedroigor
 */
public class BrokerKeyCloakRule extends AbstractKeycloakRule {

    @Override
    protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
        //server.importRealm(getClass().getResourceAsStream("/broker-test/test-realm-with-broker.json"));
        RealmRepresentation realmWithBroker = KeycloakServer.loadJson(getClass().getResourceAsStream("/broker-test/test-realm-with-broker.json"), RealmRepresentation.class);
        manager.importRealm(realmWithBroker);
        URL url = getClass().getResource("/broker-test/test-app-keycloak.json");
        deployApplication("test-app", "/test-app", UserSessionStatusServlet.class, url.getPath(), "manager");
        deployApplication("test-app-allowed-providers", "/test-app-allowed-providers", UserSessionStatusServlet.class, url.getPath(), "manager");
    }

    @Override
    protected String[] getTestRealms() {
        return new String[] {"realm-with-broker"};
    }

}
