package org.keycloak.services.resources.account;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.managers.Auth;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Objects;
import java.util.stream.Stream;

public class GroupMembershipResource {

    private final KeycloakSession session;
    private final EventBuilder event;
    private final UserModel user;
    private final Auth auth;

    public GroupMembershipResource(KeycloakSession session,
                                   Auth auth,
                                   EventBuilder event,
                                   UserModel user) {
        this.session = session;
        this.auth = auth;
        this.event = event;
        this.user = user;
    }

    @GET
    @Path("/")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<GroupRepresentation> groupMembership(@QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        auth.require(AccountRoles.VIEW_GROUPS);

        return ModelToRepresentation.toGroupHierarchy(user, !briefRepresentation);

    }

}
