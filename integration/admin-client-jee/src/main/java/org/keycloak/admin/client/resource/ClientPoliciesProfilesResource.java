package org.keycloak.admin.client.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
