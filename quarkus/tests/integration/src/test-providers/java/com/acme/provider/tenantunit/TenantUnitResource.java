package com.acme.provider.tenantunit;

import java.util.List;
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

/**
 * Persists and lists {@link TenantUnitEntity} through the persistence unit of the {@code tenant-store} datasource for
 * the tenant given in the path.
 */
public class TenantUnitResource implements RealmResourceProvider {

    private final KeycloakSession session;

    public TenantUnitResource(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @POST
    @Path("{tenant}/{id}")
    public Response persist(@PathParam("tenant") String tenant, @PathParam("id") String id) {
        TenantUnitContext.set(tenant);
        try {
            TenantUnitEntity entity = new TenantUnitEntity();
            entity.setId(id);
            entityManager().persist(entity);
            return Response.noContent().build();
        } finally {
            TenantUnitContext.clear();
        }
    }

    @GET
    @Path("{tenant}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("tenant") String tenant) {
        TenantUnitContext.set(tenant);
        try {
            List<String> ids = entityManager().createQuery("select e.id from TenantUnitEntity e order by e.id", String.class).getResultList();
            return Response.ok(ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]")), MediaType.APPLICATION_JSON).build();
        } finally {
            TenantUnitContext.clear();
        }
    }

    private EntityManager entityManager() {
        return session.getProvider(JpaConnectionProvider.class, TenantUnitTenantResolver.UNIT).getEntityManager();
    }

    @Override
    public void close() {
    }
}
