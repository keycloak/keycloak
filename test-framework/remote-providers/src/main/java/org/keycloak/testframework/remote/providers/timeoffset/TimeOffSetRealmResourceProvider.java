package org.keycloak.testframework.remote.providers.timeoffset;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class TimeOffSetRealmResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;
    private final String KEY_OFFSET = "offset";
    private final String CACHES = "caches";

    public TimeOffSetRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimeOffset() {
        int offset = Time.getOffset();
        var time = Map.of(KEY_OFFSET, offset);
        return Response.ok(time).build();
    }

    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setTimeOffset(Map<String, Object> time) {
        if (!time.containsKey(KEY_OFFSET)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        int timeOffset = (Integer) time.get(KEY_OFFSET);
        Time.setOffset(timeOffset);

        boolean caches = time.containsKey(CACHES) ? (Boolean) time.get(CACHES) : false;
        if (caches) {
            if (timeOffset > 0) {
                InfinispanTimeUtil.enableTestingTimeService(session);
            } else {
                InfinispanTimeUtil.disableTestingTimeService(session);
            }
        }

        return Response.ok().header("Content-Type", MediaType.APPLICATION_JSON).build();
    }
}
