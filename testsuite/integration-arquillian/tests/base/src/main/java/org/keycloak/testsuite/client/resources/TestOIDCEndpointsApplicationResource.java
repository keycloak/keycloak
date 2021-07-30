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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.testsuite.rest.representation.TestAuthenticationChannelRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

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
    @Path("/generate-keys")
    Map<String, String> generateKeys(@QueryParam("jwaAlgorithm") String jwaAlgorithm,
            @QueryParam("advertiseJWKAlgorithm") Boolean advertiseJWKAlgorithm);

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
                        @QueryParam("state") String state,
                        @QueryParam("jwaAlgorithm") String jwaAlgorithm);

    @GET
    @Path("/set-oidc-request")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JWT)
    void setOIDCRequest(@QueryParam("realmName") String realmName, @QueryParam("clientId") String clientId,
            @QueryParam("redirectUri") String redirectUri, @QueryParam("maxAge") String maxAge,
            @QueryParam("jwaAlgorithm") String jwaAlgorithm);

    @GET
    @Path("/register-oidc-request")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JWT)
    void registerOIDCRequest(@QueryParam("requestObject") String encodedRequestObject, @QueryParam("jwaAlgorithm") String jwaAlgorithm);

    @GET
    @Path("/register-oidc-request-symmetric-sig")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JWT)
    void registerOIDCRequestSymmetricSig(@QueryParam("requestObject") String encodedRequestObject, @QueryParam("jwaAlgorithm") String jwaAlgorithm, @QueryParam("clientSecret") String clientSecret);

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

    @POST
    @Path("/request-authentication-channel")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    Response requestAuthenticationChannel(final MultivaluedMap<String, String> request);

    @GET
    @Path("/get-authentication-channel")
    @Produces(MediaType.APPLICATION_JSON)
    TestAuthenticationChannelRequest getAuthenticationChannel(@QueryParam("bindingMessage") String bindingMessage);

    /**
     * Invoke client notification endpoint. This will be called by Keycloak itself (by CIBA callback endpoint) not by testsuite
     * @param request
     */
    @POST
    @Path("/push-ciba-client-notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    void cibaClientNotificationEndpoint(ClientNotificationEndpointRequest request);

    /**
     * Return the authReqId in case that clientNotificationEndpoint was already called by Keycloak for the given clientNotificationToken. Otherwise underlying value of
     * authReqId field from the returned JSON will be null in case that clientNotificationEndpoint was not yet called for the given clientNotificationToken.
     *
     * Pushed client notification will be removed after calling this.
     *
     * @param  clientNotificationToken
     * @return
     */
    @GET
    @Path("/get-pushed-ciba-client-notification")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    ClientNotificationEndpointRequest getPushedCibaClientNotification(@QueryParam("clientNotificationToken") String clientNotificationToken);

}
