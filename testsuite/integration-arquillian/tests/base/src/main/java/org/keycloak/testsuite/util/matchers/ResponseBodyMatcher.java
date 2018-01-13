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
package org.keycloak.testsuite.util.matchers;

import javax.ws.rs.core.Response;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Matcher for matching status code of {@link Response} instance.
 * @author hmlnarik
 */
public class ResponseBodyMatcher extends BaseMatcher<Response> {

    private final Matcher<String> matcher;

    public ResponseBodyMatcher(Matcher<String> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof Response) {
            final Response rItem = (Response) item;
            rItem.bufferEntity();
            return this.matcher.matches(rItem.readEntity(String.class));
        } else {
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("response body matches ").appendDescriptionOf(this.matcher);
    }

}
