package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Top level resource for Admin REST API
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmsAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmsAdminResource.class);
    protected AdminAuth auth;
    protected TokenManager tokenManager;

    public RealmsAdminResource(AdminAuth auth, TokenManager tokenManager) {
        this.auth = auth;
        this.tokenManager = tokenManager;
    }

    public static final CacheControl noCache = new CacheControl();

    static {
        noCache.setNoCache(true);
    }

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    @Context
    protected KeycloakSession session;

    @Context
    protected KeycloakApplication keycloak;


    /**
     * Returns a list of realms.  This list is filtered based on what realms the caller is allowed to view.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces("application/json")
    public List<RealmRepresentation> getRealms() {
        RealmManager realmManager = new RealmManager(session);
        List<RealmRepresentation> reps = new ArrayList<RealmRepresentation>();
        if (auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm())) {
            List<RealmModel> realms = session.realms().getRealms();
            for (RealmModel realm : realms) {
                addRealmRep(reps, realm, realm.getMasterAdminApp());
            }
        } else {
            ApplicationModel adminApp = auth.getRealm().getApplicationByName(realmManager.getRealmAdminApplicationName(auth.getRealm()));
            addRealmRep(reps, auth.getRealm(), adminApp);
        }
        logger.debug(("getRealms()"));
        return reps;
    }


    protected void addRealmRep(List<RealmRepresentation> reps, RealmModel realm, ApplicationModel realmManagementApplication) {
        if (auth.hasAppRole(realmManagementApplication, AdminRoles.MANAGE_REALM)) {
            reps.add(ModelToRepresentation.toRepresentation(realm, false));
        } else if (auth.hasOneOfAppRole(realmManagementApplication, AdminRoles.ALL_REALM_ROLES)) {
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());
            reps.add(rep);
        }
    }

    /**
     * Import a realm from a full representation of that realm.  Realm name must be unique.
     *
     * @param uriInfo
     * @param rep JSON representation
     * @return
     */
    @POST
    @Consumes("application/json")
    public Response importRealm(@Context final UriInfo uriInfo, final RealmRepresentation rep) {
        RealmManager realmManager = new RealmManager(session);
        realmManager.setContextPath(keycloak.getContextPath());
        if (!auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm())) {
            throw new ForbiddenException();
        }
        if (!auth.hasRealmRole(AdminRoles.CREATE_REALM)) {
            throw new ForbiddenException();
        }

        logger.debugv("importRealm: {0}", rep.getRealm());

        try {
            RealmModel realm = realmManager.importRealm(rep);
            grantPermissionsToRealmCreator(realm);

            URI location = AdminRoot.realmsUrl(uriInfo).path(realm.getName()).build();
            logger.debugv("imported realm success, sending back: {0}", location.toString());
            return Response.created(location).build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Realm " + rep.getRealm() + " already exists");
        }
    }

    /**
     * Upload a realm from a uploaded JSON file.  The posted represenation is expected to be a multipart/form-data encapsulation
     * of a JSON file.  The same format a browser would use when uploading a file.
     *
     * @param uriInfo
     * @param input multipart/form data
     * @return
     * @throws IOException
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadRealm(@Context final UriInfo uriInfo, MultipartFormDataInput input) throws IOException {
        RealmManager realmManager = new RealmManager(session);
        realmManager.setContextPath(keycloak.getContextPath());
        if (!auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm())) {
            throw new ForbiddenException();
        }
        if (!auth.hasRealmRole(AdminRoles.CREATE_REALM)) {
            throw new ForbiddenException();
        }

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");

        for (InputPart inputPart : inputParts) {
            // inputPart.getBody doesn't work as content-type is wrong, and inputPart.setMediaType is not supported on AS7 (RestEasy 2.3.2.Final)
            RealmRepresentation rep = JsonSerialization.readValue(inputPart.getBodyAsString(), RealmRepresentation.class);
            RealmModel realm;
            try {
                realm = realmManager.importRealm(rep);
            } catch (ModelDuplicateException e) {
                return Flows.errors().exists("Realm " + rep.getRealm() + " already exists");
            }

            grantPermissionsToRealmCreator(realm);

            if (inputParts.size() == 1) {
                URI location = AdminRoot.realmsUrl(uriInfo).path(realm.getName()).build();
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
        ApplicationModel realmAdminApp = realm.getMasterAdminApp();
        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.getRole(r);
            auth.getUser().grantRole(role);
        }
    }

    /**
     * Base path for the admin REST API for one particular realm.
     *
     * @param headers
     * @param name realm name (not id!)
     * @return
     */
    @Path("{realm}")
    public RealmAdminResource getRealmAdmin(@Context final HttpHeaders headers,
                                            @PathParam("realm") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) throw new NotFoundException("{realm} = " + name);

        if (!auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm())
                && !auth.getRealm().equals(realm)) {
            throw new ForbiddenException();
        }
        RealmAuth realmAuth;

        if (auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm())) {
            realmAuth = new RealmAuth(auth, realm.getMasterAdminApp());
        } else {
            realmAuth = new RealmAuth(auth, realm.getApplicationByName(realmManager.getRealmAdminApplicationName(auth.getRealm())));
        }

        RealmAdminResource adminResource = new RealmAdminResource(realmAuth, realm, tokenManager);
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        //resourceContext.initResource(adminResource);
        return adminResource;
    }

}
