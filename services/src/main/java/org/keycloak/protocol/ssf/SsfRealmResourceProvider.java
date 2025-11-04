package org.keycloak.protocol.ssf;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.protocol.ssf.event.delivery.push.PushEndpoint;
import org.keycloak.protocol.ssf.receiver.management.SsfReceiverManagementEndpoint;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.utils.KeycloakSessionUtil;

public class SsfRealmResourceProvider implements RealmResourceProvider {

    protected static final Logger log = Logger.getLogger(SsfRealmResourceProvider.class);

    @Override
    public Object getResource() {
        return this;
    }

    protected AuthenticationManager.AuthResult authenticate() {
        var session = KeycloakSessionUtil.getKeycloakSession();
        var authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        var auth = authenticator.authenticate();
        if (auth == null) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        return auth;
    }

    // Receiver Endpoints below

    /**
     * $ISSUER/ssf/push/caepdev
     * <p>
     * For example: https://tdworkshops.ngrok.dev/auth/realms/ssf-demo/ssf/push/caepdev
     *
     * @return
     */
    @Path("/push")
    public PushEndpoint pushEndpoint() {
        // push endpoint authentication checked by PushEndpoit directly.
        return SsfProvider.current().pushEndpoint();
    }

    // Receiver Management Endpoints below

    /**
     * $ISSUER/ssf/management
     * <p>
     * For example: https://tdworkshops.ngrok.dev/auth/realms/ssf-demo/ssf/management
     *
     * @return
     */
    @Path("/management")
    public SsfReceiverManagementEndpoint receiverManagementEndpoint() {

        var auth = authenticate();

        // TODO define proper permission check
        // checkManageReceiversPermission(auth);

        return SsfProvider.current().receiverManagementEndpoint();
    }

    protected void checkManageReceiversPermission(AuthenticationManager.AuthResult auth) {
        AdminAuth adminAuth = new AdminAuth(auth.session().getRealm(), auth.token(), auth.user(), auth.client());
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(KeycloakSessionUtil.getKeycloakSession(), adminAuth.getRealm(), adminAuth);

        realmAuth.clients().requireManage();
    }


    @Override
    public void close() {
        // NOOP
    }

}
