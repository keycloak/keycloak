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

package org.keycloak.admin.client.resource;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
public interface ClientScopeEvaluateResource {

    /**
     * Generate example access token
     *
     * @param scopeParam Value of the "scope" parameter. Endpoint simulates generating of the access-token as if the "scope" parameter with specified value is used. Could be null.
     * @param userId User ID
     * @param audience Value of the "audience" parameter. Endpoint simulates generating of the access-token as if the "audience" parameter with specified value is used. Audience parameter is supported
     *                 just for some grants (EG. for the token exchange). For most of the grants where "audience" parameter is not supported, it is better to use the value null to simulate generating of the appropriate token.
     *                 Parameter is supported since Keycloak 26.2.
     * @return generated access token
     */
    @GET
    @Path("generate-example-access-token")
    AccessToken generateAccessToken(@QueryParam("scope") String scopeParam, @QueryParam("userId") String userId, @QueryParam("audience") String audience);

    /**
     * Generate example ID token
     *
     * @param scopeParam Value of the "scope" parameter. Endpoint simulates generating of the ID-token as if the "scope" parameter with specified value is used. Could be null.
     * @param userId User ID
     * @param audience Value of the "audience" parameter. Endpoint simulates generating of the access-token as if the "audience" parameter with specified value is used. Audience parameter is supported
     *                 just for some grants (EG. for the token exchange). For most of the grants where "audience" parameter is not supported, it is better to use the value null to simulate generating of the appropriate token.
     *                 Parameter is supported since Keycloak 26.2.
     * @return generated ID token
     */
    @GET
    @Path("generate-example-id-token")
    IDToken generateExampleIdToken(@QueryParam("scope") String scopeParam, @QueryParam("userId") String userId, @QueryParam("audience") String audience);

    /**
     * Generate example user-info response
     *
     * @param scopeParam  Value of the "scope" parameter. Endpoint simulates generating of the user-info as if the "scope" parameter with specified value is used for generating the access-token, which would then be used for the user-info request. Could be null.
     * @param userId User ID
     * @return generated user-info
     */
    @GET
    @Path("generate-example-userinfo")
    Map<String, Object> generateExampleUserinfo(@QueryParam("scope") String scopeParam, @QueryParam("userId") String userId);

}
