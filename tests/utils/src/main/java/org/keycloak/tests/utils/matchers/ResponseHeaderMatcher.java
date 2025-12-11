/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.utils.matchers;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Matcher for matching status code of {@link Response} instance.
 * @author hmlnarik
 */
public class ResponseHeaderMatcher<T> extends BaseMatcher<Response> {

    private final Matcher<Map<String, T>> matcher;

    public ResponseHeaderMatcher(Matcher<Map<String, T>> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object item) {
        return (item instanceof Response) && this.matcher.matches(((Response) item).getHeaders());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("response headers match ").appendDescriptionOf(this.matcher);
    }

}
