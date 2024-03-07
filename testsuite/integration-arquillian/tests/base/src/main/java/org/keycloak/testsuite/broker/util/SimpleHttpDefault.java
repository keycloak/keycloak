/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker.util;

import org.apache.http.client.HttpClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.connections.httpclient.HttpClientProvider;

/**
 * This class provides additional builders used in tests to create instances of SimpleHttpTest with a default length response size set.
 *
 * @author Alexander Schwartz
 */
public abstract class SimpleHttpDefault extends SimpleHttp {

    protected SimpleHttpDefault(String url, String method, HttpClient client, long maxConsumedResponseSize) {
        // dummy constructor, only needed to make the compiler happy
        super(url, method, client, maxConsumedResponseSize);
    }

    public static SimpleHttp doDelete(String url, HttpClient client) {
        return SimpleHttp.doDelete(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

    public static SimpleHttp doPost(String url, HttpClient client) {
        return SimpleHttp.doPost(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

    public static SimpleHttp doPut(String url, HttpClient client) {
        return SimpleHttp.doPut(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

    public static SimpleHttp doGet(String url, HttpClient client) {
        return SimpleHttp.doGet(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

}
