package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.ClientProfilesRepresentation;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface ClientPoliciesProfilesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientProfilesRepresentation getProfiles(@QueryParam("include-global-profiles") Boolean includeGlobalProfiles);

    /**
     * Update client profiles in the realm. The "globalProfiles" field of clientProfiles is ignored as it is not possible to update global profiles
     *
     * @param clientProfiles
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateProfiles(final ClientProfilesRepresentation clientProfiles);
}
