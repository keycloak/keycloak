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

package org.keycloak.adapters.authorization;

import java.security.Principal;

import org.keycloak.adapters.authorization.util.JsonUtils;
import org.keycloak.representations.AccessToken;

/**
 * A {@link Principal} backed by a token representing the entity requesting permissions.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface TokenPrincipal extends Principal {

    /**
     * The token in its raw format.
     *
     * @return the token in its raw format.
     */
    String getRawToken();

    /**
     * The {@link AccessToken} representation of {@link TokenPrincipal#getRawToken()}.
     *
     * @return the access token representation
     */
    default AccessToken getToken() {
        return JsonUtils.asAccessToken(getRawToken());
    }

    /**
     * The name of the entity represented by the token.
     *
     * @return the name of the principal
     */
    default String getName() {
        return getToken().getPreferredUsername();
    }
}
