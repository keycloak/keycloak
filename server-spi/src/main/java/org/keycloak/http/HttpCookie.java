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

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.keycloak.common.util.ServerCookie.SameSiteAttributeValue;

/**
 * An extension of {@link javax.ws.rs.core.Cookie} in order to support additional
 * fields and behavior.
 *
 * @deprecated This class will be removed in the future. Please use {@link jakarta.ws.rs.core.NewCookie.Builder}
 */
@Deprecated(since = "24.0.0", forRemoval = true)
public final class HttpCookie extends NewCookie {

    public HttpCookie(int version, String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly, SameSiteAttributeValue sameSite) {
        super(name, value, path, domain, version, comment, maxAge, null, secure, httpOnly, convertSameSite(sameSite));
    }

    private static SameSite convertSameSite(SameSiteAttributeValue sameSiteAttributeValue) {
        if (sameSiteAttributeValue == null) {
            return null;
        }
        switch (sameSiteAttributeValue) {
            case NONE: return SameSite.NONE;
            case LAX: return SameSite.LAX;
            case STRICT: return SameSite.STRICT;
        }
        throw new IllegalArgumentException("Unknown SameSite value " + sameSiteAttributeValue);
    }

    public String toHeaderValue() {
        return RuntimeDelegate.getInstance().createHeaderDelegate(NewCookie.class).toString(this);
    }
}
