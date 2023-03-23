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
import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * This stream will automatically close itself after terminal operation.
 */
class ClosingLongStream implements LongStream {

    private final LongStream delegate;

    public ClosingLongStream(LongStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public LongStream filter(LongPredicate predicate) {
        return new ClosingLongStream(delegate.filter(predicate));
    }

    @Override
    public LongStream map(LongUnaryOperator mapper) {
        return new ClosingLongStream(delegate.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        return new ClosingStream<>(delegate.mapToObj(mapper));
    }

    @Override
    public IntStream mapToInt(LongToIntFunction mapper) {
        return new ClosingIntStream(delegate.mapToInt(mapper));
    }

    @Override
    public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        return new ClosingDoubleStream(delegate.mapToDouble(mapper));
    }

    @Override
    public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        return new ClosingLongStream(delegate.flatMap(mapper));
    }

    @Override
    public LongStream distinct() {
        return new ClosingLongStream(delegate.distinct());
    }

    @Override
    public LongStream sorted() {
        return new ClosingLongStream(delegate.sorted());
    }

    @Override
    public LongStream peek(LongConsumer action) {
        return new ClosingLongStream(delegate.peek(action));
    }

    @Override
    public LongStream limit(long maxSize) {
        return new ClosingLongStream(delegate.limit(maxSize));
    }

    @Override
    public LongStream skip(long n) {
        return new ClosingLongStream(delegate.skip(n));
    }

    @Override
    public void forEach(LongConsumer action) {
        delegate.forEach(action);
        close();
    }

    @Override
    public void forEachOrdered(LongConsumer action) {
        delegate.forEachOrdered(action);
        close();
    }

    @Override
    public long[] toArray() {
        long[] result = delegate.toArray();
        close();
        return result;
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        long result = delegate.reduce(identity, op);
        close();
        return result;
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        OptionalLong result = delegate.reduce(op);
        close();
        return result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        R result = delegate.collect(supplier, accumulator, combiner);
        close();
        return result;
    }

    @Override
    public long sum() {
        long result = delegate.sum();
        close();
        return result;
    }

    @Override
    public OptionalLong min() {
        OptionalLong result = delegate.min();
        close();
        return result;
    }

    @Override
    public OptionalLong max() {
        OptionalLong result = delegate.max();
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
    public LongSummaryStatistics summaryStatistics() {
        LongSummaryStatistics result = delegate.summaryStatistics();
        close();
        return result;
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        boolean result = delegate.anyMatch(predicate);
        close();
        return result;
    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        boolean result = delegate.allMatch(predicate);
        close();
        return result;
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        boolean result = delegate.noneMatch(predicate);
        close();
        return result;
    }

    @Override
    public OptionalLong findFirst() {
        OptionalLong result = delegate.findFirst();
        close();
        return result;
    }

    @Override
    public OptionalLong findAny() {
        OptionalLong result = delegate.findAny();
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
    public Stream<Long> boxed() {
        return new ClosingStream<>(delegate.boxed());
    }

    @Override
    public LongStream sequential() {
        return new ClosingLongStream(delegate.sequential());
    }

    @Override
    public LongStream parallel() {
        return new ClosingLongStream(delegate.parallel());
    }

    @Override
    public LongStream unordered() {
        return new ClosingLongStream(delegate.unordered());
    }

    @Override
    public LongStream onClose(Runnable closeHandler) {
        return new ClosingLongStream(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public PrimitiveIterator.OfLong iterator() {
        return new ClosingIterator(delegate.iterator());
    }

    @Override
    public Spliterator.OfLong spliterator() {
        return new ClosingSpliterator(delegate.spliterator());
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    private class ClosingIterator implements PrimitiveIterator.OfLong {

        private final PrimitiveIterator.OfLong iterator;

        public ClosingIterator(PrimitiveIterator.OfLong iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            final boolean res = iterator.hasNext();
            if (! res) {
                close();
            }
            return res;
        }

        @Override
        public Long next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            iterator.forEachRemaining(action);
            close();
        }

        @Override
        public long nextLong() {
            return iterator.nextLong();
        }
    }

    private class ClosingSpliterator implements Spliterator.OfLong {

        private final Spliterator.OfLong spliterator;

        public ClosingSpliterator(Spliterator.OfLong spliterator) {
            this.spliterator = spliterator;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            final boolean res = spliterator.tryAdvance(action);
            if (! res) {
                close();
            }
            return res;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            spliterator.forEachRemaining(action);
            close();
        }

        @Override
        public Spliterator.OfLong trySplit() {
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
        public Comparator<? super Long> getComparator() {
            return spliterator.getComparator();
        }

    }
}
