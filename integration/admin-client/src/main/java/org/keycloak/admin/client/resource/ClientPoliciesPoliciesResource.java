package org.keycloak.admin.client.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.keycloak.representations.idm.ClientPoliciesRepresentation;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface ClientPoliciesPoliciesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientPoliciesRepresentation getPolicies();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updatePolicies(final ClientPoliciesRepresentation clientPolicies);
}

