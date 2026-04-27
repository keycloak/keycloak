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

import java.io.IOException;

import jakarta.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Matcher for matching status code of {@link Response} instance.
 * @author hmlnarik
 */
public class HttpResponseBodyMatcher extends BaseMatcher<HttpResponse> {

    private final Matcher<String> matcher;

    private ThreadLocal<String> lastEntity = new ThreadLocal<>();

    public HttpResponseBodyMatcher(Matcher<String> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object item) {
        lastEntity.remove();
        try {
            lastEntity.set(EntityUtils.toString(((HttpResponse) item).getEntity()));
            return (item instanceof HttpResponse) && this.matcher.matches(lastEntity.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        Description d = description.appendText("was ").appendValue(item);
        if (lastEntity.get() != null) {
            d.appendText(" with entity ").appendText(lastEntity.get());
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("response body matches ").appendDescriptionOf(this.matcher);
    }

}
