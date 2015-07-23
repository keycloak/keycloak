package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.ClientConnection;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.exportimport.ClientImporter;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.LDAPConnectionTestManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.timer.TimerProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

/**
 * Base resource class for the admin REST api of one realm
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AttackDetectionResource {
    protected static final Logger logger = Logger.getLogger(AttackDetectionResource.class);
    protected RealmAuth auth;
    protected RealmModel realm;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected ClientConnection connection;

    @Context
    protected HttpHeaders headers;

    @Context
    protected BruteForceProtector protector;

    public AttackDetectionResource(RealmAuth auth, RealmModel realm, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.realm(realm);

        auth.init(RealmAuth.Resource.REALM);
    }

    /**
     * Get status of a username in brute force detection
     *
     * @param username
     * @return
     */
    @GET
    @Path("brute-force/usernames/{username}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> bruteForceUserStatus(@PathParam("username") String username) {
        auth.hasView();
        Map<String, Object> data = new HashMap<>();
        data.put("disabled", false);
        data.put("numFailures", 0);
        data.put("lastFailure", 0);
        data.put("lastIPFailure", "n/a");
        if (!realm.isBruteForceProtected()) return data;

        UsernameLoginFailureModel model = session.sessions().getUserLoginFailure(realm, username);
        if (model == null) return data;
        if (protector.isTemporarilyDisabled(session, realm, username)) {
            data.put("disabled", true);
        }
        data.put("numFailures", model.getNumFailures());
        data.put("lastFailure", model.getLastFailure());
        data.put("lastIPFailure", model.getLastIPFailure());
        return data;
    }

    /**
     * Clear any user login failures for the user.  This can release temporary disabled user
     *
     * @param username
     */
    @Path("brute-force/usernames/{username}")
    @DELETE
    public void clearBruteForceForUser(@PathParam("username") String username) {
        auth.requireManage();
        UsernameLoginFailureModel model = session.sessions().getUserLoginFailure(realm, username);
        if (model != null) {
            session.sessions().removeUserLoginFailure(realm, username);
            adminEvent.operation(OperationType.DELETE).success();
        }
    }

    /**
     * Clear any user login failures for all users.  This can release temporary disabled users
     *
     */
    @Path("brute-force/usernames")
    @DELETE
    public void clearAllBruteForce() {
        auth.requireManage();
        session.sessions().removeAllUserLoginFailures(realm);
        adminEvent.operation(OperationType.DELETE).success();
    }


}
