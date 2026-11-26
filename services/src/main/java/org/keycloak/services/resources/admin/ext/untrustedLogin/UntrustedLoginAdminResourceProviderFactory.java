package org.keycloak.services.resources.admin.ext.untrustedlogin;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.keycloak.models.untrustedlogin.UntrustedLoginRealmConfig;

/**
 * Exposes this feature's realm config as a proper typed Admin REST resource:
 *
 *   GET  /admin/realms/{realm}/untrusted-login-notifications/config
 *   PUT  /admin/realms/{realm}/untrusted-login-notifications/config
 *
 * This is what an extension module CAN do without touching Keycloak core - a real,
 * documented REST contract (UntrustedLoginConfigRepresentation) instead of admins editing
 * raw realm attributes through the generic realm PUT endpoint. What it can't do from here:
 * appear as a native toggle in Realm Settings in the Admin Console UI - that UI is a
 * React app living in Keycloak's own js/apps/admin-ui source tree, and wiring a new
 * settings panel into it requires a change to Keycloak core, not this module. A deployment
 * wanting a UI could call this endpoint from a small custom admin console extension (the
 * Admin Console does support UI extensions on newer versions) or just call it directly.
 *
 * Combines factory + provider in one class, per the same pattern used elsewhere for
 * admin realm resource providers (create() returns `this`, since there's no per-request
 * state to construct beyond what getResource() already receives).
 */
public class UntrustedLoginAdminResourceProviderFactory
        implements AdminRealmResourceProviderFactory, AdminRealmResourceProvider {

    private static final Logger log = Logger.getLogger(UntrustedLoginAdminResourceProviderFactory.class);

    // This ID becomes the first path segment under /admin/realms/{realm}/...
    public static final String ID = "untrusted-login-notifications";

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return ID;
    }

    /**
     * Called per-request. Returns the actual JAX-RS resource object handling this
     * request - here, a fresh RequestHandler carrying the realm/session/auth/adminEvent
     * that this particular request resolved to.
     */
    @Override
    public Object getResource(KeycloakSession session, RealmModel realm,
                               AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new RequestHandler(session, realm, auth);
    }

    /**
     * The actual JAX-RS resource. Kept as a separate inner class (rather than annotating
     * methods directly on the factory) so each request gets its own instance holding
     * request-scoped realm/auth state, instead of that state having to be threaded through
     * every method as parameters.
     */
    public static class RequestHandler {

        private final KeycloakSession session;
        private final RealmModel realm;
        private final AdminPermissionEvaluator auth;

        RequestHandler(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
            this.session = session;
            this.realm = realm;
            this.auth = auth;
        }

        @GET
        @Path("config")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getConfig() {
            // Read access: any admin permitted to view this realm at all. requireManageRealm()
            // reserved for the write path below, consistent with how RealmAdminResource itself
            // gates realm-settings reads vs. writes.
            auth.realm().requireViewRealm();
            return Response.ok(UntrustedLoginConfigRepresentation.fromRealm(realm)).build();
        }

        @PUT
        @Path("config")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateConfig(UntrustedLoginConfigRepresentation rep) {
            // SECURITY-SENSITIVE - VERIFY BEFORE MERGING: requireManageRealm() is the
            // conventional method name for "caller may modify realm settings" on
            // AdminPermissionEvaluator.realm() as of the fine-grained admin permissions
            // (fgap) package (org.keycloak.services.resources.admin.fgap), matching the
            // same gate RealmAdminResource uses for realm settings writes. Confirm the
            // exact method name/signature against the AdminPermissionEvaluator /
            // RealmPermissionEvaluator Javadoc for your target Keycloak version before
            // relying on this - getting an authorization check wrong is worse than getting
            // it slow, and this is exactly the kind of thing that needs a real test against
            // a running server, not just a read of the source.
            auth.realm().requireManageRealm();

            if (rep == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Request body required").build();
            }

            try {
                UntrustedLoginRealmConfig.setTrustWindowDays(realm, rep.getTrustWindowDays());
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build();
            }
            UntrustedLoginRealmConfig.setEnabled(realm, rep.isEnabled());

            log.infof("Updated untrusted-login-notifications config for realm %s: enabled=%s, trustWindowDays=%d",
                    realm.getName(), rep.isEnabled(), rep.getTrustWindowDays());

            return Response.ok(UntrustedLoginConfigRepresentation.fromRealm(realm)).build();
        }
    }
}