/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.common.util.NullSafeChecks.isNotNull;
import static org.keycloak.common.util.NullSafeChecks.isNull;
import static org.keycloak.common.util.NullSafeChecks.isTrue;
import static org.hamcrest.CoreMatchers.is;

public class NullSafeChecksTest {

    @Test
    public void nullObjects() {
        final String string = null;
        checkNullObjects(() -> string);

        final Optional<String> optionalNullSpec = Optional.ofNullable(null);
        checkNullObjects(() -> optionalNullSpec.get());
    }

    @Test
    public void notNullObjects() {
        final String string = "asdf";

        assertThat(isNull(() -> string.getBytes().toString()), is(false));
        assertThat(isNotNull(() -> string.getBytes().toString()), is(true));
        assertThat(isTrue(() -> Arrays.equals(string.getBytes(), "asdf".getBytes())), is(true));
        assertThat(isTrue(() -> Arrays.equals(string.getBytes(), "none".getBytes())), is(false));

        final Optional<String> optionalString = Optional.of("asdf");
        assertThat(isNull(() -> optionalString.get().getBytes().toString()), is(false));
        assertThat(isNotNull(() -> optionalString.get().getBytes().toString()), is(true));
        assertThat(isTrue(() -> Arrays.equals(optionalString.get().getBytes(), "asdf".getBytes())), is(true));
        assertThat(isTrue(() -> Arrays.equals(optionalString.get().getBytes(), "none".getBytes())), is(false));
    }

    @Test
    public void methodReference() {
        final String string = "asdf";

        assertThat(isNull(string.getBytes()::toString), is(false));
        assertThat(isNotNull(string.getBytes()::toString), is(true));

        final String nullString = null;
        try {
            assertThat(isNull(nullString.getBytes()::toString), is(false));
            Assert.fail("The NullSafeChecks does not support method references");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    private void checkNullObjects(Supplier<String> string) {
        assertThat(isNull(() -> string.get().getBytes()), is(true));
        assertThat(isNull(() -> string.get().getClass().getSimpleName()), is(true));

        assertThat(isNotNull(() -> string.get().getBytes()), is(false));
        assertThat(isNotNull(() -> string.get().getClass().getSimpleName()), is(false));

        assertThat(isTrue(() -> string.get().getBytes() != null), is(false));
        assertThat(isTrue(() -> string.get().getClass().isAnnotation() == false), is(false));
    }
}
