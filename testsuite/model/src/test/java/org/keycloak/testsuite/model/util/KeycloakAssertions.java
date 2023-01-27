/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.util;

import org.hamcrest.Matcher;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class KeycloakAssertions {

    /**
     * Runs {@code task} and checks whether the execution resulted in
     * an exception that matches {@code matcher}. The method fails also
     * when no exception is thrown.
     *
     * @param task task
     * @param matcher matcher that the exception should match
     */
    public static void assertException(Runnable task, Matcher<? super Throwable> matcher) {
        Throwable ex = catchException(task);
        assertThat(ex, allOf(notNullValue(), matcher));
    }

    /**
     * Runs the {@code task} and returns any throwable that is thrown.
     * If not exception is thrown, the method returns {@code null}
     *
     * @param task task
     */
    public static Throwable catchException(Runnable task) {
        try {
            task.run();
            return null;
        } catch (Throwable ex) {
            return ex;
        }
    }
}
