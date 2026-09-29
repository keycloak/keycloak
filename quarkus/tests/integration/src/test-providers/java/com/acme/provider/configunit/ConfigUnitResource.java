package com.acme.provider.configunit;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

import org.hibernate.SessionFactory;

/**
 * Exposes a named persistence unit ("default" for the default unit), so that a distribution test can assert what
 * reached it and that entities can be persisted through it.
 */
public class ConfigUnitResource implements RealmResourceProvider {

    private static final List<String> SETTINGS = List.of("hibernate.dialect", "hibernate.default_schema",
            "hibernate.use_sql_comments", "hibernate.log_slow_query", "hibernate.jdbc.batch_size", "hibernate.show_sql",
            "hibernate.format_sql", "hibernate.query.plan_cache_max_size");

    private final KeycloakSession session;

    public ConfigUnitResource(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Path("{unit}/settings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response settings(@PathParam("unit") String unit) {
        JpaConnectionProvider provider = provider(unit);
        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Map<String, Object> properties = provider.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getProperties();
        String json = SETTINGS.stream()
                .filter(properties::containsKey)
                .map(key -> "\"" + key + "\":\"" + properties.get(key) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("{unit}/entities/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response persist(@PathParam("unit") String unit, @PathParam("id") String id) {
        JpaConnectionProvider provider = provider(unit);
        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        ConfigUnitEntity entity = new ConfigUnitEntity();
        entity.setId(id);
        provider.getEntityManager().persist(entity);
        return Response.noContent().build();
    }

    @GET
    @Path("{unit}/entities")
    @Produces(MediaType.APPLICATION_JSON)
    public Response entities(@PathParam("unit") String unit) {
        JpaConnectionProvider provider = provider(unit);
        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        EntityManager em = provider.getEntityManager();
        List<String> ids = em.createQuery("select e.id from ConfigUnitEntity e order by e.id", String.class).getResultList();
        return Response.ok(ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]")), MediaType.APPLICATION_JSON).build();
    }

    private JpaConnectionProvider provider(String unit) {
        return "default".equals(unit) ? session.getProvider(JpaConnectionProvider.class) : session.getProvider(JpaConnectionProvider.class, unit);
    }

    @Override
    public void close() {
    }
}
