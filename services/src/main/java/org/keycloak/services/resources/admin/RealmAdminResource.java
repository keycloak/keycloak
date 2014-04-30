package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.Event;
import org.keycloak.audit.EventQuery;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.idm.RealmAuditRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmAuth auth;
    protected RealmModel realm;
    private TokenManager tokenManager;

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    @Context
    protected KeycloakSession session;

    @Context
    protected ProviderSession providers;

    public RealmAdminResource(RealmAuth auth, RealmModel realm, TokenManager tokenManager) {
        this.auth = auth;
        this.realm = realm;
        this.tokenManager = tokenManager;

        auth.init(RealmAuth.Resource.REALM);
    }

    @Path("applications")
    public ApplicationsResource getApplications() {
        ApplicationsResource applicationsResource = new ApplicationsResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(applicationsResource);
        //resourceContext.initResource(applicationsResource);
        return applicationsResource;
    }

    @Path("oauth-clients")
    public OAuthClientsResource getOAuthClients() {
        OAuthClientsResource oauth = new OAuthClientsResource(realm, auth, session);
        ResteasyProviderFactory.getInstance().injectProperties(oauth);
        //resourceContext.initResource(oauth);
        return oauth;
    }

    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(realm, auth, realm);
    }

    @GET
    @NoCache
    @Produces("application/json")
    public RealmRepresentation getRealm() {
        if (auth.hasView()) {
            return ModelToRepresentation.toRepresentation(realm);
        } else {
            auth.requireAny();

            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());

            return rep;
        }
    }

    @PUT
    @Consumes("application/json")
    public Response updateRealm(final RealmRepresentation rep) {
        auth.requireManage();

        logger.debug("updating realm: " + realm.getName());
        try {
            new RealmManager(session).updateRealm(rep, realm);
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Realm " + rep.getRealm() + " already exists");
        }
    }

    @DELETE
    public void deleteRealm() {
        auth.requireManage();

        if (!new RealmManager(session).removeRealm(realm)) {
            throw new NotFoundException("Realm doesn't exist");
        }
    }

    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(realm, auth, tokenManager);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        //resourceContext.initResource(users);
        return users;
    }

    @Path("roles-by-id")
    public RoleByIdResource rolesById() {
        RoleByIdResource resource = new RoleByIdResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;
    }

    @Path("push-revocation")
    @POST
    public void pushRevocation() {
        auth.requireManage();
        new ResourceAdminManager().pushRealmRevocationPolicy(realm);
    }

    @Path("logout-all")
    @POST
    public void logoutAll() {
        auth.requireManage();
        new ResourceAdminManager().logoutAll(realm);
    }

    @Path("session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, SessionStats> getSessionStats() {
        logger.info("session-stats");
        auth.requireView();
        Map<String, SessionStats> stats = new HashMap<String, SessionStats>();
        for (ApplicationModel applicationModel : realm.getApplications()) {
            if (applicationModel.getManagementUrl() == null) continue;
            SessionStats appStats = new ResourceAdminManager().getSessionStats(realm, applicationModel, false);
            stats.put(applicationModel.getName(), appStats);
        }
        return stats;
    }

    @GET
    @Path("audit")
    @Produces("application/json")
    public RealmAuditRepresentation getRealmAudit() {
        auth.init(RealmAuth.Resource.AUDIT).requireView();

        return ModelToRepresentation.toAuditReprensetation(realm);
    }

    @PUT
    @Path("audit")
    @Consumes("application/json")
    public void updateRealmAudit(final RealmAuditRepresentation rep) {
        auth.init(RealmAuth.Resource.AUDIT).requireManage();

        logger.debug("updating realm audit: " + realm.getName());
        new RealmManager(session).updateRealmAudit(rep, realm);
    }

    @Path("audit/events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getAudit(@QueryParam("client") String client, @QueryParam("event") String event, @QueryParam("user") String user,
                                @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults) {
        auth.init(RealmAuth.Resource.AUDIT).requireView();

        AuditProvider audit = providers.getProvider(AuditProvider.class);

        EventQuery query = audit.createQuery().realm(realm.getId());
        if (client != null) {
            query.client(client);
        }
        if (event != null) {
            query.event(event);
        }
        if (user != null) {
            query.user(user);
        }
        if (ipAddress != null) {
            query.ipAddress(ipAddress);
        }
        if (firstResult != null) {
            query.firstResult(firstResult);
        }
        if (maxResults != null) {
            query.maxResults(maxResults);
        }

        return query.getResultList();
    }

    @Path("audit/events")
    @DELETE
    public void clearAudit() {
        auth.init(RealmAuth.Resource.AUDIT).requireManage();

        AuditProvider audit = providers.getProvider(AuditProvider.class);
        audit.clear(realm.getId());
    }
}
