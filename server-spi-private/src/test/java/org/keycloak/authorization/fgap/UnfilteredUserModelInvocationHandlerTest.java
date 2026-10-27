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

import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.keycloak.models.UserModel;
import org.keycloak.utils.StreamsUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UnfilteredUserModelInvocationHandlerTest {

    @Test
    public void preservesNaturalSortOrder() {
        UserModel user = userWithAttributes(Stream.of("b", "a").sorted());

        assertEquals(List.of("a", "b"), user.getAttributeStream("attribute").toList());
    }

    @Test
    public void preservesCustomComparator() {
        Comparator<String> comparator = Comparator.reverseOrder();
        TreeSet<String> attributes = new TreeSet<>(comparator);
        attributes.addAll(List.of("a", "b"));
        UserModel user = userWithAttributes(attributes.stream());

        try (Stream<String> stream = user.getAttributeStream("attribute")) {
            assertSame(comparator, stream.spliterator().getComparator());
        }
    }

    @Test
    public void equalsIsReflexiveForIdentityBasedUser() {
        UserModel user = userWithAttributes(Stream.empty());
        assertTrue(user.equals(user));
    }

    @Test
    public void closesSourceAfterFindFirstWithoutEagerConsumption() {
        AtomicBoolean closed = new AtomicBoolean();
        AtomicInteger consumed = new AtomicInteger();
        UserModel user = userWithAttributes(StreamsUtil.closing(Stream.of("a", "b")
                .peek(value -> consumed.incrementAndGet())
                .onClose(() -> closed.set(true))));

        Stream<String> stream = user.getAttributeStream("attribute");
        assertEquals(0, consumed.get());
        assertEquals("a", stream.findFirst().orElseThrow());
        assertEquals(1, consumed.get());
        assertTrue(closed.get());
    }

    @Test
    public void closesSourceAfterAnyMatch() {
        AtomicBoolean closed = new AtomicBoolean();
        UserModel user = userWithAttributes(StreamsUtil.closing(Stream.of("a", "b")
                .onClose(() -> closed.set(true))));

        assertTrue(user.getAttributeStream("attribute").anyMatch("a"::equals));
        assertTrue(closed.get());
    }

    @Test
    public void closesSourceWhenConsumerThrows() {
        AtomicBoolean closed = new AtomicBoolean();
        IllegalStateException failure = new IllegalStateException("Consumer failed");
        UserModel user = userWithAttributes(StreamsUtil.closing(Stream.of("a", "b")
                .onClose(() -> closed.set(true))));

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                user.getAttributeStream("attribute").anyMatch(value -> {
                    throw failure;
                })));
        assertTrue(closed.get());
    }

    private UserModel userWithAttributes(Stream<String> attributes) {
        UserModel delegate = (UserModel) Proxy.newProxyInstance(UserModel.class.getClassLoader(),
                new Class<?>[] { UserModel.class }, (proxy, method, args) -> {
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("toString".equals(method.getName())) {
                        return UserModel.class.getSimpleName();
                    }
                    if ("getAttributeStream".equals(method.getName())) {
                        return attributes;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return UnfilteredUserModelInvocationHandler.wrap(null, delegate);
    }
}
