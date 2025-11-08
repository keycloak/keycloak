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

package org.keycloak.testsuite.util;

import java.util.Set;

import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class AuthenticatorUtilTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void variousFactoryProviders() {
        testingClient.server().run(session -> {

            RealmModel realm = session.realms().getRealmByName(TEST_REALM_NAME);
            assertThat(realm, notNullValue());

            ClientModel client = realm.getClientByClientId("test-app");
            assertThat(client, notNullValue());

            AuthenticationSessionModel authSession = session.authenticationSessions().createRootAuthenticationSession(realm)
                    .createAuthenticationSession(client);
            assertThat(authSession, notNullValue());

            Set<String> callbacksFactories = AuthenticatorUtil.getAuthCallbacksFactoryIds(authSession);
            assertThat(callbacksFactories, notNullValue());
            assertThat(callbacksFactories, Matchers.empty());

            AuthenticatorUtil.setAuthCallbacksFactoryIds(authSession, "factory1");
            callbacksFactories = AuthenticatorUtil.getAuthCallbacksFactoryIds(authSession);
            assertThat(callbacksFactories, notNullValue());
            assertThat(callbacksFactories.size(), is(1));

            String note = authSession.getAuthNote(AuthenticatorUtil.CALLBACKS_FACTORY_IDS_NOTE);
            assertThat(note, notNullValue());
            assertThat(note, is("factory1"));

            AuthenticatorUtil.setAuthCallbacksFactoryIds(authSession, "factory2");
            callbacksFactories = AuthenticatorUtil.getAuthCallbacksFactoryIds(authSession);
            assertThat(callbacksFactories, notNullValue());
            assertThat(callbacksFactories.size(), is(2));

            note = authSession.getAuthNote(AuthenticatorUtil.CALLBACKS_FACTORY_IDS_NOTE);
            assertThat(note, notNullValue());
            assertThat(note, is("factory1" + Constants.CFG_DELIMITER + "factory2"));

            AuthenticatorUtil.setAuthCallbacksFactoryIds(authSession, "factory1");
            callbacksFactories = AuthenticatorUtil.getAuthCallbacksFactoryIds(authSession);
            assertThat(callbacksFactories, notNullValue());
            assertThat(callbacksFactories.size(), is(2));

            note = authSession.getAuthNote(AuthenticatorUtil.CALLBACKS_FACTORY_IDS_NOTE);
            assertThat(note, notNullValue());
            assertThat(note, is("factory1" + Constants.CFG_DELIMITER + "factory2"));

            AuthenticatorUtil.setAuthCallbacksFactoryIds(authSession, "");
            callbacksFactories = AuthenticatorUtil.getAuthCallbacksFactoryIds(authSession);
            assertThat(callbacksFactories, notNullValue());
            assertThat(callbacksFactories.size(), is(2));

            note = authSession.getAuthNote(AuthenticatorUtil.CALLBACKS_FACTORY_IDS_NOTE);
            assertThat(note, notNullValue());
            assertThat(note, is("factory1" + Constants.CFG_DELIMITER + "factory2"));

            AuthenticatorUtil.setAuthCallbacksFactoryIds(authSession, null);
            callbacksFactories = AuthenticatorUtil.getAuthCallbacksFactoryIds(authSession);
            assertThat(callbacksFactories, notNullValue());
            assertThat(callbacksFactories.size(), is(2));

            note = authSession.getAuthNote(AuthenticatorUtil.CALLBACKS_FACTORY_IDS_NOTE);
            assertThat(note, notNullValue());
            assertThat(note, is("factory1" + Constants.CFG_DELIMITER + "factory2"));
        });
    }
}
