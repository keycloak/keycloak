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
package org.keycloak.userprofile.validator;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Test;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidatorConfig;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrokeringFederatedUsernameHasValueValidatorTest {

    private final BrokeringFederatedUsernameHasValueValidator validator = new BrokeringFederatedUsernameHasValueValidator();

    @Test
    public void acceptsProvidedUsername() {
        assertTrue(validate(false, List.of("jdoe")).isValid());
    }

    @Test
    public void rejectsMissingUsername() {
        assertFalse(validate(false, List.of()).isValid());
    }

    @Test
    public void rejectsEmptyUsername() {
        assertFalse(validate(false, List.of("")).isValid());
    }

    @Test
    public void rejectsWhitespaceUsername() {
        assertFalse(validate(false, List.of("   ")).isValid());
    }

    @Test
    public void rejectsNullUsername() {
        assertFalse(validate(false, java.util.Collections.singletonList(null)).isValid());
    }

    @Test
    public void acceptsMissingUsernameWhenEmailIsUsedAsUsername() {
        assertTrue(validate(true, List.of()).isValid());
    }

    private ValidationContext validate(boolean emailAsUsername, List<String> usernames) {
        RealmModel realm = stub(RealmModel.class, (method, args) ->
                method.getName().equals("isRegistrationEmailAsUsername") ? emailAsUsername : defaultValue(method));
        KeycloakContext keycloakContext = stub(KeycloakContext.class,
                (method, args) -> method.getName().equals("getRealm") ? realm : defaultValue(method));
        KeycloakSession session = stub(KeycloakSession.class,
                (method, args) -> method.getName().equals("getContext") ? keycloakContext : defaultValue(method));

        return validator.validate(usernames, UserModel.USERNAME, new ValidationContext(session), ValidatorConfig.EMPTY);
    }

    private static Object defaultValue(Method method) {
        Class<?> type = method.getReturnType();
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == void.class) return null;
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> iface, BiFunction<Method, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface},
                (proxy, method, args) -> handler.apply(method, args));
    }
}