/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.admin.client.resource;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;

/**
  * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public interface AuthenticationFlowsResource {
    
//    @Path("{alias}")
//    public AuthenticationFlowResource get(@PathParam("alias") String alias);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(AuthenticationFlowRepresentation authenticationFlowRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuthenticationFlowRepresentation> list();
}
