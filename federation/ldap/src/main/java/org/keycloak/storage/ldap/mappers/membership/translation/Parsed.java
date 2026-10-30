/*
 * Copyright 2026 Andrzej Świętek
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
package org.keycloak.storage.ldap.mappers.membership.translation;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A configuration value that was either read successfully or rejected with problems an administrator can act on.
 */
sealed interface Parsed<T> {

    Optional<T> value();

    List<String> problems();

    record Valid<T>(T result) implements Parsed<T> {

        @Override
        public Optional<T> value() {
            return Optional.of(result);
        }

        @Override
        public List<String> problems() {
            return List.of();
        }
    }

    record Invalid<T>(List<String> problems) implements Parsed<T> {

        public Invalid {
            problems = List.copyOf(problems);
        }

        @Override
        public Optional<T> value() {
            return Optional.empty();
        }
    }

    static <T> Parsed<T> valid(T value) {
        return new Valid<>(value);
    }

    static <T> Parsed<T> invalid(String problem) {
        return new Invalid<>(List.of(problem));
    }

    static <T> Parsed<T> invalid(List<String> problems) {
        return new Invalid<>(problems);
    }

    /**
     * Valid when both are, otherwise invalid with the problems of both.
     */
    static <A, B, R> Parsed<R> combine(Parsed<A> first, Parsed<B> second, BiFunction<A, B, R> combiner) {
        return first instanceof Valid<A> a && second instanceof Valid<B> b
                ? valid(combiner.apply(a.result(), b.result()))
                : invalid(Stream.concat(first.problems().stream(), second.problems().stream()).toList());
    }

    /**
     * Valid when every part is, otherwise invalid with the problems of all parts in order.
     */
    static <T> Parsed<List<T>> all(List<Parsed<T>> parts) {
        return ofProblems(valuesOf(parts), problemsOf(parts));
    }

    /**
     * Valid with the value when there are no problems, otherwise invalid with the problems.
     */
    static <T> Parsed<T> ofProblems(T value, List<String> problems) {
        return problems.isEmpty() ? valid(value) : invalid(problems);
    }

    /**
     * Valid when the value is present, otherwise invalid with the problem.
     */
    static <T> Parsed<T> required(Optional<T> value, String problem) {
        return value.map(Parsed::valid).orElseGet(() -> invalid(problem));
    }

    /**
     * Invalid with the problem the check finds in the value, if any.
     */
    default Parsed<T> check(Function<T, Optional<String>> problemOf) {
        return flatMap(value -> ofProblems(value, problemOf.apply(value).stream().toList()));
    }

    default <R> Parsed<R> map(Function<T, R> mapper) {
        return flatMap(value -> valid(mapper.apply(value)));
    }

    default <R> Parsed<R> flatMap(Function<T, Parsed<R>> mapper) {
        return value().map(mapper).orElseGet(() -> invalid(problems()));
    }

    private static <T> List<String> problemsOf(List<Parsed<T>> parts) {
        return parts.stream().map(Parsed::problems).flatMap(List::stream).toList();
    }

    private static <T> List<T> valuesOf(List<Parsed<T>> parts) {
        return parts.stream().map(Parsed::value).flatMap(Optional::stream).toList();
    }
}
