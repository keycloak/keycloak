/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.client.resource;

import static org.keycloak.authorization.client.util.Throwables.handleAndWrapException;

import java.util.concurrent.Callable;

import org.keycloak.authorization.client.representation.PermissionRequest;
import org.keycloak.authorization.client.representation.PermissionResponse;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionResource {

    private final Http http;
    private final Callable<String> pat;

    public PermissionResource(Http http, Callable<String> pat) {
        this.http = http;
        this.pat = pat;
    }

    public PermissionResponse forResource(PermissionRequest request) {
        try {
            return this.http.<PermissionResponse>post("/authz/protection/permission")
                    .authorizationBearer(this.pat.call())
                    .json(JsonSerialization.writeValueAsBytes(request))
                    .response().json(PermissionResponse.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Error obtaining permission ticket", cause);
        }
    }
}
