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

public class DefaultSecurityHeadersOptions implements SecurityHeadersOptions {

    private boolean skipHeaders;
    private boolean allowAnyFrameAncestor;
    private boolean allowEmptyContentType;
    private String allowedFrameSrc;

    public SecurityHeadersOptions allowFrameSrc(String source) {
        allowedFrameSrc = source;
        return this;
    }

    @Override
    public SecurityHeadersOptions allowAnyFrameAncestor() {
        allowAnyFrameAncestor = true;
        return this;
    }

    public SecurityHeadersOptions skipHeaders() {
        skipHeaders = true;
        return this;
    }

    @Override
    public SecurityHeadersOptions allowEmptyContentType() {
        allowEmptyContentType = true;
        return this;
    }

    String getAllowedFrameSrc() {
        return allowedFrameSrc;
    }

    boolean isAllowAnyFrameAncestor() {
        return allowAnyFrameAncestor;
    }

    boolean isSkipHeaders() {
        return skipHeaders;
    }

    public boolean isAllowEmptyContentType() {
        return allowEmptyContentType;
    }

}
