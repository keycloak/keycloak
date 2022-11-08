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

package org.keycloak.services.util;

import javax.ws.rs.core.NewCookie;
import java.util.Date;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class CookieBuilder {
    private final String name;
    private final String value;

    private String path;
    private String domain;
    private String comment;
    private int version;
    private int maxAge;
    private boolean secure;
    private boolean httpOnly;
    private Date expiry;

    public CookieBuilder(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public CookieBuilder path(String path) {
        this.path = path;
        return this;
    }

    public CookieBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    public CookieBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public CookieBuilder version(int version) {
        this.version = version;
        return this;
    }

    public CookieBuilder maxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public CookieBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public CookieBuilder httpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public CookieBuilder expiry(Date expireDate) {
        this.expiry = expireDate;
        return this;
    }

    public NewCookie build() {
        return new NewCookie(name, value, path, domain, version, comment, maxAge, expiry, secure, httpOnly);
    }
}
