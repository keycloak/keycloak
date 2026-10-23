/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc;

import java.util.OptionalInt;

import org.keycloak.provider.Provider;

/**
 * Maps the acr values requested by an OIDC client, through the acr_values parameter or the acr claim of the claims parameter,
 * to the numerical level of authentication used by the authentication flow.
 */
public interface OIDCLevelOfAuthenticationProvider extends Provider {
    /**
     * Maps a single requested acr value to a level of authentication.
     *
     * @param acrValue requested acr value
     * @return numerical level of authentication or an empty OptionalInt if the acr value cannot be mapped
     */
    OptionalInt getRequestedLoa(String acrValue);
}
