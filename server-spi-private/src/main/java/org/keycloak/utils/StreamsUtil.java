/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamsUtil {

    /**
     * Returns the original stream that is closed on terminating operation.
     *
     * It is used, for example, for closing hibernate provided streams since it is required by hibernate documentation.
     *
     * @param stream the stream which is expected to be closed on termination
     * @return stream that will be closed on terminating operation
     */
    public static <T> Stream<T> closing(Stream<T> stream) {
        return new ClosingStream<>(stream);
    }

    /**
     * Returns the original stream if the stream is not empty. Otherwise throws the provided exception.
     * @param stream Stream to be examined.
     * @param ex Exception to be thrown if the stream is empty.
     * @return Stream
     */
    public static <T> Stream<T> throwIfEmpty(Stream<T> stream, RuntimeException ex) {
        Iterator<T> iterator = stream.iterator();
        if (iterator.hasNext()) {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
        } else {
            throw ex;
        }
    }

    /**
     * Returns the original stream that is limited with {@link Stream#skip(long) skip} and
     * {@link Stream#limit(long) limit} functions based on values of {@code first} and {@code max} parameters.
     * 
     * @param originalStream Stream to be limited.
     * @param first Index of first item to be returned by the stream. Ignored if negative, zero {@code null}.
     * @param max Maximum number of items to be returned by the stream. Ignored if negative or {@code null}.
     * @param <T> Type of items in the stream
     * @return Stream
     */
    public static <T> Stream<T> paginatedStream(Stream<T> originalStream, Integer first, Integer max) {
        if (first != null && first > 0) {
            originalStream = originalStream.skip(first);
        }

        if (max != null && max >= 0) {
            originalStream = originalStream.limit(max);
        }

        return originalStream;
    }

    /**
     * distinctByKey is not supposed to be used with parallel streams
     *
     * To make this method synchronized use {@code ConcurrentHashMap<Object, Boolean>} instead of HashSet
     *
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * A Java stream utility that splits a stream into chunks of a fixed size. Last chunk in
     * the stream might be smaller than the desired size. Ordering guarantees
     * depend on underlying stream.
     *
     * @param <T> The type of the stream
     * @param originalStream The original stream
     * @param chunkSize The chunk size
     * @return The stream in chunks
     */
    public static <T> Stream<Collection<T>> chunkedStream(Stream<T> originalStream, int chunkSize) {
        Spliterator<T> source = originalStream.spliterator();
        return StreamSupport.stream(new Spliterator<Collection<T>>() {
            final ArrayList<T> buf = new ArrayList<>();

            @Override
            public boolean tryAdvance(Consumer<? super Collection<T>> action) {
                while (buf.size() < chunkSize) {
                    if (!source.tryAdvance(buf::add)) {
                        if (!buf.isEmpty()) {
                            action.accept((Collection<T>) buf.clone());
                            buf.clear();
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                action.accept((Collection<T>) buf.clone());
                buf.clear();
                return true;
            }

            @Override
            public Spliterator<Collection<T>> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                long sourceSize = source.estimateSize();
                return sourceSize / chunkSize + (sourceSize % chunkSize != 0? 1 : 0);
            }

            @Override
            public int characteristics() {
                return NONNULL | ORDERED;
            }
        }, false);
    }

    /**
     * This works around a bug in JDK 21 (but no longer in JDK 24) where a sorted stream has all its elements processed
     * when used inside of a flatmap and a terminal operation like limit() is used outside of it.
     * See StreamUtilTests.testSortedInsideOfFlatMapShouldRespectTerminalOperation for an example.
     * Possible <a href="https://bugs.openjdk.org/browse/JDK-8196106">JDK-8196106</a> as the reference to the bug.
     *
     * @param <T> The type of the stream
     * @param originalStream The original stream
     * @return The stream that is lazily evaluating
     */
    public static <T> Stream<T> prepareSortedStreamToWorkInsideOfFlatMapWithTerminalOperations(Stream<T> originalStream) {
        return StreamSupport.stream(originalStream.spliterator(), false);
    }

}
