/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.social;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthRequest {

    private URI authUri;

    private Map<String, String> attributes;

    public static AuthRequestBuilder create(String url) {
        AuthRequestBuilder req = new AuthRequestBuilder();

        req.b = new StringBuilder();
        req.b.append(url);

        return req;
    }

    private AuthRequest(URI authUri, Map<String, String> attributes) {
        this.authUri = authUri;
        this.attributes = attributes;
    }

    public URI getAuthUri() {
        return authUri;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public static class AuthRequestBuilder {

        private StringBuilder b;

        private char sep;

        private Map<String, String> attributes;

        private String id;

        private AuthRequestBuilder() {
            sep = '?';
        }

        public AuthRequestBuilder setQueryParam(String name, String value) {
            try {
                if (sep == '?') {
                    b.append(sep);
                    sep = '&';
                } else {
                    b.append(sep);
                }
                b.append(URLEncoder.encode(name, "UTF-8"));
                b.append("=");
                b.append(URLEncoder.encode(value, "UTF-8"));
                return this;
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        public AuthRequestBuilder setAttribute(String name, String value) {
            if (attributes == null) {
                attributes = new HashMap<String, String>();
            }
            attributes.put(name, value);
            return this;
        }

        public AuthRequest build() {
            try {
                return new AuthRequest(new URI(b.toString()), attributes);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

    }

}
