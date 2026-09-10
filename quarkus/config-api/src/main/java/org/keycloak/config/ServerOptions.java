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

package org.keycloak.config;

import java.util.Optional;

public final class ServerOptions {

    private ServerOptions() {}

    public static final Option<Boolean> SERVER_ASYNC_BOOTSTRAP = new OptionBuilder<>("server-async-bootstrap", Boolean.class)
            .category(OptionCategory.SERVER)
            .defaultValue(Optional.empty())
            .description("If true, endpoints are opened while the bootstrap runs in the background. If false, endpoints are opened after bootstrap completes, ensuring the server is ready to handle requests. Async bootstrap is enabled by default when the health endpoints are also enabled, unless this option is explicitly set to false.")
            .build();
}
