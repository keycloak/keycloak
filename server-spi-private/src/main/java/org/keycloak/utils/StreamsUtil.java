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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterators;
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
}
