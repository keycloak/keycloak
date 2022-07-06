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

package org.keycloak.operator.testsuite.utils;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.assertj.core.api.ThrowingConsumer;
import org.assertj.core.util.Strings;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.opentest4j.AssertionFailedError;

import java.util.function.Predicate;

/**
 * @author Alexander Schwartz
 */
class AssertingMatcher<T> extends BaseMatcher<T> {
    private final ThrowingConsumer<T> consumer;

    private Throwable throwable;

    public AssertingMatcher(ThrowingConsumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText("with value ");

        if (item instanceof KubernetesResource) {
            description.appendText("\n").appendText(Serialization.asYaml(item)).appendText("---\n");
        } else {
            description.appendValue(item).appendText(" ");
        }

        description.appendText("it failed");

        if (throwable != null && !Strings.isNullOrEmpty(throwable.getMessage())) {
            description.appendText(" with ").appendText(throwable.getMessage());
        }
    }

    @Override
    public boolean matches(Object o) {
        try {
            //noinspection unchecked
            consumer.acceptThrows((T) o);
        } catch (Throwable ex) {
            throwable = ex;
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("to fulfill the condition");
    }
}
