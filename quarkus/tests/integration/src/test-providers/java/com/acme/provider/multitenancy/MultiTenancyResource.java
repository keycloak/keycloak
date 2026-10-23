package com.acme.provider.multitenancy;

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

public class MultiTenancyResource implements RealmResourceProvider {

    private static final Map<String, String> PERSISTENCE_UNITS = Map.of(
            "database", "database-store",
            "schema", "schema-store",
            "discriminator", "disc-store");

    private final KeycloakSession session;

    public MultiTenancyResource(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @POST
    @Path("{unit}/{tenant}/{id}")
    public Response persist(@PathParam("unit") String unit, @PathParam("tenant") String tenant, @PathParam("id") String id) {
        String persistenceUnit = PERSISTENCE_UNITS.get(unit);
        if (persistenceUnit == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        TenantContext.set(tenant);
        try {
            EntityManager em = session.getProvider(JpaConnectionProvider.class, persistenceUnit).getEntityManager();
            em.persist(newEntity(unit, id));
            return Response.noContent().build();
        } finally {
            TenantContext.clear();
        }
    }

    @GET
    @Path("{unit}/{tenant}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("unit") String unit, @PathParam("tenant") String tenant) {
        String persistenceUnit = PERSISTENCE_UNITS.get(unit);
        if (persistenceUnit == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        TenantContext.set(tenant);
        try {
            EntityManager em = session.getProvider(JpaConnectionProvider.class, persistenceUnit).getEntityManager();
            List<String> ids = em.createQuery("select e.id from " + entityName(unit) + " e order by e.id", String.class)
                    .getResultList();
            String json = ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } finally {
            TenantContext.clear();
        }
    }

    @GET
    @Path("schema/{tenant}/server-query")
    @Produces(MediaType.TEXT_PLAIN)
    public Response serverQueryAfterTenantSchemaSwitch(@PathParam("tenant") String tenant) {
        TenantContext.set(tenant);
        try {
            // switches the schema of a connection taken from the server datasource
            session.getProvider(JpaConnectionProvider.class, "schema-store").getEntityManager().createNativeQuery("SELECT 1").getSingleResult();
        } finally {
            TenantContext.clear();
        }
        return Response.ok(String.valueOf(session.users().getUsersCount(session.getContext().getRealm())), MediaType.TEXT_PLAIN).build();
    }

    private static Object newEntity(String unit, String id) {
        switch (unit) {
            case "database" -> {
                DatabaseTenantEntity entity = new DatabaseTenantEntity();
                entity.setId(id);
                return entity;
            }
            case "schema" -> {
                SchemaTenantEntity entity = new SchemaTenantEntity();
                entity.setId(id);
                return entity;
            }
            case "discriminator" -> {
                DiscriminatorTenantEntity entity = new DiscriminatorTenantEntity();
                entity.setId(id);
                return entity;
            }
            default -> throw new IllegalArgumentException("Unknown unit: " + unit);
        }
    }

    private static String entityName(String unit) {
        return switch (unit) {
            case "database" -> "DatabaseTenantEntity";
            case "schema" -> "SchemaTenantEntity";
            case "discriminator" -> "DiscriminatorTenantEntity";
            default -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };
    }

    @Override
    public void close() {
    }
}
