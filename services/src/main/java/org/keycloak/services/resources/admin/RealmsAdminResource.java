package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.SaasService;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmsAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmsAdminResource.class);
    protected UserModel admin;

    public RealmsAdminResource(UserModel admin) {
        this.admin = admin;
    }

    public static final CacheControl noCache = new CacheControl();
    static {
        noCache.setNoCache(true);
    }

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    @GET
    @NoCache
    @Produces("application/json")
    public List<RealmRepresentation> getRealms() {
        logger.debug(("getRealms()"));
        RealmManager realmManager = new RealmManager(session);
        List<RealmModel> realms = session.getRealms(admin);
        List<RealmRepresentation> reps = new ArrayList<RealmRepresentation>();
        for (RealmModel realm : realms) {
            reps.add(realmManager.toRepresentation(realm));
        }
        return reps;
    }

    public static UriBuilder realmUrl(UriInfo uriInfo) {
        return realmsUrl(uriInfo).path("{id}");
    }

    public static UriBuilder realmsUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(SaasService.class).path(SaasService.class, "getRealmsAdmin");
    }

    @POST
    @Consumes("application/json")
    public Response importRealm(@Context final UriInfo uriInfo, final RealmRepresentation rep) {
        logger.debug("importRealm: {0}", rep.getRealm());
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.importRealm(rep, admin);
        URI location = realmUrl(uriInfo).build(realm.getId());
        logger.debug("imported realm success, sending back: {0}", location.toString());
        return Response.created(location).build();
    }

    @Path("{id}")
    public RealmAdminResource getRealmAdmin(@Context final HttpHeaders headers,
                                            @PathParam("id") final String id) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealm(id);
        if (realm == null) throw new NotFoundException();

        RealmAdminResource adminResource = new RealmAdminResource(admin, realm);
        resourceContext.initResource(adminResource);
        return adminResource;
    }


}
