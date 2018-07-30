/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.elytron;

import io.undertow.server.HttpServerExchange;

/**
 * <p>A wrapper for {@code {@link HttpServerExchange}} accessible only from classes in the same package.
 *
 * <p>This class is used to provide to the elytron mechanism access to the current exchange in order to allow making
 * changes to the exchange (e.g. response) during the evaluation of requests. By default, changes to the exchange are only
 * propagated after the execution of the mechanism. But in certain situations, such as when making a programmatic logout (HttpServletRequest.logout()) from
 * within application code, any change made to the exchange is not propagated.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class ProtectedHttpServerExchange {

    private final HttpServerExchange exchange;

    public ProtectedHttpServerExchange(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    HttpServerExchange getExchange() {
        return exchange;
    }
}
