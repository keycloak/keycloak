package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.ClientConnection;
import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.OpenIDConnectService;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.EventsManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.util.StreamUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

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

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    @Context
    protected KeycloakSession session;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected BruteForceProtector protector;

    public static UriBuilder realmBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getRealmResource");
    }

    public static UriBuilder realmBaseUrl(UriBuilder base) {
        return base.path(RealmsResource.class).path(RealmsResource.class, "getRealmResource");
    }

    public static UriBuilder accountUrl(UriBuilder base) {
        return base.path(RealmsResource.class).path(RealmsResource.class, "getAccountService");
    }

    /**
     *
     *
     * @param name
     * @param client_id
     * @return
     */
    @Path("{realm}/login-status-iframe.html")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getLoginStatusIframe(final @PathParam("realm") String name,
                                       @QueryParam("client_id") String client_id,
                                       @QueryParam("origin") String origin) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);
        ClientModel client = realm.findClient(client_id);
        if (client == null) {
            throw new NotFoundException("could not find client: " + client_id);
        }

        InputStream is = getClass().getClassLoader().getResourceAsStream("login-status-iframe.html");
        if (is == null) throw new NotFoundException("Could not find login-status-iframe.html ");

        boolean valid = false;
        for (String o : client.getWebOrigins()) {
            if (o.equals("*") || o.equals(origin)) {
                valid = true;
                break;
            }
        }

        for (String r : OpenIDConnectService.resolveValidRedirects(uriInfo, client.getRedirectUris())) {
            int i = r.indexOf('/', 8);
            if (i != -1) {
                r = r.substring(0, i);
            }

            if (r.equals(origin)) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            throw new BadRequestException("Invalid origin");
        }

        try {
            String file = StreamUtil.readString(is);
            file = file.replace("ORIGIN", origin);

            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoTransform(false);
            cacheControl.setMaxAge(Config.scope("theme").getInt("staticMaxAge", -1));

            return Response.ok(file).cacheControl(cacheControl).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Path("{realm}/protocol/{protocol}")
    public Object getProtocol(final @PathParam("realm") String name,
                                            final @PathParam("protocol") String protocol) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);
        EventBuilder event = new EventsManager(realm, session, clientConnection).createEventBuilder();
        AuthenticationManager authManager = new AuthenticationManager(protector);

        LoginProtocolFactory factory = (LoginProtocolFactory)session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, protocol);
        Object endpoint = factory.createProtocolEndpoint(realm, event, authManager);

        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        //resourceContext.initResource(tokenService);
        return endpoint;
    }

    @Path("{realm}/tokens")
    @Deprecated
    public Object getTokenService(final @PathParam("realm") String name) {
        // for backward compatibility.
        return getProtocol(name, "openid-connect");
    }

    @Path("{realm}/login-actions")
    public LoginActionsService getLoginActionsService(final @PathParam("realm") String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);
        EventBuilder event = new EventsManager(realm, session, clientConnection).createEventBuilder();
        AuthenticationManager authManager = new AuthenticationManager(protector);
        LoginActionsService service = new LoginActionsService(realm, authManager, event);
        ResteasyProviderFactory.getInstance().injectProperties(service);

        //resourceContext.initResource(service);
        return service;
    }


    protected RealmModel locateRealm(String name, RealmManager realmManager) {
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm " + name + " does not exist");
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
            throw new NotFoundException("account management not enabled");
        }

        EventBuilder event = new EventsManager(realm, session, clientConnection).createEventBuilder();
        AccountService accountService = new AccountService(realm, application, event);
        ResteasyProviderFactory.getInstance().injectProperties(accountService);
        //resourceContext.initResource(accountService);
        accountService.init();
        return accountService;
    }

    @Path("{realm}")
    public PublicRealmResource getRealmResource(final @PathParam("realm") String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = locateRealm(name, realmManager);
        PublicRealmResource realmResource = new PublicRealmResource(realm);
        ResteasyProviderFactory.getInstance().injectProperties(realmResource);
        //resourceContext.initResource(realmResource);
        return realmResource;
    }




}
