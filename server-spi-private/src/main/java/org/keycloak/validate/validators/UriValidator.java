/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validate.validators;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * URI validation - accepts {@link URI}, {@link URL} and single String. Null input is valid, use other validators (like
 * {@link NotBlankValidator} or {@link NotEmptyValidator} to force field as required.
 */
public class UriValidator implements SimpleValidator, ConfiguredProvider {

    public static final UriValidator INSTANCE = new UriValidator();

    public static final String KEY_ALLOWED_SCHEMES = "allowedSchemes";
    public static final String KEY_ALLOW_FRAGMENT = "allowFragment";
    public static final String KEY_REQUIRE_VALID_URL = "requireValidUrl";

    public static final Set<String> DEFAULT_ALLOWED_SCHEMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "http",
            "https"
    )));
    public static final String MESSAGE_INVALID_URI = "error-invalid-uri";
    public static final String MESSAGE_INVALID_SCHEME = "error-invalid-uri-scheme";
    public static final String MESSAGE_INVALID_FRAGMENT = "error-invalid-uri-fragment";

    public static boolean DEFAULT_ALLOW_FRAGMENT = true;

    public static boolean DEFAULT_REQUIRE_VALID_URL = true;

    public static final String ID = "uri";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
    	
    	if(input == null || (input instanceof String && ((String) input).isEmpty())) {
    		return context;
    	}

        try {
            URI uri = toUri(input);

            if (uri == null) {
                context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_URI, input));
            } else {
                Set<String> allowedSchemes = config.getStringSetOrDefault(KEY_ALLOWED_SCHEMES, DEFAULT_ALLOWED_SCHEMES);
                boolean allowFragment = config.getBooleanOrDefault(KEY_ALLOW_FRAGMENT, DEFAULT_ALLOW_FRAGMENT);
                boolean requireValidUrl = config.getBooleanOrDefault(KEY_REQUIRE_VALID_URL, DEFAULT_REQUIRE_VALID_URL);

                validateUri(uri, inputHint, context, allowedSchemes, allowFragment, requireValidUrl);
            }
        } catch (MalformedURLException | IllegalArgumentException | URISyntaxException e) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_URI, input, e.getMessage()));
        }

        return context;
    }

    private URI toUri(Object input) throws URISyntaxException {

        if (input instanceof String) {
            String uriString = (String) input;
            return new URI(uriString);
        } else if (input instanceof URI) {
            return (URI) input;
        } else if (input instanceof URL) {
            return ((URL) input).toURI();
        }

        return null;
    }

    public boolean validateUri(URI uri, Set<String> allowedSchemes, boolean allowFragment, boolean requireValidUrl) {
        try {
            return validateUri(uri, "url", new ValidationContext(), allowedSchemes, allowFragment, requireValidUrl);
        } catch (MalformedURLException mue) {
            return false;
        }
    }

    public boolean validateUri(URI uri, String inputHint, ValidationContext context,
                               Set<String> allowedSchemes, boolean allowFragment, boolean requireValidUrl)
            throws MalformedURLException {

        boolean valid = true;
        if (uri.getScheme() != null && !allowedSchemes.contains(uri.getScheme())) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_SCHEME, uri, uri.getScheme()));
            valid = false;
        }

        if (!allowFragment && uri.getFragment() != null) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_FRAGMENT, uri, uri.getFragment()));
            valid = false;
        }

        // Don't check if URL is valid if there are other problems with it; otherwise it could lead to duplicate errors.
        // This cannot be moved higher because it acts on differently based on environment (e.g. sometimes it checks
        // scheme, sometimes it doesn't).
        if (requireValidUrl && valid) {
            URL ignored = uri.toURL(); // throws an exception
        }

        return valid;
    }

    @Override
    public String getHelpText() {
        return "Uri Validator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
