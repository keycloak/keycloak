package org.keycloak.services.resources.admin;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.ResourceRepresentation;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmResourcesResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;

    public RealmResourcesResource(UserModel admin, RealmModel realm) {
        this.admin = admin;
        this.realm = realm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ResourceRepresentation> getResources() {
        return null;
    }
}
