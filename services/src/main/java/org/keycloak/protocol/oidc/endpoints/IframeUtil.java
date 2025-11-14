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

package org.keycloak.protocol.oidc.endpoints;

import java.util.function.Supplier;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Version;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.util.CacheControlUtil;

public class IframeUtil {

    public static Response returnIframeFromResources(String fileName, String version, KeycloakSession session) {
        return returnIframe(version, session, () -> IframeUtil.class.getResourceAsStream(fileName));
    }

    public static Response returnIframe(String version, KeycloakSession session, Supplier<Object> responseEntityProvider) {
        CacheControl cacheControl;
        if (version != null) {
            if (!version.equals(Version.RESOURCES_VERSION)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            cacheControl = CacheControlUtil.getDefaultCacheControl();
        } else {
            cacheControl = CacheControlUtil.noCache();
        }

        Object resource = responseEntityProvider.get();
        if (resource != null) {
            session.getProvider(SecurityHeadersProvider.class).options().allowAnyFrameAncestor();
            return Response.ok(resource).cacheControl(cacheControl).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
