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

package org.keycloak.testsuite.client.resources;

import org.keycloak.jose.jwk.JSONWebKeySet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface TestOIDCEndpointsApplicationResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generate-keys")
    Map<String, String> generateKeys(@QueryParam("jwaAlgorithm") String jwaAlgorithm);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/get-keys-as-pem")
    Map<String, String> getKeysAsPem();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/get-keys-as-base64")
    Map<String, String> getKeysAsBase64();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/get-jwks")
    JSONWebKeySet getJwks();


    @GET
    @Path("/set-oidc-request")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JWT)
    void setOIDCRequest(@QueryParam("realmName") String realmName, @QueryParam("clientId") String clientId,
                        @QueryParam("redirectUri") String redirectUri, @QueryParam("maxAge") String maxAge,
                        @QueryParam("jwaAlgorithm") String jwaAlgorithm);

    @GET
    @Path("/get-oidc-request")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JWT)
    String getOIDCRequest();

    @GET
    @Path("/set-sector-identifier-redirect-uris")
    @Produces(MediaType.APPLICATION_JSON)
    void setSectorIdentifierRedirectUris(@QueryParam("redirectUris") List<String> redirectUris);

    @GET
    @Path("/get-sector-identifier-redirect-uris")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> getSectorIdentifierRedirectUris();

}
