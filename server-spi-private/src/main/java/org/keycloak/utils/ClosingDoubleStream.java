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

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * This stream will automatically close itself after terminal operation.
 */
class ClosingDoubleStream implements DoubleStream {

    private final DoubleStream delegate;

    public ClosingDoubleStream(DoubleStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public DoubleStream filter(DoublePredicate predicate) {
        return new ClosingDoubleStream(delegate.filter(predicate));
    }

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        return new ClosingDoubleStream(delegate.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return new ClosingStream<>(delegate.mapToObj(mapper));
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        return new ClosingIntStream(delegate.mapToInt(mapper));
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        return new ClosingLongStream(delegate.mapToLong(mapper));
    }

    @Override
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        return new ClosingDoubleStream(delegate.flatMap(mapper));
    }

    @Override
    public DoubleStream distinct() {
        return new ClosingDoubleStream(delegate.distinct());
    }

    @Override
    public DoubleStream sorted() {
        return new ClosingDoubleStream(delegate.sorted());
    }

    @Override
    public DoubleStream peek(DoubleConsumer action) {
        return new ClosingDoubleStream(delegate.peek(action));
    }

    @Override
    public DoubleStream limit(long maxSize) {
        return new ClosingDoubleStream(delegate.limit(maxSize));
    }

    @Override
    public DoubleStream skip(long n) {
        return new ClosingDoubleStream(delegate.skip(n));
    }

    @Override
    public void forEach(DoubleConsumer action) {
        delegate.forEach(action);
        close();
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        delegate.forEachOrdered(action);
        close();
    }

    @Override
    public double[] toArray() {
        double[] result = delegate.toArray();
        close();
        return result;
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        double result = delegate.reduce(identity, op);
        close();
        return result;
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        OptionalDouble result = delegate.reduce(op);
        close();
        return result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        R result = delegate.collect(supplier, accumulator, combiner);
        close();
        return result;
    }

    @Override
    public double sum() {
        double result = delegate.sum();
        close();
        return result;
    }

    @Override
    public OptionalDouble min() {
        OptionalDouble result = delegate.min();
        close();
        return result;
    }

    @Override
    public OptionalDouble max() {
        OptionalDouble result = delegate.max();
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
    public DoubleSummaryStatistics summaryStatistics() {
        DoubleSummaryStatistics result = delegate.summaryStatistics();
        close();
        return result;
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        boolean result = delegate.anyMatch(predicate);
        close();
        return result;
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        boolean result = delegate.allMatch(predicate);
        close();
        return result;
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        boolean result = delegate.noneMatch(predicate);
        close();
        return result;
    }

    @Override
    public OptionalDouble findFirst() {
        OptionalDouble result = delegate.findFirst();
        close();
        return result;
    }

    @Override
    public OptionalDouble findAny() {
        OptionalDouble result = delegate.findAny();
        close();
        return result;
    }

    @Override
    public Stream<Double> boxed() {
        return new ClosingStream<>(delegate.boxed());
    }

    @Override
    public DoubleStream sequential() {
        return new ClosingDoubleStream(delegate.sequential());
    }

    @Override
    public DoubleStream parallel() {
        return new ClosingDoubleStream(delegate.parallel());
    }

    @Override
    public DoubleStream unordered() {
        return new ClosingDoubleStream(delegate.unordered());
    }

    @Override
    public DoubleStream onClose(Runnable closeHandler) {
        return new ClosingDoubleStream(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator.OfDouble spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }
}
