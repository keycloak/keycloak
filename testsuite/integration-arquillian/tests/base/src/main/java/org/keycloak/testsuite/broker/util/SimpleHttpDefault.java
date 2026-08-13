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

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;

import org.apache.http.client.HttpClient;

/**
 * This class provides additional builders used in tests to create instances of SimpleHttpTest with a default length response size set.
 *
 * @author Alexander Schwartz
 */
public abstract class SimpleHttpDefault {

    public static SimpleHttpRequest doDelete(String url, HttpClient client) {
        return SimpleHttp.create(client).doDelete(url);
    }

    public static SimpleHttpRequest doPost(String url, HttpClient client) {
        return SimpleHttp.create(client).doPost(url);
    }

    public static SimpleHttpRequest doPut(String url, HttpClient client) {
        return SimpleHttp.create(client).doPut(url);
    }

    public static SimpleHttpRequest doGet(String url, HttpClient client) {
        return SimpleHttp.create(client).doGet(url);
    }

    public static SimpleHttpRequest doHead(String url, HttpClient client) {
        return SimpleHttp.create(client).doHead(url);
    }

}
