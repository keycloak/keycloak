/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.ClientTypesRepresentation;

/**
 *  @since Keycloak 25. All the child endpoints are also available since that version<p>
 *
 *  This endpoint including all the child endpoints requires feature {@link org.keycloak.common.Profile.Feature#CLIENT_TYPES} to be enabled<p>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientTypesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientTypesRepresentation getClientTypes();


    /**
     * Update client types in the realm. The "global-client-types" field of client types is ignored as it is not possible to update global types
     *
     * @param clientTypes
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateClientTypes(final ClientTypesRepresentation clientTypes);
}
