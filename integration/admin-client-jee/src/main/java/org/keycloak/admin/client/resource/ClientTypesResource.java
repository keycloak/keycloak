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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.representations.idm.ClientTypesRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientTypesResource {

    @GET
    @NoCache
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
