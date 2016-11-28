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

import org.keycloak.representations.idm.SynchronizationResultRepresentation;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserStorageProviderResource {
    @POST
    @Path("{componentId}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    SynchronizationResultRepresentation syncUsers(@QueryParam("action") String action);

    @POST
    @Path("{componentId}/mappers/{mapperId}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    SynchronizationResultRepresentation syncMapperData(@PathParam("componentId") String componentId, @PathParam("mapperId") String mapperId, @QueryParam("direction") String direction);


}
