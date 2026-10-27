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
package org.keycloak.authorization.fgap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.runWithoutAuthorization;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * Creates a {@link FGAPAdminUserModel} proxy whose operations are never subject to FGAP query filtering.
 *
 * <p>Every method is invoked within {@link AdminPermissionsSchema#runWithoutAuthorization(KeycloakSession, Supplier)}.
 * Streams returned by the proxy remain lazy; both their creation and consumption run unfiltered.
 */
final class UnfilteredUserModelInvocationHandler implements InvocationHandler {

    private final KeycloakSession session;
    private final UserModel delegate;

    private UnfilteredUserModelInvocationHandler(KeycloakSession session, UserModel delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    static FGAPAdminUserModel wrap(KeycloakSession session, UserModel user) {
        if (user instanceof FGAPAdminUserModel admin) {
            return admin;
        }
        return (FGAPAdminUserModel) Proxy.newProxyInstance(FGAPAdminUserModel.class.getClassLoader(),
                new Class[] { FGAPAdminUserModel.class }, new UnfilteredUserModelInvocationHandler(session, user));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "equals" -> proxy == args[0] || delegate.equals(args[0]);
                case "hashCode" -> delegate.hashCode();
                default -> delegate.toString();
            };
        }

        Object result = runWithoutAuthorization(session, () -> {
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                return e;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });

        if (result instanceof InvocationTargetException e) {
            throw e.getCause();
        }

        if (result instanceof Stream<?> source) {
            Spliterator<?> spliterator = runWithoutAuthorization(session, source::spliterator);
            return closing(StreamSupport.stream(new UnfilteredSpliterator<>(spliterator), false)
                    .onClose(() -> runWithoutAuthorization(session, source::close)));
        }

        return result;
    }

    private final class UnfilteredSpliterator<T> implements Spliterator<T> {

        private final Spliterator<T> delegate;

        private UnfilteredSpliterator(Spliterator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            return runWithoutAuthorization(session, () -> delegate.tryAdvance(action));
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            runWithoutAuthorization(session, () -> delegate.forEachRemaining(action));
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return delegate.estimateSize();
        }

        @Override
        public int characteristics() {
            return delegate.characteristics();
        }

        @Override
        public Comparator<? super T> getComparator() {
            return delegate.getComparator();
        }
    }
}
