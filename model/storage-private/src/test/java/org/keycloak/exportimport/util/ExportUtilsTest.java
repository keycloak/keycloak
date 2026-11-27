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

package org.keycloak.exportimport.util;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentExportRepresentation;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Exporting components must not expose the config of the (possibly cached) component model, otherwise
 * stripping secrets from the export modifies the model and concurrent exports fail.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/55477">#55477</a>
 */
public class ExportUtilsTest {

    private static final String REALM_ID = "realm-id";

    @Test
    public void exportComponentsCopiesComponentConfig() {
        ComponentModel component = new ComponentModel();
        component.setId("component-id");
        component.setParentId(REALM_ID);
        component.setProviderId("rsa-generated");
        component.setProviderType("org.keycloak.keys.KeyProvider");
        component.getConfig().putSingle("priority", "100");
        component.getConfig().putSingle("privateKey", "secret");

        RealmModel realm = (RealmModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> {
                    if ("getComponentsStream".equals(method.getName()) && args != null && args.length == 1) {
                        return REALM_ID.equals(args[0]) ? Stream.of(component) : Stream.empty();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        MultivaluedHashMap<String, ComponentExportRepresentation> exported = ExportUtils.exportComponents(realm, REALM_ID);

        List<ComponentExportRepresentation> keyProviders = exported.get("org.keycloak.keys.KeyProvider");
        assertThat(keyProviders, hasSize(1));
        MultivaluedHashMap<String, String> exportedConfig = keyProviders.get(0).getConfig();
        assertThat(exportedConfig, is(component.getConfig()));
        assertThat(exportedConfig, not(sameInstance(component.getConfig())));

        // Modifying the export, as StripSecretsUtils does, must leave the component model untouched
        exportedConfig.remove("privateKey");
        exportedConfig.putSingle("priority", "**********");

        assertThat(component.getConfig().getList("privateKey"), contains("secret"));
        assertThat(component.getConfig().getList("priority"), contains("100"));
    }
}
