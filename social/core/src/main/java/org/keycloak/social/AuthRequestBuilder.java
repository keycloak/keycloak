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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthRequestBuilder {

    private UriBuilder b;
    
    private Map<String, Object> attributes;

    private String id;

    private AuthRequestBuilder() {
    }

    public static AuthRequestBuilder create(String id, String path) {
        AuthRequestBuilder req = new AuthRequestBuilder();
        req.id = id;
        req.b = UriBuilder.fromUri(path);
        req.attributes = new HashMap<String, Object>();
        return req;
    }

    public AuthRequestBuilder setQueryParam(String name, String value) {
        b.queryParam(name, value);
        return this;
    }

    public AuthRequestBuilder setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public AuthRequest build() {
        return new AuthRequest(id, b.build(), attributes);
    }

}
