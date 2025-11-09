package org.keycloak.protocol.ssf.endpoint.admin;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * SsfReceiverAdminResource provides access to SSF Receiver operations. SSS
 */
public class SsfReceiverAdminResource {

    protected static final Logger log = Logger.getLogger(SsfReceiverAdminResource.class);

    protected final KeycloakSession session;
    protected final AdminPermissionEvaluator auth;

    public SsfReceiverAdminResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
    }

    /**
     * Exposes the {@link SsfVerificationResource} to verify the stream and event delivery setup for a SSF Receiver as a custom endpoint.
     *
     * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/receivers/{receiverAlias}/verify}
     * @param alias
     * @return
     */
    @Path("/{receiverAlias}/verify")
    public SsfVerificationResource verificationEndpoint(@PathParam("receiverAlias") String alias) {
        return new SsfVerificationResource(session, alias);
    }
}
