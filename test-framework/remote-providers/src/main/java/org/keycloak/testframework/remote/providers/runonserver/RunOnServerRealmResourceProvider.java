package org.keycloak.testframework.remote.providers.runonserver;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.util.JsonSerialization;

public class RunOnServerRealmResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;
    private final RunOnServerRealmResourceProviderFactory factory;

    public RunOnServerRealmResourceProvider(KeycloakSession session, RunOnServerRealmResourceProviderFactory factory) {
        this.session = session;
        this.factory = factory;
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
    public String runOnServer(String runOnServer, @QueryParam("executionId") String executionId) {
        try {
            ClassLoader classLoader = factory.getTestClassLoader(executionId);
            Object o = SerializationUtil.decode(runOnServer, classLoader);
            if (o instanceof FetchOnServer f) {
                Object result = f.run(session);
                return result != null ? JsonSerialization.writeValueAsString(result) : null;
            } else if (o instanceof RunOnServer r) {
                r.run(session);
                return null;
            } else {
                throw new IllegalArgumentException("Can't handle serialized class: " + o.getClass().getName());
            }
        } catch (Throwable t) {
            return SerializationUtil.encodeException(t);
        }
    }

}
