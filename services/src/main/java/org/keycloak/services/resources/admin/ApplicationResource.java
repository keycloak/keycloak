package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmModel realm;
    private RealmAuth auth;
    protected ApplicationModel application;
    protected KeycloakSession session;
    @Context
    protected UriInfo uriInfo;

    @Context
    protected Application keycloak;

    protected KeycloakApplication getKeycloakApplication() {
        return (KeycloakApplication)keycloak;
    }

    public ApplicationResource(RealmModel realm, RealmAuth auth, ApplicationModel applicationModel, KeycloakSession session) {
        this.realm = realm;
        this.auth = auth;
        this.application = applicationModel;
        this.session = session;

        auth.init(RealmAuth.Resource.APPLICATION);
    }

    @Path("claims")
    public ClaimResource getClaimResource() {
        return new ClaimResource(application);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(final ApplicationRepresentation rep) {
        auth.requireManage();

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));
        applicationManager.updateApplication(rep, application);
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationRepresentation getApplication() {
        auth.requireView();

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));
        return applicationManager.toRepresentation(application);
    }


    @GET
    @NoCache
    @Path("installation/json")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstallation() throws IOException {
        auth.requireView();

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));
        Object rep = applicationManager.toInstallationRepresentation(realm, application, getKeycloakApplication().getBaseUri(uriInfo));

        // TODO Temporary solution to pretty-print
        return JsonSerialization.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rep);
    }

    @GET
    @NoCache
    @Path("installation/jboss")
    @Produces(MediaType.TEXT_PLAIN)
    public String getJBossInstallation() throws IOException {
        auth.requireView();

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));
        return applicationManager.toJBossSubsystemConfig(realm, application, getKeycloakApplication().getBaseUri(uriInfo));
    }

    @DELETE
    @NoCache
    public void deleteApplication() {
        auth.requireManage();

        realm.removeApplication(application.getId());
    }

    @Path("client-secret")
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public CredentialRepresentation regenerateSecret() {
        auth.requireManage();

        logger.debug("regenerateSecret");
        UserCredentialModel cred = new ApplicationManager().generateSecret(application);
        CredentialRepresentation rep = ModelToRepresentation.toRepresentation(cred);
        return rep;
    }

    @Path("client-secret")
    @GET
    @Produces("application/json")
    public CredentialRepresentation getClientSecret() {
        auth.requireView();

        logger.debug("getClientSecret");
        UserCredentialModel model = UserCredentialModel.secret(application.getSecret());
        if (model == null) throw new NotFoundException("Application does not have a secret");
        return ModelToRepresentation.toRepresentation(model);
    }


    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        return new ScopeMappedResource(realm, auth, application, session);
    }

    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(realm, auth, application);
    }

    @Path("allowed-origins")
    @GET
    @Produces("application/json")
    public Set<String> getAllowedOrigins()
    {
        auth.requireView();

        return application.getWebOrigins();
    }

    @Path("allowed-origins")
    @PUT
    @Consumes("application/json")
    public void updateAllowedOrigins(Set<String> allowedOrigins)
    {
        auth.requireManage();

        application.setWebOrigins(allowedOrigins);
    }

    @Path("allowed-origins")
    @DELETE
    @Consumes("application/json")
    public void deleteAllowedOrigins(Set<String> allowedOrigins)
    {
        auth.requireManage();

        for (String origin : allowedOrigins) {
            application.removeWebOrigin(origin);
        }
    }



}
