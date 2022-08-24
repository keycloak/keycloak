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

package org.keycloak.testsuite.model.singleUseObject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.common.util.Time;
import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.models.ActionTokenStoreProvider;
import org.keycloak.models.ActionTokenValueModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;

@RequireProvider(ActionTokenStoreProvider.class)
@RequireProvider(SingleUseObjectProvider.class)
public class SingleUseObjectModelTest extends KeycloakModelTest {

    private String realmId;

    private String userId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("realm");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realmId = realm.getId();
        UserModel user = s.users().addUser(realm, "user");
        userId = user.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        Time.setOffset(0);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testActionTokens() {
        ActionTokenKeyModel key = withRealm(realmId, (session, realm) -> {
            ActionTokenStoreProvider actionTokenStore = session.getProvider(ActionTokenStoreProvider.class);
            DefaultActionTokenKey actionTokenKey = new DefaultActionTokenKey(userId, UUID.randomUUID().toString(), Time.currentTime() + 60, null);
            Map<String, String> notes = new HashMap<>();
            notes.put("foo", "bar");
            actionTokenStore.put(actionTokenKey, notes);
            return actionTokenKey;
        });

        inComittedTransaction(session -> {
            ActionTokenStoreProvider actionTokenStore = session.getProvider(ActionTokenStoreProvider.class);
            ActionTokenValueModel valueModel = actionTokenStore.get(key);
            Assert.assertNotNull(valueModel);
            Assert.assertEquals("bar", valueModel.getNote("foo"));

            valueModel = actionTokenStore.remove(key);
            Assert.assertNotNull(valueModel);
            Assert.assertEquals("bar", valueModel.getNote("foo"));
        });

        inComittedTransaction(session -> {
            ActionTokenStoreProvider actionTokenStore = session.getProvider(ActionTokenStoreProvider.class);
            ActionTokenValueModel valueModel = actionTokenStore.get(key);
            Assert.assertNull(valueModel);

            Map<String, String> notes = new HashMap<>();
            notes.put("foo", "bar");
            actionTokenStore.put(key, notes);
        });

        inComittedTransaction(session -> {
            ActionTokenStoreProvider actionTokenStore = session.getProvider(ActionTokenStoreProvider.class);
            ActionTokenValueModel valueModel = actionTokenStore.get(key);
            Assert.assertNotNull(valueModel);
            Assert.assertEquals("bar", valueModel.getNote("foo"));

            Time.setOffset(70);

            valueModel = actionTokenStore.get(key);
            Assert.assertNull(valueModel);
        });
    }

    @Test
    public void testSingleUseStore() {
        String key = UUID.randomUUID().toString();
        Map<String, String> notes = new HashMap<>();
        notes.put("foo", "bar");

        Map<String, String> notes2 = new HashMap<>();
        notes2.put("baf", "meow");

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.getProvider(SingleUseObjectProvider.class);
            Assert.assertFalse(singleUseStore.replace(key, notes2));

            singleUseStore.put(key,  60, notes);
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.getProvider(SingleUseObjectProvider.class);
            Map<String, String> actualNotes = singleUseStore.get(key);
            Assert.assertEquals(notes, actualNotes);

            Assert.assertTrue(singleUseStore.replace(key, notes2));
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.getProvider(SingleUseObjectProvider.class);
            Map<String, String> actualNotes = singleUseStore.get(key);
            Assert.assertEquals(notes2, actualNotes);

            Assert.assertFalse(singleUseStore.putIfAbsent(key, 60));

            Assert.assertEquals(notes2, singleUseStore.remove(key));
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.getProvider(SingleUseObjectProvider.class);
            Assert.assertTrue(singleUseStore.putIfAbsent(key, 60));
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.getProvider(SingleUseObjectProvider.class);
            Map<String, String> actualNotes = singleUseStore.get(key);
            assertThat(actualNotes, Matchers.anEmptyMap());

            Time.setOffset(70);

            Assert.assertNull(singleUseStore.get(key));
        });
    }
}
