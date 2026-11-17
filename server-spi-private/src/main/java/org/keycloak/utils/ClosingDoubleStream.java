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
        try {
            delegate.forEach(action);
        } finally {
            close();
        }
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        try {
            delegate.forEachOrdered(action);
        } finally {
            close();
        }
    }

    @Override
    public double[] toArray() {
        try {
            return delegate.toArray();
        } finally {
            close();
        }
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        try {
            return delegate.reduce(identity, op);
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        try {
            return delegate.reduce(op);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        } finally {
            close();
        }
    }

    @Override
    public double sum() {
        try {
            return delegate.sum();
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble min() {
        try {
            return delegate.min();
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble max() {
        try {
            return delegate.max();
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
    public OptionalDouble average() {
        try {
            return delegate.average();
        } finally {
            close();
        }
    }

    @Override
    public DoubleSummaryStatistics summaryStatistics() {
        try {
            return delegate.summaryStatistics();
        } finally {
            close();
        }
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        try {
            return delegate.anyMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        try {
            return delegate.allMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        try {
            return delegate.noneMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble findFirst() {
        try {
            return delegate.findFirst();
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble findAny() {
        try {
            return delegate.findAny();
        } finally {
            close();
        }
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
        return new ClosingIterator(delegate.iterator());
    }

    @Override
    public Spliterator.OfDouble spliterator() {
        return new ClosingSpliterator(delegate.spliterator());
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    private class ClosingIterator implements PrimitiveIterator.OfDouble {

        private final PrimitiveIterator.OfDouble iterator;

        public ClosingIterator(PrimitiveIterator.OfDouble iterator) {
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
        public Double next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            try {
                iterator.forEachRemaining(action);
            } finally {
                close();
            }
        }

        @Override
        public double nextDouble() {
            return iterator.nextDouble();
        }
    }

    private class ClosingSpliterator implements Spliterator.OfDouble {

        private final Spliterator.OfDouble spliterator;

        public ClosingSpliterator(Spliterator.OfDouble spliterator) {
            this.spliterator = spliterator;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
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
        public void forEachRemaining(DoubleConsumer action) {
            try {
                spliterator.forEachRemaining(action);
            } finally {
                close();
            }
        }

        @Override
        public Spliterator.OfDouble trySplit() {
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
        public Comparator<? super Double> getComparator() {
            return spliterator.getComparator();
        }

    }
}
