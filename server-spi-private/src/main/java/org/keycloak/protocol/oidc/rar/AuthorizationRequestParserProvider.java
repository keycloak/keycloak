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
package org.keycloak.protocol.oidc.rar;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;
import org.keycloak.rar.AuthorizationRequestContext;

/**
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
public interface AuthorizationRequestParserProvider extends Provider {

    AuthorizationRequestContext parseScopes(@Nonnull ClientModel client, @Nullable String scopeParam);

    default AuthorizationRequestContext parseScopes(@Nullable UserModel user, @Nonnull ClientModel client, @Nullable String scopeParam) {
        return parseScopes(client, scopeParam);
    }

    /**
     * Same as {@link #parseScopes(UserModel, ClientModel, String)}, but also given the client session scopes are
     * being resolved for, when one already exists (e.g. {@code null} before any client/user session exists, such as
     * a consent screen preview). Needed for validations with side effects tied to that client session (e.g. identity
     * pinning).
     *
     * @param clientSession the client session scopes are being resolved for, or {@code null}
     */
    default AuthorizationRequestContext parseScopes(@Nullable UserModel user, @Nonnull ClientModel client,
            @Nullable AuthenticatedClientSessionModel clientSession, @Nullable String scopeParam) {
        return parseScopes(user, client, scopeParam);
    }

}
