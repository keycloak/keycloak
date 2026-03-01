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

package org.keycloak.protocol.saml.clientregistration;

import java.net.URI;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.EntityDescriptorDescriptionConverter;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientregistration.AbstractClientRegistrationProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EntityDescriptorClientRegistrationProvider extends AbstractClientRegistrationProvider {

    public EntityDescriptorClientRegistrationProvider(KeycloakSession session) {
        super(session);
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSaml(String descriptor) {
        ClientRepresentation client = session.getProvider(ClientDescriptionConverter.class, EntityDescriptorDescriptionConverter.ID).convertToInternal(descriptor);
        EntityDescriptorClientRegistrationContext context = new EntityDescriptorClientRegistrationContext(session, client, this);
        client = create(context);
        validateClient(client, true);
        URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
        return Response.created(uri).entity(client).build();
    }



}
