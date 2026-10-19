/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.migration.migrators;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MigrateTo27_0_0Test {

    private final MigrateTo27_0_0 migrator = new MigrateTo27_0_0();

    @Test
    public void migratesKeycloakV2AdminTheme() {
        RealmModel realm = Mockito.mock(RealmModel.class);
        when(realm.getAdminTheme()).thenReturn("keycloak.v2");

        migrator.migrateRealm(Mockito.mock(KeycloakSession.class), realm);

        verify(realm).setAdminTheme("keycloak.v3");
    }

    @Test
    public void migratesRhSsoV2AdminTheme() {
        RealmModel realm = Mockito.mock(RealmModel.class);
        when(realm.getAdminTheme()).thenReturn("rh-sso.v2");

        migrator.migrateRealm(Mockito.mock(KeycloakSession.class), realm);

        verify(realm).setAdminTheme("keycloak.v3");
    }

    @Test
    public void leavesCustomAdminThemeUnchanged() {
        RealmModel realm = Mockito.mock(RealmModel.class);
        when(realm.getAdminTheme()).thenReturn("my-custom-admin-theme");

        migrator.migrateRealm(Mockito.mock(KeycloakSession.class), realm);

        assertThat(realm.getAdminTheme(), equalTo("my-custom-admin-theme"));
        Mockito.verify(realm, Mockito.never()).setAdminTheme(Mockito.anyString());
    }

    @Test
    public void leavesNullAdminThemeUnchanged() {
        RealmModel realm = Mockito.mock(RealmModel.class);
        when(realm.getAdminTheme()).thenReturn(null);

        migrator.migrateRealm(Mockito.mock(KeycloakSession.class), realm);

        assertThat(realm.getAdminTheme(), nullValue());
        Mockito.verify(realm, Mockito.never()).setAdminTheme(Mockito.anyString());
    }
}
