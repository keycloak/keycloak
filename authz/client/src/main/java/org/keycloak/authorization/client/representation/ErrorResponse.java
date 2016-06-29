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
package org.keycloak.authorization.client.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ErrorResponse {

    private String error;

    @JsonProperty("error_description")
    private String description;

    @JsonProperty("error_uri")
    private String uri;

    public ErrorResponse(final String error, final String description, final String uri) {
        this.error = error;
        this.description = description;
        this.uri = uri;
    }

    public ErrorResponse(final String error) {
        this(error, null, null);
    }

    public ErrorResponse() {
        this(null, null, null);
    }

    public String getError() {
        return this.error;
    }

    public String getDescription() {
        return this.description;
    }

    public String getUri() {
        return this.uri;
    }
}
