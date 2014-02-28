package org.keycloak.services.resources;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.PublishedRealmRepresentation;
import org.keycloak.services.resources.admin.AdminService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PublicRealmResource {
    protected static final  Logger logger = Logger.getLogger(PublicRealmResource.class);

    @Context
    protected UriInfo uriInfo;

    protected RealmModel realm;

    public PublicRealmResource(RealmModel realm) {
        this.realm = realm;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public PublishedRealmRepresentation getRealm(@PathParam("realm") String id) {
        return realmRep(realm, uriInfo);
    }

    public static PublishedRealmRepresentation realmRep(RealmModel realm, UriInfo uriInfo) {
        PublishedRealmRepresentation rep = new PublishedRealmRepresentation();
        rep.setRealm(realm.getName());
        rep.setTokenServiceUrl(TokenService.tokenServiceBaseUrl(uriInfo).build(realm.getId()).toString());
        rep.setAccountServiceUrl(AccountService.accountServiceBaseUrl(uriInfo).build(realm.getId()).toString());
        rep.setAdminApiUrl(AdminService.adminApiUrl(uriInfo).build(realm.getId()).toString());
        rep.setPublicKeyPem(realm.getPublicKeyPem());
        return rep;
    }


}
