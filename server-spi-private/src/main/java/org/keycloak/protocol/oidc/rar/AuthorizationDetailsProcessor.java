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

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.representations.AuthorizationDetailsResponse;

/**
 * Provider interface for processing authorization_details parameter in OAuth2/OIDC authorization and token requests.
 * This follows the RAR (Rich Authorization Requests) specification and allows different
 * implementations to handle various types of authorization details.
 * The authorization_details parameter can be used in both authorization requests and token requests
 * (as specified for example in the OpenID for Verifiable Credential Issuance specification).
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public interface AuthorizationDetailsProcessor<ADR extends AuthorizationDetailsResponse> extends Provider {

    /**
     * Checks if this processor should be regarded as supported in the running context.
     */
    boolean isSupported();

    /**
     * @return supported type of authorization_details "type" claim, which this processor is able to process. This should usually correspond with the "providerId" of
     * the {@link AuthorizationDetailsProcessorFactory}, which created this processor
     */
    String getSupportedType();

    /**
     * @return supported Java type of {@link AuthorizationDetailsResponse} subclass, which this processor can create in the token response
     */
    Class<ADR> getSupportedResponseJavaType();

    /**
     * Processes the authorization_details parameter and returns a response if this processor
     * is able to handle the given authorization_details parameter.
     *
     * @param userSession                   the user session
     * @param clientSessionCtx              the client session context
     * @param authorizationDetailsMember the authorization_details member (usually one member from the list) sent in the "authorization_details" request parameter
     * @return authorization details response if this processor can handle the parameter,
     * null if the parameter is incompatible with this processor
     */
    ADR process(UserSessionModel userSession,
                ClientSessionContext clientSessionCtx,
                AuthorizationDetailsJSONRepresentation authorizationDetailsMember) throws InvalidAuthorizationDetailsException;

    /**
     * Method is invoked in cases when authorization_details parameter is missing in the request. It allows processor to
     * generate authorization details response in such a case
     *
     * @param userSession      the user session
     * @param clientSessionCtx the client session context
     * @return authorization details response if this processor can handle current request in case that authorization_details parameter was not provided
     */
    List<ADR> handleMissingAuthorizationDetails(UserSessionModel userSession,
                                                ClientSessionContext clientSessionCtx) throws InvalidAuthorizationDetailsException;

    /**
     * Method is invoked when authorization_details was used in the authorization request but is missing from the token request.
     * This method should process the stored authorization_details and ensure they are returned in the token response.
     *
     * @param userSession       the user session
     * @param clientSessionCtx  the client session context
     * @param storedAuthDetailsMember the parsed member (usually one member of the list) from the authorization_details parameter that were stored during the authorization request
     * @return authorization details response if this processor can handle the stored authorization_details,
     * null if the processor cannot handle the stored authorization_details
     */
    ADR processStoredAuthorizationDetails(UserSessionModel userSession,
                                          ClientSessionContext clientSessionCtx,
                                          AuthorizationDetailsJSONRepresentation storedAuthDetailsMember) throws InvalidAuthorizationDetailsException;

    /**
     * @param authzDetailsResponse all the authorizationDetails. May contain also authorizationDetails entries, with different "type" than the type understandable by this processor
     * @return sublist of the list provided by "authDetailsResponse" parameter, which will contain just the authorizationDetails of the corresponding type of this processor.
     */
    default List<ADR> getSupportedAuthorizationDetails(List<AuthorizationDetailsResponse> authzDetailsResponse) {
        if (authzDetailsResponse == null) {
            return null;
        }
        return authzDetailsResponse.stream()
                .filter(authDetailsResponse -> getSupportedType().equals(authDetailsResponse.getType()))
                .map(authDetailsResponse -> authDetailsResponse.asSubtype(getSupportedResponseJavaType()))
                .toList();
    }

}
