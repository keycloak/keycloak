/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services;

import org.keycloak.http.HttpResponse;

public class HttpResponseImpl implements HttpResponse {

    private org.jboss.resteasy.spi.HttpResponse delegate;

    public HttpResponseImpl(org.jboss.resteasy.spi.HttpResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setStatus(int statusCode) {
        delegate.setStatus(statusCode);
    }

    @Override
    public void addHeader(String name, String value) {
        delegate.getOutputHeaders().add(name, value);
    }

    @Override
    public void setHeader(String name, String value) {
        delegate.getOutputHeaders().putSingle(name, value);
    }
}
