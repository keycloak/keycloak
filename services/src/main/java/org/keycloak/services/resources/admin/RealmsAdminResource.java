package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.util.GenericType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmsAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmsAdminResource.class);
    protected Auth auth;
    protected TokenManager tokenManager;

    public RealmsAdminResource(Auth auth, TokenManager tokenManager) {
        this.auth = auth;
        this.tokenManager = tokenManager;
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
        List<RealmModel> realms = session.getRealms();
        List<RealmRepresentation> reps = new ArrayList<RealmRepresentation>();
        for (RealmModel realm : realms) {
            String realmAdminApp = AdminRoles.getAdminApp(realm);

            if (auth.hasAppRole(realmAdminApp, AdminRoles.MANAGE_REALM)) {
                reps.add(ModelToRepresentation.toRepresentation(realm));
            } else if (auth.hasOneOfAppRole(realmAdminApp, AdminRoles.ALL_REALM_ROLES)) {
                RealmRepresentation rep = new RealmRepresentation();
                rep.setRealm(realm.getName());
                reps.add(rep);
            }
        }
        return reps;
    }

    public static UriBuilder realmUrl(UriInfo uriInfo) {
        return realmsUrl(uriInfo).path("{id}");
    }

    public static UriBuilder realmsUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(AdminService.class).path(AdminService.class, "getRealmsAdmin");
    }

    @POST
    @Consumes("application/json")
    public Response importRealm(@Context final UriInfo uriInfo, final RealmRepresentation rep) {
        if (!auth.hasRealmRole(AdminRoles.CREATE_REALM)) {
            throw new ForbiddenException();
        }

        logger.debug("importRealm: {0}", rep.getRealm());
        RealmManager realmManager = new RealmManager(session);
        if (realmManager.getRealmByName(rep.getRealm()) != null) {
            return Flows.errors().exists("Realm " + rep.getRealm() + " already exists");
        }

        RealmModel realm = realmManager.importRealm(rep);
        grantPermissionsToRealmCreator(realm);

        URI location = realmUrl(uriInfo).build(realm.getName());
        logger.debug("imported realm success, sending back: {0}", location.toString());
        return Response.created(location).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadRealm(@Context final UriInfo uriInfo, MultipartFormDataInput input) throws IOException  {
        if (!auth.hasRealmRole(AdminRoles.CREATE_REALM)) {
            throw new ForbiddenException();
        }

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");

        RealmManager realmManager = new RealmManager(session);
        for (InputPart inputPart : inputParts) {
            inputPart.setMediaType(MediaType.APPLICATION_JSON_TYPE);
            RealmRepresentation rep = inputPart.getBody(new GenericType<RealmRepresentation>(){});

            RealmModel realm = realmManager.importRealm(rep);
            grantPermissionsToRealmCreator(realm);

            if (inputParts.size() == 1) {
                URI location = realmUrl(uriInfo).build(realm.getName());
                return Response.created(location).build();
            }
        }

        return Response.noContent().build();
    }

    private void grantPermissionsToRealmCreator(RealmModel realm) {
        if (auth.hasRealmRole(AdminRoles.ADMIN)) {
            return;
        }

        RealmModel adminRealm = new RealmManager(session).getKeycloakAdminstrationRealm();
        ApplicationModel realmAdminApp = adminRealm.getApplicationByName(AdminRoles.getAdminApp(realm));
        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.getRole(r);
            adminRealm.grantRole(auth.getUser(), role);
        }
    }


    @Path("{realm}")
    public RealmAdminResource getRealmAdmin(@Context final HttpHeaders headers,
                                            @PathParam("realm") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) throw new NotFoundException("{realm} = " + name);

        RealmAuth realmAuth = new RealmAuth(auth, AdminRoles.getAdminApp(realm));

        RealmAdminResource adminResource = new RealmAdminResource(realmAuth, realm, tokenManager);
        resourceContext.initResource(adminResource);
        return adminResource;
    }

}
