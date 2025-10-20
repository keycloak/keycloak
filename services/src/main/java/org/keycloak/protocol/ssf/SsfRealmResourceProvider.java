package org.keycloak.protocol.ssf;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ssf.event.delivery.push.PushEndpoint;
import org.keycloak.protocol.ssf.receiver.management.ReceiverManagementEndpoint;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
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
        authenticate();
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
    public ReceiverManagementEndpoint receiverManagementEndpoint() {
        // TODO check manage permissions
        authenticate();
        return SsfProvider.current().receiverManagementEndpoint();
    }


    @Override
    public void close() {
        // NOOP
    }

    //    @AutoService(RealmResourceProviderFactory.class)
    public static class Factory implements RealmResourceProviderFactory, EnvironmentDependentProviderFactory {

        private static final SsfRealmResourceProvider INSTANCE = new SsfRealmResourceProvider();

        /**
         * Exposes the SSF endpoints via $ISSUER/ssf
         *
         * @return
         */
        @Override
        public String getId() {
            return "ssf";
        }

        @Override
        public RealmResourceProvider create(KeycloakSession keycloakSession) {
            return INSTANCE;
        }

        @Override
        public void init(Config.Scope scope) {
        }

        @Override
        public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
            // NOOP
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isSupported(Config.Scope config) {
            return Profile.isFeatureEnabled(Profile.Feature.SSF);
        }
    }
}
