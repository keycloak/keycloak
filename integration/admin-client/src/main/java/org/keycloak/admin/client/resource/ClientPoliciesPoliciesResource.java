package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.ClientPoliciesRepresentation;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface ClientPoliciesPoliciesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientPoliciesRepresentation getPolicies();

    /**
     * Get client policies for the realm.
     *
     * @param includeGlobalPolicies Indicates if global server clioent policies should be included or not. Parameter available since Keycloak server 25. Will be ignored on older Keycloak versions with the default value false
     * @return client policies
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientPoliciesRepresentation getPolicies(@QueryParam("include-global-policies") Boolean includeGlobalPolicies);

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updatePolicies(final ClientPoliciesRepresentation clientPolicies);
}
