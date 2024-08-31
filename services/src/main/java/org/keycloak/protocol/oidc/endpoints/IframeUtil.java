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

import org.keycloak.common.Version;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.utils.ReplacingInputStream;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.Exception;
import java.util.UUID;

public class IframeUtil {
    private static final String NONCE_IDENTIFIER = "$NONCE_SCRIPT";

    public static Response returnIframeFromResources(String fileName, String version, KeycloakSession session) {
        CacheControl cacheControl;
        if (version != null) {
            if (!version.equals(Version.RESOURCES_VERSION)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            cacheControl = CacheControlUtil.getDefaultCacheControl();
        } else {
            cacheControl = CacheControlUtil.noCache();
        }

        InputStream resource = IframeUtil.class.getResourceAsStream(fileName);
        if (resource != null) {
            session.getProvider(SecurityHeadersProvider.class).options().allowAnyFrameAncestor();

            try {
                String scriptNonce = UUID.randomUUID().toString();
                session.getProvider(SecurityHeadersProvider.class).options().addScriptSrc("'nonce-" + scriptNonce + "'");

                resource = new ReplacingInputStream(resource, NONCE_IDENTIFIER, scriptNonce);
            }
            catch (Exception _e) {
            }

            return Response.ok(resource).cacheControl(cacheControl).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
