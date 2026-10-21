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
    public void testAutoClosingOfClosingStreamOuter() {
        AtomicBoolean closed = new AtomicBoolean();
        StreamsUtil.closing(Stream.of(1, 2, 3)).onClose(() -> closed.set(true)).forEach(NOOP);

        Assert.assertTrue(closed.get());
    }

    @Test
    public void testAutoClosingOfClosingStreamFlatMap() {
        AtomicBoolean closed = new AtomicBoolean();
        Stream.of("value")
          .flatMap(v -> StreamsUtil.closing(Stream.of(1, 2, 3)).onClose(() -> closed.set(true)))
          .forEach(NOOP);

        Assert.assertTrue(closed.get());
    }

    @Test
    public void testAutoClosingOfClosingUsingIterator() {
        AtomicBoolean closed = new AtomicBoolean();
        StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true))).iterator().forEachRemaining(NOOP);

        Assert.assertTrue(closed.get());
    }

    @Test
    public void testAutoClosingOfClosingUsingConcat() {
        AtomicBoolean closed = new AtomicBoolean();
        Stream.concat(
          Stream.of(4, 5),
          StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true)))
        ).iterator().forEachRemaining(NOOP);

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

    @Test
    public void testClosingStreamClosesOnExceptionDuringTerminalOperation() {
        AtomicBoolean closed = new AtomicBoolean();
        try {
            StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true)))
                    .forEach(i -> { throw new RuntimeException("fail"); });
            Assert.fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals("fail", e.getMessage());
        }
        Assert.assertTrue("Stream should be closed even when terminal operation throws", closed.get());
    }

    @Test
    public void testClosingStreamClosesOnExceptionDuringToList() {
        AtomicBoolean closed = new AtomicBoolean();
        try {
            StreamsUtil.closing(Stream.of(1, 2, 3)
                    .map(i -> { if (i == 2) throw new RuntimeException("fail"); return i; })
                    .onClose(() -> closed.set(true)))
                    .toList();
            Assert.fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }
        Assert.assertTrue("Stream should be closed even when toList throws", closed.get());
    }

    @Test
    public void testClosingIntStreamClosesOnExceptionDuringTerminalOperation() {
        AtomicBoolean closed = new AtomicBoolean();
        try {
            StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true)))
                    .mapToInt(i -> i)
                    .forEach(i -> { throw new RuntimeException("fail"); });
            Assert.fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals("fail", e.getMessage());
        }
        Assert.assertTrue("IntStream should be closed even when terminal operation throws", closed.get());
    }

    @Test
    public void testClosingLongStreamClosesOnExceptionDuringTerminalOperation() {
        AtomicBoolean closed = new AtomicBoolean();
        try {
            StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true)))
                    .mapToLong(i -> i)
                    .forEach(i -> { throw new RuntimeException("fail"); });
            Assert.fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals("fail", e.getMessage());
        }
        Assert.assertTrue("LongStream should be closed even when terminal operation throws", closed.get());
    }

    @Test
    public void testClosingDoubleStreamClosesOnExceptionDuringTerminalOperation() {
        AtomicBoolean closed = new AtomicBoolean();
        try {
            StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true)))
                    .mapToDouble(i -> i)
                    .forEach(i -> { throw new RuntimeException("fail"); });
            Assert.fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals("fail", e.getMessage());
        }
        Assert.assertTrue("DoubleStream should be closed even when terminal operation throws", closed.get());
    }

    @Test
    public void testClosingIntStreamAsLongStreamPreservesClose() {
        AtomicBoolean closed = new AtomicBoolean();
        long sum = StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true)))
                .mapToInt(i -> i)
                .asLongStream()
                .sum();
        Assert.assertEquals(6L, sum);
        Assert.assertTrue("Stream should be closed after asLongStream() terminal operation", closed.get());
    }

    @Test
    public void testClosingIntStreamAsDoubleStreamPreservesClose() {
        AtomicBoolean closed = new AtomicBoolean();
        double sum = StreamsUtil.closing(Stream.of(1, 2, 3).onClose(() -> closed.set(true)))
                .mapToInt(i -> i)
                .asDoubleStream()
                .sum();
        Assert.assertEquals(6.0, sum, 0.001);
        Assert.assertTrue("Stream should be closed after asDoubleStream() terminal operation", closed.get());
    }

    @Test
    public void testSortedInsideOfFlatMapShouldRespectTerminalOperation() {
        AtomicInteger numberOfFetchedElements = new AtomicInteger();

        Stream.of(new Object())
                .flatMap(
                        o -> Stream.of(1, 2, 3).peek(integer -> numberOfFetchedElements.incrementAndGet()))
                .limit(1).forEach(NOOP);

        Assert.assertEquals(1, numberOfFetchedElements.get());

        numberOfFetchedElements.set(0);

        Stream.of(new Object())
                .flatMap(
                        o -> Stream.of(1, 2, 3).sorted().peek(integer -> numberOfFetchedElements.incrementAndGet()))
                .limit(1).forEach(NOOP);

        // Expect actually 1, but always delivery 3 on JDK 21, but will work on JDK 24
        // Assert.assertEquals(1, numberOfFetchedElements.get());

        numberOfFetchedElements.set(0);

        Stream.of(new Object())
                .flatMap(
                        o -> StreamsUtil.prepareSortedStreamToWorkInsideOfFlatMapWithTerminalOperations(Stream.of(1, 2, 3).sorted()).peek(integer -> numberOfFetchedElements.incrementAndGet()))
                .limit(1).forEach(NOOP);

        // With the workaround it is only 1 as expected
        Assert.assertEquals(1, numberOfFetchedElements.get());

    }

}
