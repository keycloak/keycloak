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

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

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
    Map<String, String> importFrom(MultipartFormDataOutput data);

    @POST
    @Path("import-config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> importFrom(Map<String, Object> data);
}
