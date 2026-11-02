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

package org.keycloak.spi.infinispan.impl.embedded;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.keycloak.config.CachingOptions;

/** Uses the Java 20+ named-group API while retaining this module's Java 17 compatibility. */
final class JGroupsTlsNamedGroups {
    private static final String OPTION = CachingOptions.CACHE_EMBEDDED_MTLS_KEY_EXCHANGE_GROUPS_PROPERTY;

    private JGroupsTlsNamedGroups() {
    }

    static String[] validate(SSLContext context, String[] configured) {
        String[] supported;
        try {
            supported = (String[]) SSLParameters.class.getMethod("getNamedGroups")
                    .invoke(context.getSupportedSSLParameters());
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Option '%s' requires a Java runtime with the SSLParameters named-group API (Java 21 or later).".formatted(OPTION), e);
        }
        if ((supported == null || supported.length == 0) && "BCJSSE".equals(context.getProvider().getName())) {
            try {
                var registry = Class.forName("org.bouncycastle.tls.NamedGroup");
                var names = new java.util.ArrayList<String>();
                for (var field : registry.getFields()) {
                    if (field.getType() == int.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        String name = (String) registry.getMethod("getStandardName", int.class).invoke(null, field.getInt(null));
                        names.add(name == null ? field.getName() : name);
                    }
                }
                supported = names.toArray(String[]::new);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Cannot inspect BC TLS named groups", e);
            }
        }
        if (supported == null || supported.length == 0) {
            throw new IllegalArgumentException("Option '%s' cannot be enforced because TLS provider '%s' does not report supported named groups."
                    .formatted(OPTION, context.getProvider().getName()));
        }
        var groups = new LinkedHashSet<String>();
        for (String value : configured) {
            String name = value == null ? "" : value.trim();
            String canonical = Arrays.stream(supported).filter(group -> group.equalsIgnoreCase(name)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported TLS named group '%s' in option '%s' for provider '%s' on Java %s."
                            .formatted(name, OPTION, context.getProvider().getName(), Runtime.version())));
            if (!groups.add(canonical)) {
                throw new IllegalArgumentException("Duplicate TLS named group '%s' in option '%s'.".formatted(name, OPTION));
            }
        }
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Option '%s' must contain at least one TLS named group.".formatted(OPTION));
        }
        return groups.toArray(String[]::new);
    }

    static void apply(SSLParameters parameters, String[] groups) {
        try {
            SSLParameters.class.getMethod("setNamedGroups", String[].class).invoke(parameters, (Object) groups);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot apply option '%s'.".formatted(OPTION), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot apply option '%s'.".formatted(OPTION), e);
        }
    }
}
