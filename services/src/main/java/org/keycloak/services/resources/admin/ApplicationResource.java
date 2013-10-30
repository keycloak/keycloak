package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.representations.idm.*;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationResource extends RoleContainerResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmModel realm;
    protected ApplicationModel application;
    protected KeycloakSession session;

    public ApplicationResource(RealmModel realm, ApplicationModel applicationModel, KeycloakSession session) {
        super(applicationModel);
        this.realm = realm;
        this.application = applicationModel;
        this.session = session;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(final ApplicationRepresentation rep) {
        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));
        applicationManager.updateApplication(rep, application);
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationRepresentation getApplication() {
        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));
        return applicationManager.toRepresentation(application);
    }

    @Path("credentials")
    @PUT
    @Consumes("application/json")
    public void updateCredentials(List<CredentialRepresentation> credentials) {
        logger.debug("updateCredentials");
        if (credentials == null) return;

        for (CredentialRepresentation rep : credentials) {
            UserCredentialModel cred = RealmManager.fromRepresentation(rep);
            realm.updateCredential(application.getApplicationUser(), cred);
        }
    }

    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        return new ScopeMappedResource(realm, application.getApplicationUser(), session);
    }

    @Path("allowed-origins")
    @GET
    @Produces("application/json")
    public Set<String> getAllowedOrigins()
    {
        return application.getApplicationUser().getWebOrigins();
    }

    @Path("allowed-origins")
    @PUT
    @Consumes("application/json")
    public void updateAllowedOrigins(Set<String> allowedOrigins)
    {
        application.getApplicationUser().setWebOrigins(allowedOrigins);
    }

    @Path("allowed-origins")
    @DELETE
    @Consumes("application/json")
    public void deleteAllowedOrigins(Set<String> allowedOrigins)
    {
        for (String origin : allowedOrigins) {
            application.getApplicationUser().removeWebOrigin(origin);
        }
    }



}
