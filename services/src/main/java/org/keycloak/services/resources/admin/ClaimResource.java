package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.ClientModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClaimRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Base resource class for managing allowed claims for an application or oauth client
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClaimResource {
    protected ClientModel model;
    protected RealmAuth auth;

    public ClaimResource(ClientModel model, RealmAuth auth) {
        this.model = model;
        this.auth = auth;
    }

    /**
     * Get the claims a client is allowed to ask for
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ClaimRepresentation getClaims() {
        auth.requireView();
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Set the cliams a client is allowed to ask for.
     *
     * @param rep
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateClaims(ClaimRepresentation rep) {
        auth.requireManage();
        RepresentationToModel.setClaims(model, rep);
    }
}
