package org.keycloak.protocol.ssf.endpoint.admin;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public class SsfReceiverAdminResource {

    protected static final Logger log = Logger.getLogger(SsfReceiverAdminResource.class);

    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;

    public SsfReceiverAdminResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
    }

    /**
     * http://localhost:8081/admin/realms/ssf-demo/ssf/receivers/{receiverAlias}/verify
     * @param alias
     * @return
     */
    @Path("/{receiverAlias}/verify")
    public SsfVerificationResource verificationEndpoint(@PathParam("receiverAlias") String alias) {
        return new SsfVerificationResource(session, alias);
    }
}
