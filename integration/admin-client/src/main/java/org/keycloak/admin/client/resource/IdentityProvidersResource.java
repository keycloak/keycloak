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

package org.keycloak.admin.client.resource;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * @author pedroigor
 */
public interface IdentityProvidersResource {

    @Path("instances/{alias}")
    IdentityProviderResource get(@PathParam("alias") String alias);

    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    List<IdentityProviderRepresentation> findAll();

    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    List<IdentityProviderRepresentation> find(@QueryParam("type") String type, @QueryParam("capability") String capability,
                                              @QueryParam("search") String search, @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                              @QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults);

    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    List<IdentityProviderRepresentation> find(@QueryParam("search") String search, @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                              @QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults);

    /**
     * Get the paginated list of identity providers, filtered according to the specified parameters.
     *
     * @param search Filter to search specific providers by name. Search can be prefixed (name*), contains (*name*) or exact (\"name\"). Default prefixed.
     * @param briefRepresentation Boolean which defines whether brief representations are returned (default: false).
     *                            If true, only basic data like ID, alias, providerId and enabled status will be returned in the result
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param realmOnly Boolean which defines if only realm-level IDPs (not associated with orgs) should be returned (default: false).
     *                  Parameter available since Keycloak server 26. Will be ignored on older Keycloak versions with the default value false
     * @return The list of providers.
     */
    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    List<IdentityProviderRepresentation> find(@QueryParam("search") String search, @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                              @QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults,
                                              @QueryParam("realmOnly") Boolean realmOnly);

    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(IdentityProviderRepresentation identityProvider);

    @GET
    @Path("/providers/{provider_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getIdentityProviders(@PathParam("provider_id") String providerId);

    @POST
    @Path("import-config")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> importFrom(Object data);

    @POST
    @Path("import-config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> importFrom(Map<String, Object> data);
}
