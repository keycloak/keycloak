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

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequestDetailsBuilder {

    private String providerId;

    private Map<String, String> clientAttributes;

    private Map<String, String> socialAttributes;

    private RequestDetailsBuilder() {
    }

    public static RequestDetailsBuilder create(String providerId) {
        RequestDetailsBuilder req = new RequestDetailsBuilder();
        req.providerId = providerId;
        req.clientAttributes = new HashMap<String, String>();
        req.socialAttributes = new HashMap<String, String>();
        return req;
    }

    public RequestDetailsBuilder putClientAttribute(String name, String value) {
        clientAttributes.put(name, value);
        return this;
    }

    public RequestDetailsBuilder putClientAttributes(Map<String, String> attributes) {
        clientAttributes.putAll(attributes);
        return this;
    }

    public RequestDetailsBuilder putSocialAttribute(String name, String value) {
        socialAttributes.put(name, value);
        return this;
    }

    public RequestDetailsBuilder putSocialAttributes(Map<String, String> attributes) {
        socialAttributes.putAll(attributes);
        return this;
    }

    public RequestDetails build() {
        return new RequestDetails(providerId, clientAttributes, socialAttributes);
    }

}
