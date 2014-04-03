package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.audit.Audit;
import org.keycloak.audit.AuditListener;
import org.keycloak.audit.AuditProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ClientConnection;
import org.keycloak.services.ProviderSession;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.services.managers.TokenManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/realms")
public class RealmsResource {
    protected static Logger logger = Logger.getLogger(RealmsResource.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpHeaders headers;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    @Context
    protected ProviderSession providers;

    @Context
    protected ClientConnection clientConnection;

    protected TokenManager tokenManager;
    protected SocialRequestManager socialRequestManager;

    public RealmsResource(TokenManager tokenManager, SocialRequestManager socialRequestManager) {
        this.tokenManager = tokenManager;
        this.socialRequestManager = socialRequestManager;
    }

    public static UriBuilder realmBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getRealmResource");
    }

    @Path("{realm}/tokens")
    public TokenService getTokenService(final @PathParam("realm") String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);
        Audit audit = createAudit(realm);
        TokenService tokenService = new TokenService(realm, tokenManager, audit);
        resourceContext.initResource(tokenService);
        return tokenService;
    }

    protected RealmModel locateRealm(String name, RealmManager realmManager) {
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm " + name + " not found");
        }
        return realm;
    }

    @Path("{realm}/account")
    public AccountService getAccountService(final @PathParam("realm") String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);

        ApplicationModel application = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APP);
        if (application == null || !application.isEnabled()) {
            logger.debug("account management not enabled");
            throw new NotFoundException();
        }

        Audit audit = createAudit(realm);
        AccountService accountService = new AccountService(realm, application, tokenManager, socialRequestManager, audit);
        resourceContext.initResource(accountService);
        accountService.init();
        return accountService;
    }

    @Path("{realm}")
    public PublicRealmResource getRealmResource(final @PathParam("realm") String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);
        PublicRealmResource realmResource = new PublicRealmResource(realm);
        resourceContext.initResource(realmResource);
        return realmResource;
    }

    private Audit createAudit(RealmModel realm) {
        List<AuditListener> listeners = new LinkedList<AuditListener>();

        AuditProvider auditProvider = providers.getProvider(AuditProvider.class);
        if (auditProvider != null) {
            listeners.add(auditProvider);
        }

        if (realm.getAuditListeners() != null) {
            for (String id : realm.getAuditListeners()) {
                AuditListener listener = providers.getProvider(AuditListener.class, id);
                if (listener != null) {
                    listeners.add(listener);
                }
            }
        }

        return new Audit(listeners, realm, clientConnection.getRemoteAddr());
    }

}
