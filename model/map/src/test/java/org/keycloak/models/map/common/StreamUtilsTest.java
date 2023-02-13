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

import org.keycloak.models.map.common.StreamUtils.Pair;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hmlnarik
 */
public class StreamUtilsTest {

    @Test
    public void testLeftInnerJoinStream() {
        Stream<Integer> a = Stream.of(0,1,2,3,4);
        Stream[] b = new Stream[] {
            Stream.of(1,2),
            Stream.of(1,2),
            null,
            null,
            Stream.of(5, 6, 7),
        };

        try (Stream<Pair<Integer, Integer>> res = StreamUtils.leftInnerJoinStream(a, n -> b[n])) {
            final List<Pair<Integer, Integer>> l = res.collect(Collectors.toList());
            assertEquals(l, Arrays.asList(
              new Pair<>(0, 1),
              new Pair<>(0, 2),
              new Pair<>(1, 1),
              new Pair<>(1, 2),
              new Pair<>(4, 5),
              new Pair<>(4, 6),
              new Pair<>(4, 7)
            ));
        }
    }

    @Test
    public void testLeftInnerJoinIterable() {
        Stream<Integer> a = Stream.of(0,1,2,3,4);
        Iterable[] b = new Iterable[] {
            Arrays.asList(1,2),
            Arrays.asList(1,2),
            null,
            null,
            Arrays.asList(5, 6, 7),
        };

        try (Stream<Pair<Integer, Integer>> res = StreamUtils.leftInnerJoinIterable(a, n -> b[n])) {
            final List<Pair<Integer, Integer>> l = res.collect(Collectors.toList());
            assertEquals(l, Arrays.asList(
              new Pair<>(0, 1),
              new Pair<>(0, 2),
              new Pair<>(1, 1),
              new Pair<>(1, 2),
              new Pair<>(4, 5),
              new Pair<>(4, 6),
              new Pair<>(4, 7)
            ));
        }
    }

}
