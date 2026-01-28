/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.utils;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.common.util.UriUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

/**
 * Created by st on 22.09.15.
 */
public class WebOriginsUtils {

    public static final String INCLUDE_REDIRECTS = "+";

    public static Set<String> resolveValidWebOrigins(KeycloakSession session, ClientModel client) {
        Set<String> origins = new HashSet<>();
        if (client.getWebOrigins() != null) {
            origins.addAll(client.getWebOrigins());
        }
        if (origins.contains(INCLUDE_REDIRECTS)) {
            origins.remove(INCLUDE_REDIRECTS);
            for (String redirectUri : RedirectUtils.resolveValidRedirects(session, client.getRootUrl(), client.getRedirectUris())) {
                if (redirectUri.startsWith("http://") || redirectUri.startsWith("https://")) {
                    origins.add(UriUtils.getOrigin(redirectUri));
                }
            }
        }
        return origins;
    }

}
