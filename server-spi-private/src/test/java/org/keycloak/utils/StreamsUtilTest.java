/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class StreamsUtilTest {

    private static final Consumer<Integer> NOOP = o -> {
    };

    @Test
    public void testAutoClosingOfClosingStream() {
        AtomicBoolean closed = new AtomicBoolean();
        StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true))).forEach(NOOP);

        Assert.assertTrue(closed.get());
    }

    @Test
    public void testMultipleClosingHandlersOnClosingStream() {
        AtomicInteger firstHandlerFiringCount = new AtomicInteger();
        AtomicInteger secondHandlerFiringCount = new AtomicInteger();
        AtomicInteger thirdHandlerFiringCount = new AtomicInteger();

        StreamsUtil.closing(Stream.of(1, 2, 3).onClose(firstHandlerFiringCount::incrementAndGet))
                .onClose(secondHandlerFiringCount::incrementAndGet).mapToInt(value -> value)
                .onClose(thirdHandlerFiringCount::incrementAndGet).forEach(value -> {
                });

        Assert.assertEquals(1, firstHandlerFiringCount.get());
        Assert.assertEquals(1, secondHandlerFiringCount.get());
        Assert.assertEquals(1, thirdHandlerFiringCount.get());
    }

    @Test
    public void testLimitOnClosingStream() {
        AtomicInteger numberOfFetchedElements = new AtomicInteger();

        Stream.of(new Object())
                .flatMap(
                        o -> StreamsUtil.closing(Stream.of(1, 2, 3).peek(integer -> numberOfFetchedElements.incrementAndGet())))
                .limit(1).forEach(NOOP);

        Assert.assertEquals(1, numberOfFetchedElements.get());
    }

}
