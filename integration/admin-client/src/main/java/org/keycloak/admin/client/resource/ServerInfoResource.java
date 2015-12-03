package org.keycloak.admin.client.resource;

import org.keycloak.representations.info.ServerInfoRepresentation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/admin/serverinfo")
public interface ServerInfoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ServerInfoRepresentation getInfo();

}
