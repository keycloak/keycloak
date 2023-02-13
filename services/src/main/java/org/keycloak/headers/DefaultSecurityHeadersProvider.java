/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.headers;

import org.jboss.logging.Logger;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ContentSecurityPolicyBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.Map;

import static org.keycloak.models.BrowserSecurityHeaders.CONTENT_SECURITY_POLICY;

public class DefaultSecurityHeadersProvider implements SecurityHeadersProvider {

    private static final Logger LOGGER = Logger.getLogger(DefaultSecurityHeadersProvider.class);

    private final Map<String, String> headerValues;
    private final KeycloakSession session;

    private DefaultSecurityHeadersOptions options;

    public DefaultSecurityHeadersProvider(KeycloakSession session) {
        this.session = session;

        RealmModel realm = session.getContext().getRealm();
        if (realm != null) {
            headerValues = realm.getBrowserSecurityHeaders();
        } else {
            headerValues = Collections.emptyMap();
        }
    }

    @Override
    public SecurityHeadersOptions options() {
        if (options == null) {
            options = new DefaultSecurityHeadersOptions();
        }
        return options;
    }

    @Override
    public void addHeaders(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (options != null && options.isSkipHeaders()) {
            return;
        }

        MediaType requestType = requestContext.getMediaType();
        MediaType responseType = responseContext.getMediaType();
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        if (responseType == null && !isEmptyMediaTypeAllowed(requestContext, responseContext)) {
            LOGGER.errorv("MediaType not set on path {0}, with response status {1}", session.getContext().getUri().getRequestUri().getPath(), responseContext.getStatus());
            throw new InternalServerErrorException();
        }

        if (isRest(requestType, responseType)) {
            addRestHeaders(headers);
        } else if (isHtml(requestType, responseType)) {
            addHtmlHeaders(headers);
        } else {
            addGenericHeaders(headers);
        }
    }

    private void addGenericHeaders(MultivaluedMap<String, Object> headers) {
        addHeader(BrowserSecurityHeaders.STRICT_TRANSPORT_SECURITY, headers);
        addHeader(BrowserSecurityHeaders.X_CONTENT_TYPE_OPTIONS, headers);
        addHeader(BrowserSecurityHeaders.X_XSS_PROTECTION, headers);
        addHeader(BrowserSecurityHeaders.REFERRER_POLICY, headers);
    }

    private void addRestHeaders(MultivaluedMap<String, Object> headers) {
        addHeader(BrowserSecurityHeaders.STRICT_TRANSPORT_SECURITY, headers);
        addHeader(BrowserSecurityHeaders.X_FRAME_OPTIONS, headers);
        addHeader(BrowserSecurityHeaders.X_CONTENT_TYPE_OPTIONS, headers);
        addHeader(BrowserSecurityHeaders.X_XSS_PROTECTION, headers);
        addHeader(BrowserSecurityHeaders.REFERRER_POLICY, headers);
    }

    private void addHtmlHeaders(MultivaluedMap<String, Object> headers) {
        for (BrowserSecurityHeaders header : BrowserSecurityHeaders.values()) {
            addHeader(header, headers);
        }

        // TODO This will be refactored as part of introducing a more strict CSP header
        if (options != null) {
            ContentSecurityPolicyBuilder csp = ContentSecurityPolicyBuilder.create();

            if (options.isAllowAnyFrameAncestor()) {
                headers.remove(BrowserSecurityHeaders.X_FRAME_OPTIONS.getHeaderName());

                csp.frameAncestors(null);
            }

            String allowedFrameSrc = options.getAllowedFrameSrc();
            if (allowedFrameSrc != null) {
                csp.frameSrc(allowedFrameSrc);
            }

            if (CONTENT_SECURITY_POLICY.getDefaultValue().equals(headers.getFirst(CONTENT_SECURITY_POLICY.getHeaderName()))) {
                headers.putSingle(CONTENT_SECURITY_POLICY.getHeaderName(), csp.build());
            }
        }
    }

    private void addHeader(BrowserSecurityHeaders header, MultivaluedMap<String, Object> headers) {
        String value = headerValues.getOrDefault(header.getKey(), header.getDefaultValue());
        if (value != null && !value.isEmpty()) {
            headers.putSingle(header.getHeaderName(), value);
        }
    }

    /**
     * Prevent responses without content-type unless explicitly safe to do so
     */
    private boolean isEmptyMediaTypeAllowed(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (!responseContext.hasEntity()) {
            if (options != null && options.isAllowEmptyContentType()) {
                return true;
            }
            int status = responseContext.getStatus();
            if (status == 201 || status == 204 ||
                status == 301 || status == 302 || status == 303 || status == 307 || status == 308 ||
                status == 400 || status == 401 || status == 403 || status == 404) {
                return true;
            }
            if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
                return true;
            }
        }
        return false;
    }

    private boolean isRest(MediaType requestType, MediaType responseType) {
        MediaType mediaType = responseType != null ? responseType : requestType;
        return matches(mediaType, MediaType.APPLICATION_JSON_TYPE) || matches(mediaType, MediaType.APPLICATION_XML_TYPE);
    }

    private boolean isHtml(MediaType requestType, MediaType responseType) {
        if (matches(responseType, MediaType.TEXT_HTML_TYPE)) {
            return true;
        } else if (matches(requestType, MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
            return true;
        }
        return false;
    }

    private boolean matches(MediaType a, MediaType b) {
        if (a == null) {
            return b == null;
        }
        return a.getType().equalsIgnoreCase(b.getType()) && a.getSubtype().equalsIgnoreCase(b.getSubtype());
    }

}
