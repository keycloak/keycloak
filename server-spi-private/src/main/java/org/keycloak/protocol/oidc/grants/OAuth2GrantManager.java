/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.EventType;

/**
 * A class to register and resolve OAuth 2.0 Grant Type implementations
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public class OAuth2GrantManager {

    private static final MultivaluedHashMap<String, OAuth2GrantType> GRANT_TYPE_MAP = new MultivaluedHashMap<>();
    private static final Map<String, EventType> EVENT_TYPE_MAP = new HashMap<>();

    static {
            EVENT_TYPE_MAP.put(OAuth2Constants.AUTHORIZATION_CODE, EventType.CODE_TO_TOKEN);
            EVENT_TYPE_MAP.put(OAuth2Constants.REFRESH_TOKEN, EventType.REFRESH_TOKEN);
            EVENT_TYPE_MAP.put(OAuth2Constants.PASSWORD, EventType.LOGIN);
            EVENT_TYPE_MAP.put(OAuth2Constants.CLIENT_CREDENTIALS, EventType.CLIENT_LOGIN);
            EVENT_TYPE_MAP.put(OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE, EventType.TOKEN_EXCHANGE);
            EVENT_TYPE_MAP.put(OAuth2Constants.UMA_GRANT_TYPE, EventType.PERMISSION_TOKEN);
            EVENT_TYPE_MAP.put(OAuth2Constants.CIBA_GRANT_TYPE, EventType.AUTHREQID_TO_TOKEN);
            EVENT_TYPE_MAP.put(OAuth2Constants.DEVICE_CODE_GRANT_TYPE, EventType.OAUTH2_DEVICE_CODE_TO_TOKEN);
    }

    /**
     * Register OAuth 2.0 grant type
     * @param grant grant type to register
     */
    public static void register(OAuth2GrantType grant) {
        GRANT_TYPE_MAP.add(grant.getGrantType(), grant);
    }

    /**
     * Resolve grant type implementation from grant type and request context
     * @param grantType grant type
     * @param context grant request context
     * @return grant type implementation
     */
    public static Optional<OAuth2GrantType> resolve(String grantType, OAuth2GrantType.Context context) {
        return resolve(resolve(grantType), context);
    }

    /**
     * Return registered implementations for the given grant type
     * @param grantType grant type
     * @return list of implementations
     */
    public static List<OAuth2GrantType> resolve(String grantType) {
        return GRANT_TYPE_MAP.get(grantType);
    }

    /**
     * Select the "best" grant type implementation from a list of candidates, using supports() and then priority
     * @param grants a list of candidate implementations
     * @param context grant request context
     * @return grant type implementation
     */
    public static Optional<OAuth2GrantType> resolve(List<OAuth2GrantType> grants, OAuth2GrantType.Context context) {
        Optional<OAuth2GrantType> grant;

        switch (grants.size()) {
            case 0:
                grant = Optional.empty();
                break;
            case 1:
                grant = Optional.of(grants.get(0));
                break;
            default:
                grant = grants.stream().filter(g -> g.supports(context)).sorted((g1, g2) -> g2.order() - g1.order()).findFirst();
                break;
        }

        return grant.map(g -> g.create(context.getSession()));
    }

    /**
     * Map well-known OAuth 2.0 grant types to Keycloak event types
     * @param grantType
     * @return
     */
    public static EventType grantToEvent(String grantType) {
        return EVENT_TYPE_MAP.getOrDefault(grantType, EventType.OAUTH2_EXTENSION_GRANT);
    }

}
