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
package org.keycloak.userprofile;

import java.lang.reflect.Proxy;
import java.util.function.UnaryOperator;

import org.keycloak.convert.Converter;
import org.keycloak.convert.ConverterConfig;
import org.keycloak.convert.ConverterContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;

import org.junit.Assert;
import org.junit.Test;

public class AttributeConverterMetadataTest {

    @Test
    public void testMissingConverterPassesValuesThrough() {
        KeycloakSession session = session(null);
        AttributeConverterMetadata metadata = new AttributeConverterMetadata("missing");

        UnaryOperator<Object> converter = metadata.bind(new ConverterContext(session));

        Assert.assertEquals(" foo ", converter.apply(" foo "));
        Assert.assertEquals(" foo ", metadata.convert(session, " foo "));
    }

    @Test
    public void testConverterIsResolvedOnceWhenBound() {
        int[] lookups = new int[1];
        Converter upperCase = (input, context, config) -> input.toString().toUpperCase();
        KeycloakSession session = session(id -> {
            lookups[0]++;
            return "upper".equals(id) ? upperCase : null;
        });
        AttributeConverterMetadata metadata = new AttributeConverterMetadata("upper", ConverterConfig.EMPTY);

        UnaryOperator<Object> converter = metadata.bind(new ConverterContext(session));

        Assert.assertEquals("FOO", converter.apply("foo"));
        Assert.assertEquals("BAR", converter.apply("bar"));
        Assert.assertEquals(1, lookups[0]);
    }

    private static KeycloakSession session(java.util.function.Function<String, Converter> converters) {
        KeycloakContext context = (KeycloakContext) Proxy.newProxyInstance(AttributeConverterMetadataTest.class.getClassLoader(),
                new Class<?>[] { KeycloakContext.class }, (proxy, method, args) -> null);
        return (KeycloakSession) Proxy.newProxyInstance(AttributeConverterMetadataTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getContext" -> context;
                    case "getProvider" -> converters == null ? null : converters.apply((String) args[1]);
                    default -> null;
                });
    }
}
