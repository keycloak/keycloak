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
package org.keycloak.models;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;

import org.jboss.resteasy.reactive.common.jaxrs.UriBuilderImpl;

import static org.keycloak.common.util.UriUtils.parseQueryParameters;

public class KeycloakUriInfo implements UriInfo {

    private final UriInfo delegate;

    private URI absolutePath;
    private URI requestURI;
    private URI baseURI;

    public KeycloakUriInfo(KeycloakSession session, UrlType type, UriInfo delegate) {
        this.delegate = delegate;

        HostnameProvider hostnameProvider = session.getProvider(HostnameProvider.class);
        baseURI = hostnameProvider.getBaseUri(delegate, type);
    }

    public UriInfo getDelegate() {
        return delegate;
    }

    @Override
    public URI getRequestUri() {
        if (requestURI == null) {
            requestURI = delegate.getRequestUri();
        }
        return requestURI;
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return UriBuilder.fromUri(getRequestUri());
    }

    @Override
    public URI getAbsolutePath() {
        if (absolutePath == null) {
            absolutePath = delegate.getAbsolutePath();
        }
        return absolutePath;
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return UriBuilder.fromUri(getAbsolutePath());
    }

    @Override
    public URI getBaseUri() {
        return baseURI;
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return UriBuilder.fromUri(getBaseUri());
    }

    @Override
    public URI resolve(URI uri) {
        return getBaseUri().resolve(uri);
    }

    @Override
    public URI relativize(URI uri) {
        URI from = this.getRequestUri();
        URI to = uri;
        if (uri.getScheme() == null && uri.getHost() == null) {
            to = this.getBaseUriBuilder().replaceQuery(null).path(uri.getPath()).replaceQuery(uri.getQuery()).fragment(uri.getFragment()).build(new Object[0]);
        }

        return UriBuilderImpl.relativize(from, to);
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public String getPath(boolean decode) {
        return delegate.getPath(decode);
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return delegate.getPathSegments();
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        return delegate.getPathSegments(decode);
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return delegate.getPathParameters();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        return delegate.getPathParameters(decode);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return delegate.getQueryParameters();
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        if (decode) {
            return delegate.getQueryParameters(decode);
        }

        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        String rawQuery = delegate.getRequestUri().getRawQuery();

        if (rawQuery == null) {
            return result;
        }

        for (Map.Entry<String, List<String>> entry : parseQueryParameters(rawQuery, false).entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @Override
    public List<String> getMatchedURIs() {
        return delegate.getMatchedURIs();
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        return delegate.getMatchedURIs(decode);
    }

    @Override
    public List<Object> getMatchedResources() {
        return delegate.getMatchedResources();
    }
}
