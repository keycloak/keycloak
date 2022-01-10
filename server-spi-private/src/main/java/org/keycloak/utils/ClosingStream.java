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
        delegate.forEach(action);
        close();
    }

    @Override
    public void forEachOrdered(Consumer<? super R> action) {
        delegate.forEachOrdered(action);
        close();
    }

    @Override
    public Object[] toArray() {
        Object[] result = delegate.toArray();
        close();
        return result;
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        A[] result = delegate.toArray(generator);
        close();
        return result;
    }

    @Override
    public R reduce(R identity, BinaryOperator<R> accumulator) {
        R result = delegate.reduce(identity, accumulator);
        close();
        return result;
    }

    @Override
    public Optional<R> reduce(BinaryOperator<R> accumulator) {
        Optional<R> result = delegate.reduce(accumulator);
        close();
        return result;
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super R, U> accumulator, BinaryOperator<U> combiner) {
        U result = delegate.reduce(identity, accumulator, combiner);
        close();
        return result;
    }

    @Override
    public <R1> R1 collect(Supplier<R1> supplier, BiConsumer<R1, ? super R> accumulator, BiConsumer<R1, R1> combiner) {
        R1 result = delegate.collect(supplier, accumulator, combiner);
        close();
        return result;
    }

    @Override
    public <R1, A> R1 collect(Collector<? super R, A, R1> collector) {
        R1 result = delegate.collect(collector);
        close();
        return result;
    }

    @Override
    public Optional<R> min(Comparator<? super R> comparator) {
        Optional<R> result = delegate.min(comparator);
        close();
        return result;
    }

    @Override
    public Optional<R> max(Comparator<? super R> comparator) {
        Optional<R> result = delegate.max(comparator);
        close();
        return result;
    }

    @Override
    public long count() {
        long result = delegate.count();
        close();
        return result;
    }

    @Override
    public boolean anyMatch(Predicate<? super R> predicate) {
        boolean result = delegate.anyMatch(predicate);
        close();
        return result;
    }

    @Override
    public boolean allMatch(Predicate<? super R> predicate) {
        boolean result = delegate.allMatch(predicate);
        close();
        return result;
    }

    @Override
    public boolean noneMatch(Predicate<? super R> predicate) {
        boolean result = delegate.noneMatch(predicate);
        close();
        return result;
    }

    @Override
    public Optional<R> findFirst() {
        Optional<R> result = delegate.findFirst();
        close();
        return result;
    }

    @Override
    public Optional<R> findAny() {
        Optional<R> result = delegate.findAny();
        close();
        return result;
    }

    @Override
    public Iterator<R> iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator<R> spliterator() {
        return delegate.spliterator();
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

}
