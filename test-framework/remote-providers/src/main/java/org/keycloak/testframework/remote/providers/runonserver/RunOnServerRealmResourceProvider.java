package org.keycloak.testframework.remote.providers.runonserver;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.util.JsonSerialization;

public class RunOnServerRealmResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public RunOnServerRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }

    @POST
    @Path("/")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String runOnServer(String runOnServer) throws Exception {
        try {
            Object r = SerializationUtil.decode(runOnServer, TestClassLoader.getInstance());

            if (r instanceof FetchOnServer) {
                Object result = ((FetchOnServer) r).run(session);
                return result != null ? JsonSerialization.writeValueAsString(result) : null;
            } else if (r instanceof RunOnServer) {
                ((RunOnServer) r).run(session);
                return null;
            } else {
                throw new IllegalArgumentException();
            }
        } catch (Throwable t) {
            return SerializationUtil.encodeException(t);
        }
    }
}
