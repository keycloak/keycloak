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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RealmModelDelegate;
import org.keycloak.representations.idm.RealmRepresentation;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MigrateTo27_0_0Test {

    private final MigrateTo27_0_0 migrator = new MigrateTo27_0_0();

    @Test
    public void migrateRealmUpdatesBuiltinLoginThemes() {
        assertLoginThemeMigrated("keycloak.v2", "keycloak.v3");
        assertLoginThemeMigrated("rh-sso.v2", "keycloak.v3");
    }

    @Test
    public void migrateRealmLeavesCustomLoginTheme() {
        assertLoginThemeMigrated("my-custom-theme", "my-custom-theme");
    }

    @Test
    public void migrateRealmUpdatesClientLoginThemes() {
        Map<String, String> v2ClientAttrs = new HashMap<>();
        v2ClientAttrs.put("login_theme", "keycloak.v2");
        ClientModel v2Client = clientWithAttributes(v2ClientAttrs);

        Map<String, String> rhSsoClientAttrs = new HashMap<>();
        rhSsoClientAttrs.put("login_theme", "rh-sso.v2");
        ClientModel rhSsoClient = clientWithAttributes(rhSsoClientAttrs);

        Map<String, String> customClientAttrs = new HashMap<>();
        customClientAttrs.put("login_theme", "my-theme");
        ClientModel customClient = clientWithAttributes(customClientAttrs);

        RealmModel realm = realmWithClients(Stream.of(v2Client, rhSsoClient, customClient));
        migrator.migrateRealm(null, realm);

        assertThat(v2ClientAttrs.get("login_theme"), is("keycloak.v3"));
        assertThat(rhSsoClientAttrs.get("login_theme"), is("keycloak.v3"));
        assertThat(customClientAttrs.get("login_theme"), is("my-theme"));
    }

    @Test
    public void migrateImportUpdatesLoginTheme() {
        RealmModel realm = realmWithLoginTheme("keycloak.v2");
        migrator.migrateImport(null, realm, new RealmRepresentation(), false);
        assertThat(realm.getLoginTheme(), is("keycloak.v3"));
    }

    private void assertLoginThemeMigrated(String before, String after) {
        RealmModel realm = realmWithLoginTheme(before);
        migrator.migrateRealm(null, realm);
        assertThat(realm.getLoginTheme(), is(equalTo(after)));
    }

    private static RealmModel realmWithLoginTheme(String loginTheme) {
        return new RealmModelDelegate(null) {
            private String theme = loginTheme;

            @Override
            public String getLoginTheme() {
                return theme;
            }

            @Override
            public void setLoginTheme(String theme) {
                this.theme = theme;
            }

            @Override
            public Stream<ClientModel> getClientsStream() {
                return Stream.empty();
            }
        };
    }

    private static RealmModel realmWithClients(Stream<ClientModel> clients) {
        return new RealmModelDelegate(null) {
            @Override
            public String getLoginTheme() {
                return null;
            }

            @Override
            public Stream<ClientModel> getClientsStream() {
                return clients;
            }
        };
    }

    private static ClientModel clientWithAttributes(Map<String, String> attributes) {
        return (ClientModel) java.lang.reflect.Proxy.newProxyInstance(
                ClientModel.class.getClassLoader(),
                new Class<?>[] { ClientModel.class },
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getAttribute" -> attributes.get(args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], (String) args[1]);
                            yield null;
                        }
                        case "getAttributes" -> attributes;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "TestClient";
                        default -> defaultValue(method.getReturnType());
                    };
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType.isPrimitive()) {
            throw new UnsupportedOperationException("Unhandled primitive type: " + returnType);
        }
        return null;
    }
}
