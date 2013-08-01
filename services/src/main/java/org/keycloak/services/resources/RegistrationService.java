package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.UserCredentialModel;

import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/registrations")
public class RegistrationService {
    protected static final Logger logger = Logger.getLogger(RegistrationService.class);
    public static final String REALM_CREATOR_ROLE = "realm-creator";

    @Context
    protected UriInfo uriInfo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(final UserRepresentation newUser) {
        return new Transaction() {
            @Override
            protected Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel defaultRealm = realmManager.defaultRealm();
                if (!defaultRealm.isEnabled()) {
                    throw new ForbiddenException();
                }
                if (!defaultRealm.isRegistrationAllowed()) {
                    throw new ForbiddenException();
                }
                UserModel user = defaultRealm.getUser(newUser.getUsername());
                if (user != null) {
                    return Response.status(400).type("text/plain").entity("user exists").build();
                }

                user = defaultRealm.addUser(newUser.getUsername());
                for (CredentialRepresentation cred : newUser.getCredentials()) {
                    UserCredentialModel credModel = new UserCredentialModel();
                    credModel.setType(cred.getType());
                    credModel.setValue(cred.getValue());
                    defaultRealm.updateCredential(user, credModel);
                }
                RoleModel realmCreator = defaultRealm.getRole(REALM_CREATOR_ROLE);
                defaultRealm.grantRole(user, realmCreator);
                URI uri = uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(user.getLoginName()).build();
                return Response.created(uri).build();
            }
        }.call();
    }


}
