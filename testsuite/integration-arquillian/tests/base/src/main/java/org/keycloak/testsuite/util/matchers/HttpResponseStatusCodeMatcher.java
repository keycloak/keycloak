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

import java.io.IOException;
import javax.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Matcher for matching status code of {@link Response} instance.
 * @author hmlnarik
 */
public class HttpResponseStatusCodeMatcher extends BaseMatcher<HttpResponse> {

    private final Matcher<? extends Number> matcher;

    public HttpResponseStatusCodeMatcher(Matcher<? extends Number> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object item) {
        return (item instanceof HttpResponse) && this.matcher.matches(((HttpResponse) item).getStatusLine().getStatusCode());
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        Description d = description.appendText("was ").appendValue(item)
          .appendText(" with entity ");
        try {
            d.appendText(EntityUtils.toString(((HttpResponse) item).getEntity()));
        } catch (IOException e) {
            d.appendText("<Cannot decode entity: " + e.getMessage() + ">");
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("response status code matches ").appendDescriptionOf(this.matcher);
    }

}
