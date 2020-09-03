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
package org.keycloak.models.map.common;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import static java.util.Spliterator.IMMUTABLE;

/**
 *
 * @author hmlnarik
 */
public class StreamUtils {

    public static final class Pair<T1, T2> {
        private final T1 k;
        private final T2 v;

        public Pair(T1 k, T2 v) {
            this.k = k;
            this.v = v;
        }

        public T1 getK() {
            return k;
        }

        public T2 getV() {
            return v;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.k);
            hash = 97 * hash + Objects.hashCode(this.v);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Pair<?, ?> other = (Pair<?, ?>) obj;
            if ( ! Objects.equals(this.k, other.k)) {
                return false;
            }
            if ( ! Objects.equals(this.v, other.v)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "[" + k + ", " + v + "]";
        }
    }

    public static abstract class AbstractToPairSpliterator<K, V, M> implements Spliterator<Pair<K, V>> {

        protected final Iterator<K> streamIterator;
        protected final M mapper;
        protected Iterator<V> flatMapIterator;
        protected K currentKey;

        public AbstractToPairSpliterator(Stream<K> stream, M mapper) {
            this.streamIterator = stream.iterator();
            this.mapper = mapper;
        }

        protected abstract void nextKey();
        
        @Override
        public boolean tryAdvance(Consumer<? super Pair<K, V>> action) {
            if (flatMapIterator != null && flatMapIterator.hasNext()) {
                action.accept(new Pair<>(currentKey, flatMapIterator.next()));
                return true;
            }

            nextKey();

            if (flatMapIterator != null && flatMapIterator.hasNext()) {
                action.accept(new Pair<>(currentKey, flatMapIterator.next()));
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<Pair<K, V>> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return IMMUTABLE;
        }
    }

    private static class ToPairSpliterator<K,V> extends AbstractToPairSpliterator<K, V, Function<? super K, Stream<V>>> {

        public ToPairSpliterator(Stream<K> stream, Function<? super K, Stream<V>> mapper) {
            super(stream, mapper);
        }

        @Override
        protected void nextKey() {
            this.flatMapIterator = null;
            while (this.flatMapIterator == null && streamIterator.hasNext()) {
                currentKey = streamIterator.next();
                final Stream<V> vStream = mapper.apply(currentKey);
                this.flatMapIterator = vStream == null ? null : vStream.iterator();
            }
        }
    }

    private static class IterableToPairSpliterator<K,V> extends AbstractToPairSpliterator<K, V, Function<? super K, ? extends Iterable<V>>> {

        public IterableToPairSpliterator(Stream<K> stream, Function<? super K, ? extends Iterable<V>> mapper) {
            super(stream, mapper);
        }

        @Override
        protected void nextKey() {
            this.flatMapIterator = null;
            while (this.flatMapIterator == null && streamIterator.hasNext()) {
                currentKey = streamIterator.next();
                final Iterable<V> vStream = mapper.apply(currentKey);
                this.flatMapIterator = vStream == null ? null : vStream.iterator();
            }
        }
    }


    /**
     * Creates a stream of pairs that join two streams. For each element <i>k</i> from the {@code stream}
     * and each element <i>v</i> obtained from the stream returned by the {@code mapper} for <i>k</i>, generates
     * a stream of pairs <i>(k, v)</i>.
     * <p>
     * Effectively performs equivalent of a {@code LEFT INNER JOIN} SQL operation on streams.
     * 
     * @param <K>
     * @param <V>
     * @param stream
     * @param mapper
     * @return
     */
    public static <K, V> Stream<Pair<K,V>> leftInnerJoinStream(Stream<K> stream, Function<? super K, Stream<V>> mapper) {
        return StreamSupport.stream(() -> new ToPairSpliterator<>(stream, mapper), IMMUTABLE, false);
    }

    /**
     * Creates a stream of pairs that join two streams. For each element <i>k</i> from the {@code stream}
     * and each element <i>v</i> obtained from the {@code Iterable} returned by the {@code mapper} for <i>k</i>, generates
     * a stream of pairs <i>(k, v)</i>.
     * <p>
     * Effectively performs equivalent of a {@code LEFT INNER JOIN} SQL operation on streams.
     *
     * @param <K>
     * @param <V>
     * @param stream
     * @param mapper
     * @return
     */
    public static <K, V> Stream<Pair<K,V>> leftInnerJoinIterable(Stream<K> stream, Function<? super K, ? extends Iterable<V>> mapper) {
        return StreamSupport.stream(() -> new IterableToPairSpliterator<>(stream, mapper), IMMUTABLE, false);
    }

}
