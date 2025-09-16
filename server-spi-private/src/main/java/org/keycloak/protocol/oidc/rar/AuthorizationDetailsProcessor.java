/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.UserSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.provider.Provider;

import java.util.List;

/**
 * Provider interface for processing authorization_details parameter in OAuth2/OIDC authorization and token requests.
 * This follows the RAR (Rich Authorization Requests) specification and allows different
 * implementations to handle various types of authorization details.
 * The authorization_details parameter can be used in both authorization requests and token requests
 * as specified in the OpenID for Verifiable Credential Issuance specification.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public interface AuthorizationDetailsProcessor extends Provider {

    /**
     * Processes the authorization_details parameter and returns a response if this processor
     * is able to handle the given authorization_details parameter.
     *
     * @param userSession                   the user session
     * @param clientSessionCtx              the client session context
     * @param authorizationDetailsParameter the raw authorization_details parameter value
     * @return authorization details response if this processor can handle the parameter,
     * null if the parameter is incompatible with this processor
     */
    List<AuthorizationDetailsResponse> process(UserSessionModel userSession,
                                               ClientSessionContext clientSessionCtx,
                                               String authorizationDetailsParameter);
}
