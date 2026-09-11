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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * This stream will automatically close itself after terminal operation.
 */
class ClosingStream<R> implements Stream<R> {

    private final Stream<R> delegate;

    public ClosingStream(Stream<R> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Stream<R> filter(Predicate<? super R> predicate) {
        return new ClosingStream<>(delegate.filter(predicate));
    }

    @Override
    public <R1> Stream<R1> map(Function<? super R, ? extends R1> mapper) {
        return new ClosingStream<>(delegate.map(mapper));
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super R> mapper) {
        return new ClosingIntStream(delegate.mapToInt(mapper));
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super R> mapper) {
        return new ClosingLongStream(delegate.mapToLong(mapper));
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super R> mapper) {
        return new ClosingDoubleStream(delegate.mapToDouble(mapper));
    }

    @Override
    public <R1> Stream<R1> flatMap(Function<? super R, ? extends Stream<? extends R1>> mapper) {
        return new ClosingStream<>(delegate.flatMap(mapper));
    }

    @Override
    public IntStream flatMapToInt(Function<? super R, ? extends IntStream> mapper) {
        return new ClosingIntStream(delegate.flatMapToInt(mapper));
    }

    @Override
    public LongStream flatMapToLong(Function<? super R, ? extends LongStream> mapper) {
        return new ClosingLongStream(delegate.flatMapToLong(mapper));
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super R, ? extends DoubleStream> mapper) {
        return new ClosingDoubleStream(delegate.flatMapToDouble(mapper));
    }

    @Override
    public Stream<R> distinct() {
        return new ClosingStream<>(delegate.distinct());
    }

    @Override
    public Stream<R> sorted() {
        return new ClosingStream<>(delegate.sorted());
    }

    @Override
    public Stream<R> sorted(Comparator<? super R> comparator) {
        return new ClosingStream<>(delegate.sorted(comparator));
    }

    @Override
    public Stream<R> peek(Consumer<? super R> action) {
        return new ClosingStream<>(delegate.peek(action));
    }

    @Override
    public Stream<R> limit(long maxSize) {
        return new ClosingStream<>(delegate.limit(maxSize));
    }

    @Override
    public Stream<R> skip(long n) {
        return new ClosingStream<>(delegate.skip(n));
    }

    @Override
    public void forEach(Consumer<? super R> action) {
        try {
            delegate.forEach(action);
        } finally {
            close();
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super R> action) {
        try {
            delegate.forEachOrdered(action);
        } finally {
            close();
        }
    }

    @Override
    public Object[] toArray() {
        try {
            return delegate.toArray();
        } finally {
            close();
        }
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        try {
            return delegate.toArray(generator);
        } finally {
            close();
        }
    }

    @Override
    public R reduce(R identity, BinaryOperator<R> accumulator) {
        try {
            return delegate.reduce(identity, accumulator);
        } finally {
            close();
        }
    }

    @Override
    public Optional<R> reduce(BinaryOperator<R> accumulator) {
        try {
            return delegate.reduce(accumulator);
        } finally {
            close();
        }
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super R, U> accumulator, BinaryOperator<U> combiner) {
        try {
            return delegate.reduce(identity, accumulator, combiner);
        } finally {
            close();
        }
    }

    @Override
    public <R1> R1 collect(Supplier<R1> supplier, BiConsumer<R1, ? super R> accumulator, BiConsumer<R1, R1> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        } finally {
            close();
        }
    }

    @Override
    public <R1, A> R1 collect(Collector<? super R, A, R1> collector) {
        try {
            return delegate.collect(collector);
        } finally {
            close();
        }
    }

    @Override
    public Optional<R> min(Comparator<? super R> comparator) {
        try {
            return delegate.min(comparator);
        } finally {
            close();
        }
    }

    @Override
    public Optional<R> max(Comparator<? super R> comparator) {
        try {
            return delegate.max(comparator);
        } finally {
            close();
        }
    }

    @Override
    public long count() {
        try {
            return delegate.count();
        } finally {
            close();
        }
    }

    @Override
    public boolean anyMatch(Predicate<? super R> predicate) {
        try {
            return delegate.anyMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public boolean allMatch(Predicate<? super R> predicate) {
        try {
            return delegate.allMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public boolean noneMatch(Predicate<? super R> predicate) {
        try {
            return delegate.noneMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public Optional<R> findFirst() {
        try {
            return delegate.findFirst();
        } finally {
            close();
        }
    }

    @Override
    public Optional<R> findAny() {
        try {
            return delegate.findAny();
        } finally {
            close();
        }
    }

    @Override
    public Iterator<R> iterator() {
        return new ClosingIterator(delegate.iterator());
    }

    @Override
    public Spliterator<R> spliterator() {
        return new ClosingSpliterator(delegate.spliterator());
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    @Override
    public Stream<R> sequential() {
        return new ClosingStream<>(delegate.sequential());
    }

    @Override
    public Stream<R> parallel() {
        return new ClosingStream<>(delegate.parallel());
    }

    @Override
    public Stream<R> unordered() {
        return new ClosingStream<>(delegate.unordered());
    }

    @Override
    public Stream<R> onClose(Runnable closeHandler) {
        return new ClosingStream<>(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }

    private class ClosingIterator implements Iterator<R> {

        private final Iterator<R> iterator;

        public ClosingIterator(Iterator<R> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            boolean res;
            try {
                res = iterator.hasNext();
            } catch (RuntimeException | Error e) {
                close();
                throw e;
            }
            if (!res) {
                close();
            }
            return res;
        }

        @Override
        public R next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }

        @Override
        public void forEachRemaining(Consumer<? super R> action) {
            try {
                iterator.forEachRemaining(action);
            } finally {
                close();
            }
        }
    }

    private class ClosingSpliterator implements Spliterator<R> {

        private final Spliterator<R> spliterator;

        public ClosingSpliterator(Spliterator<R> spliterator) {
            this.spliterator = spliterator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            boolean res;
            try {
                res = spliterator.tryAdvance(action);
            } catch (RuntimeException | Error e) {
                close();
                throw e;
            }
            if (!res) {
                close();
            }
            return res;
        }

        @Override
        public void forEachRemaining(Consumer<? super R> action) {
            try {
                spliterator.forEachRemaining(action);
            } finally {
                close();
            }
        }

        @Override
        public Spliterator<R> trySplit() {
            return spliterator.trySplit();
        }

        @Override
        public long estimateSize() {
            return spliterator.estimateSize();
        }

        @Override
        public long getExactSizeIfKnown() {
            return spliterator.getExactSizeIfKnown();
        }

        @Override
        public int characteristics() {
            return spliterator.characteristics();
        }

        @Override
        public boolean hasCharacteristics(int characteristics) {
            return spliterator.hasCharacteristics(characteristics);
        }

        @Override
        public Comparator<? super R> getComparator() {
            return spliterator.getComparator();
        }

    }
}
