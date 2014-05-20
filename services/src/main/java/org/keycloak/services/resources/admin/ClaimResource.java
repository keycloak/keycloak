package org.keycloak.services.resources.admin;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.services.managers.ClaimManager;
import org.keycloak.services.managers.ModelToRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ClaimRepresentation getClaims() {
        auth.requireView();
        return ModelToRepresentation.toRepresentation(model);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateClaims(ClaimRepresentation rep) {
        auth.requireManage();
        ClaimManager.setClaims(model, rep);
    }
}
