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

package org.keycloak.client.registration;

import org.apache.http.StatusLine;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HttpErrorException extends IOException {

    private final StatusLine statusLine;
    private final String errorResponse;

    public HttpErrorException(StatusLine statusLine, String errorResponse) {
        this.statusLine = statusLine;
        this.errorResponse = errorResponse;
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }

    public String getErrorResponse() {
        return errorResponse;
    }

    public OAuth2ErrorRepresentation toErrorRepresentation() {
        if (errorResponse == null) {
            return null;
        }

        try {
            return JsonSerialization.readValue(errorResponse, OAuth2ErrorRepresentation.class);
        } catch (IOException ioe) {
            throw new RuntimeException("Not OAuth2 error");
        }
    }
}
