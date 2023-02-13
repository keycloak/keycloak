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

import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * This stream will automatically close itself after terminal operation.
 */
class ClosingIntStream implements IntStream {

    private final IntStream delegate;

    public ClosingIntStream(IntStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public IntStream filter(IntPredicate predicate) {
        return new ClosingIntStream(delegate.filter(predicate));
    }

    @Override
    public IntStream map(IntUnaryOperator mapper) {
        return new ClosingIntStream(delegate.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        return new ClosingStream<>(delegate.mapToObj(mapper));
    }

    @Override
    public LongStream mapToLong(IntToLongFunction mapper) {
        return new ClosingLongStream(delegate.mapToLong(mapper));
    }

    @Override
    public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        return new ClosingDoubleStream(delegate.mapToDouble(mapper));
    }

    @Override
    public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        return new ClosingIntStream(delegate.flatMap(mapper));
    }

    @Override
    public IntStream distinct() {
        return new ClosingIntStream(delegate.distinct());
    }

    @Override
    public IntStream sorted() {
        return new ClosingIntStream(delegate.sorted());
    }

    @Override
    public IntStream peek(IntConsumer action) {
        return new ClosingIntStream(delegate.peek(action));
    }

    @Override
    public IntStream limit(long maxSize) {
        return new ClosingIntStream(delegate.limit(maxSize));
    }

    @Override
    public IntStream skip(long n) {
        return new ClosingIntStream(delegate.skip(n));
    }

    @Override
    public void forEach(IntConsumer action) {
        delegate.forEach(action);
        close();
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        delegate.forEachOrdered(action);
        close();
    }

    @Override
    public int[] toArray() {
        int[] result = delegate.toArray();
        close();
        return result;
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        int result = delegate.reduce(identity, op);
        close();
        return result;
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        OptionalInt result = delegate.reduce(op);
        close();
        return result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        R result = delegate.collect(supplier, accumulator, combiner);
        close();
        return result;
    }

    @Override
    public int sum() {
        int result = delegate.sum();
        close();
        return result;
    }

    @Override
    public OptionalInt min() {
        OptionalInt result = delegate.min();
        close();
        return result;
    }

    @Override
    public OptionalInt max() {
        OptionalInt result = delegate.max();
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
    public OptionalDouble average() {
        OptionalDouble result = delegate.average();
        close();
        return result;
    }

    @Override
    public IntSummaryStatistics summaryStatistics() {
        IntSummaryStatistics result = delegate.summaryStatistics();
        close();
        return result;
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        boolean result = delegate.anyMatch(predicate);
        close();
        return result;
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        boolean result = delegate.allMatch(predicate);
        close();
        return result;
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        boolean result = delegate.noneMatch(predicate);
        close();
        return result;
    }

    @Override
    public OptionalInt findFirst() {
        OptionalInt result = delegate.findFirst();
        close();
        return result;
    }

    @Override
    public OptionalInt findAny() {
        OptionalInt result = delegate.findAny();
        close();
        return result;
    }

    @Override
    public LongStream asLongStream() {
        LongStream result = delegate.asLongStream();
        close();
        return result;
    }

    @Override
    public DoubleStream asDoubleStream() {
        DoubleStream result = delegate.asDoubleStream();
        close();
        return result;
    }

    @Override
    public Stream<Integer> boxed() {
        return new ClosingStream<>(delegate.boxed());
    }

    @Override
    public IntStream sequential() {
        return new ClosingIntStream(delegate.sequential());
    }

    @Override
    public IntStream parallel() {
        return new ClosingIntStream(delegate.parallel());
    }

    @Override
    public IntStream unordered() {
        return new ClosingIntStream(delegate.unordered());
    }

    @Override
    public IntStream onClose(Runnable closeHandler) {
        return new ClosingIntStream(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator.OfInt spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }
}
