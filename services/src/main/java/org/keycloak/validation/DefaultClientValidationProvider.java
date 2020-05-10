/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validation;

import org.keycloak.models.ClientModel;
import org.keycloak.services.util.ResolveRelative;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class DefaultClientValidationProvider implements ClientValidationProvider {

    private ClientValidationContext context;

    // TODO Before adding more validation consider using a library for validation
    @Override
    public void validate(ClientValidationContext context) {
        this.context = context;

        try {
            validate(context.getClient());
        } catch (ValidationException e) {
            context.invalid(e.getMessage());
        }
    }

    private void validate(ClientModel client) throws ValidationException {
        // Use a fake URL for validating relative URLs as we may not be validating clients in the context of a request (import at startup)
        String authServerUrl = "https://localhost/auth";

        String resolvedRootUrl = ResolveRelative.resolveRootUrl(authServerUrl, authServerUrl, client.getRootUrl());
        String resolvedBaseUrl = ResolveRelative.resolveRelativeUri(authServerUrl, authServerUrl, resolvedRootUrl, client.getBaseUrl());

        validateRootUrl(resolvedRootUrl);
        validateBaseUrl(resolvedBaseUrl);
    }

    private void validateRootUrl(String rootUrl) throws ValidationException {
        if (rootUrl != null && !rootUrl.isEmpty()) {
            basicHttpUrlCheck("rootUrl", rootUrl);
        }
    }

    private void validateBaseUrl(String baseUrl) throws ValidationException {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            basicHttpUrlCheck("baseUrl", baseUrl);
        }
    }

    private void basicHttpUrlCheck(String field, String url) throws ValidationException {
        boolean valid = true;
        try {
            URI uri = new URL(url).toURI();
            if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
                valid = false;
            }
        } catch (MalformedURLException | URISyntaxException e) {
            valid = false;
        }

        if (!valid) {
            throw new ValidationException("Invalid URL in " + field);
        }
    }

    class ValidationException extends Exception {

        public ValidationException(String message) {
            super(message, null, false, false);
        }
    }

}
