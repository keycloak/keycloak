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
package org.keycloak.client.cli.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpUtilEncodingTest {

    @Test
    public void encodesPathSegmentsRatherThanFormValues() {
        assertThat(HttpUtil.urlencode("client id"), is("client+id"));
        assertThat(HttpUtil.urlencodePathSegment("client id"), is("client%20id"));
        assertThat(HttpUtil.urlencodePathSegment("client+id/1"), is("client%2Bid%2F1"));
    }
}
