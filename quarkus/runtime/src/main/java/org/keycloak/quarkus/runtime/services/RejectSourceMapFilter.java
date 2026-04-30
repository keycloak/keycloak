/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.services;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.jboss.logging.Logger;

/**
 * Blocks HTTP requests for JavaScript source map files (.js.map) in production mode.
 * Source maps are retained on disk for support diagnostics but must not be served
 * to clients in production, as they can expose internal implementation details of
 * customized themes.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/47545">GitHub #47545</a>
 */
public class RejectSourceMapFilter implements Handler<RoutingContext> {

    private static final Logger LOGGER = Logger.getLogger(RejectSourceMapFilter.class);

    @Override
    public void handle(RoutingContext routingContext) {
        String path = routingContext.request().path();

        if (path != null && path.endsWith(".js.map")) {
            LOGGER.debugf("Blocked source map request: %s", path);
            routingContext.fail(404);
            return;
        }

        routingContext.next();
    }
}
