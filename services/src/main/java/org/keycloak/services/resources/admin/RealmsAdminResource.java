package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.PublicRealmResource;
import org.keycloak.services.resources.SaasService;
import org.keycloak.services.resources.Transaction;

import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GET
    @NoCache
    @Produces("application/json")
    public Response getRealms() {
        return new Transaction() {
            @Override
            protected  Response callImpl() {
                logger.info(("getRealms()"));
                List<RealmModel> realms = session.getRealms(admin);
                Map<String, String> map = new HashMap<String, String>();
                for (RealmModel realm : realms) {
                    map.put(realm.getId(), realm.getName());
                }
                return Response.ok(new GenericEntity<Map<String, String>>(map){})
                               .cacheControl(noCache).build();
            }
        }.call();
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
        logger.info("importRealm: " + rep.getRealm());
        return new Transaction() {
            @Override
            protected Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.importRealm(rep, admin);
                URI location = realmUrl(uriInfo).build(realm.getId());
                logger.info("imported realm success, sending back: " + location.toString());
                return Response.created(location).build();
            }
        }.call();
    }

    @Path("{id}")
    public RealmAdminResource getRealmAdmin(@Context final HttpHeaders headers,
                                            @PathParam("id") final String id) {
        return new Transaction(false) {
            @Override
            protected RealmAdminResource callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.getRealm(id);
                if (realm == null) throw new NotFoundException();
                if (!realm.isRealmAdmin(admin)) {
                    throw new ForbiddenException();
                }

                return new RealmAdminResource(admin, realm);
            }
        }.call();
    }


}
