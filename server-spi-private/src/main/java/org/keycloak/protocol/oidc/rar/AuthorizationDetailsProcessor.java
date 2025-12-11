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

import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;

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
     * Checks if this processor should be regarded as supported in the running context.
     */
    boolean isSupported();

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

    /**
     * Method is invoked in cases when authorization_details parameter is missing in the request. It allows processor to
     * generate authorization details response in such a case
     *
     * @param userSession      the user session
     * @param clientSessionCtx the client session context
     * @return authorization details response if this processor can handle current request in case that authorization_details parameter was not provided
     */
    List<AuthorizationDetailsResponse> handleMissingAuthorizationDetails(UserSessionModel userSession,
                                                                         ClientSessionContext clientSessionCtx);

    /**
     * Method is invoked when authorization_details was used in the authorization request but is missing from the token request.
     * This method should process the stored authorization_details and ensure they are returned in the token response.
     *
     * @param userSession       the user session
     * @param clientSessionCtx  the client session context
     * @param storedAuthDetails the authorization_details that were stored during the authorization request
     * @return authorization details response if this processor can handle the stored authorization_details,
     * null if the processor cannot handle the stored authorization_details
     */
    List<AuthorizationDetailsResponse> processStoredAuthorizationDetails(UserSessionModel userSession,
                                                                         ClientSessionContext clientSessionCtx,
                                                                         String storedAuthDetails) throws OAuthErrorException;
}
