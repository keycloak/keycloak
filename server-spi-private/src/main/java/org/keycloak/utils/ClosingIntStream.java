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
        try {
            delegate.forEach(action);
        } finally {
            close();
        }
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        try {
            delegate.forEachOrdered(action);
        } finally {
            close();
        }
    }

    @Override
    public int[] toArray() {
        try {
            return delegate.toArray();
        } finally {
            close();
        }
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        try {
            return delegate.reduce(identity, op);
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        try {
            return delegate.reduce(op);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        } finally {
            close();
        }
    }

    @Override
    public int sum() {
        try {
            return delegate.sum();
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt min() {
        try {
            return delegate.min();
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt max() {
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
    public IntSummaryStatistics summaryStatistics() {
        try {
            return delegate.summaryStatistics();
        } finally {
            close();
        }
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        try {
            return delegate.anyMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        try {
            return delegate.allMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        try {
            return delegate.noneMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt findFirst() {
        try {
            return delegate.findFirst();
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt findAny() {
        try {
            return delegate.findAny();
        } finally {
            close();
        }
    }

    @Override
    public LongStream asLongStream() {
        try {
            return delegate.asLongStream();
        } finally {
            close();
        }
    }

    @Override
    public DoubleStream asDoubleStream() {
        try {
            return delegate.asDoubleStream();
        } finally {
            close();
        }
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
        return new ClosingIterator(delegate.iterator());
    }

    @Override
    public Spliterator.OfInt spliterator() {
        return new ClosingSpliterator(delegate.spliterator());
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    private class ClosingIterator implements PrimitiveIterator.OfInt {

        private final PrimitiveIterator.OfInt iterator;

        public ClosingIterator(PrimitiveIterator.OfInt iterator) {
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
        public Integer next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            try {
                iterator.forEachRemaining(action);
            } finally {
                close();
            }
        }

        @Override
        public int nextInt() {
            return iterator.nextInt();
        }
    }

    private class ClosingSpliterator implements Spliterator.OfInt {

        private final Spliterator.OfInt spliterator;

        public ClosingSpliterator(Spliterator.OfInt spliterator) {
            this.spliterator = spliterator;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
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
        public void forEachRemaining(IntConsumer action) {
            try {
                spliterator.forEachRemaining(action);
            } finally {
                close();
            }
        }

        @Override
        public Spliterator.OfInt trySplit() {
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
        public Comparator<? super Integer> getComparator() {
            return spliterator.getComparator();
        }

    }
}
