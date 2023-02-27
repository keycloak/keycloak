/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.http;

import org.keycloak.common.util.ServerCookie;
import org.keycloak.common.util.ServerCookie.SameSiteAttributeValue;

/**
 * An extension of {@link javax.ws.rs.core.Cookie} in order to support additional
 * fields and behavior.
 */
public final class HttpCookie extends javax.ws.rs.core.Cookie {

    private final String comment;
    private final int maxAge;
    private final boolean secure;
    private final boolean httpOnly;
    private final SameSiteAttributeValue sameSite;

    public HttpCookie(int version, String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly, SameSiteAttributeValue sameSite) {
        super(name, value, path, domain, version);
        this.comment = comment;
        this.maxAge = maxAge;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.sameSite = sameSite;
    }

    public String toHeaderValue() {
        StringBuilder cookieBuf = new StringBuilder();
        ServerCookie.appendCookieValue(cookieBuf, getVersion(), getName(), getValue(), getPath(), getDomain(), comment, maxAge, secure, httpOnly, sameSite);
        return cookieBuf.toString();
    }
}
