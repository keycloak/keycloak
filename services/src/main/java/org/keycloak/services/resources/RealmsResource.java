package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.representations.idm.ResourceRepresentation;
import org.keycloak.representations.idm.RoleMappingRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.models.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.ResourceModel;
import org.keycloak.services.models.UserCredentialModel;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.SimpleRole;
import org.picketlink.idm.model.SimpleUser;
import org.picketlink.idm.model.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    protected
    IdentitySession IdentitySession;

    @Context
    ResourceContext resourceContext;

    protected Map<String, AccessCodeEntry> accessCodes = new ConcurrentHashMap<String, AccessCodeEntry>();

    @Path("{realm}/tokens")
    public TokenService getTokenService(@PathParam("realm") String id) {
        RealmManager realmManager = new RealmManager(IdentitySession);
        RealmModel realm = realmManager.getRealm(id);
        if (realm == null) {
            logger.debug("realm not found");
            throw new NotFoundException();
        }
        TokenService tokenService = new TokenService(realm, accessCodes);
        resourceContext.initResource(tokenService);
        return tokenService;

    }


    @Path("{realm}")
    public RealmSubResource getRealmResource(@PathParam("realm") String id) {
        RealmManager realmManager = new RealmManager(IdentitySession);
        RealmModel realm = realmManager.getRealm(id);
        if (realm == null) {
            logger.debug("realm not found");
            throw new NotFoundException();
        }
        RealmSubResource realmResource = new RealmSubResource(realm);
        resourceContext.initResource(realmResource);
        return realmResource;

    }


    @POST
    @Consumes("application/json")
    public Response importRealm(RealmRepresentation rep) {
        IdentitySession.getTransaction().begin();
        RealmModel realm;
        try {
            realm = createRealm(rep);
            IdentitySession.getTransaction().commit();
        } catch (RuntimeException re) {
            IdentitySession.getTransaction().rollback();
            throw re;
        }
        UriBuilder builder = uriInfo.getRequestUriBuilder().path(realm.getId());
        return Response.created(builder.build())
                .entity(RealmSubResource.realmRep(realm, uriInfo))
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected RealmModel createRealm(RealmRepresentation rep) {
        RealmManager realmManager = new RealmManager(IdentitySession);
        RealmModel defaultRealm = realmManager.getRealm(Realm.DEFAULT_REALM);
        User realmCreator = new AuthenticationManager().authenticateToken(defaultRealm, headers);
        Role creatorRole = defaultRealm.getIdm().getRole(RegistrationService.REALM_CREATOR_ROLE);
        if (!defaultRealm.getIdm().hasRole(realmCreator, creatorRole)) {
            logger.warn("not a realm creator");
            throw new NotAuthorizedException("Bearer");
        }
        verifyRealmRepresentation(rep);

        RealmModel realm = realmManager.createRealm(rep.getRealm());
        realmManager.generateRealmKeys(realm);
        realm.addRealmAdmin(realmCreator);
        realm.setName(rep.getRealm());
        realm.setEnabled(rep.isEnabled());
        realm.setTokenLifespan(rep.getTokenLifespan());
        realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        realm.setSslNotRequired(rep.isSslNotRequired());
        realm.setCookieLoginAllowed(rep.isCookieLoginAllowed());
        realm.updateRealm();


        Map<String, User> userMap = new HashMap<String, User>();

        for (RequiredCredentialRepresentation requiredCred : rep.getRequiredCredentials()) {
            RequiredCredentialModel credential = new RequiredCredentialModel();
            credential.setType(requiredCred.getType());
            credential.setInput(requiredCred.isInput());
            credential.setSecret(requiredCred.isSecret());
            realm.addRequiredCredential(credential);
        }

        for (UserRepresentation userRep : rep.getUsers()) {
            User user = new SimpleUser(userRep.getUsername());
            user.setEnabled(userRep.isEnabled());
            if (userRep.getAttributes() != null) {
                for (Map.Entry<String, String> entry : userRep.getAttributes().entrySet()) {
                    user.setAttribute(new Attribute<String>(entry.getKey(), entry.getValue()));
                }
            }
            realm.getIdm().add(user);
            if (userRep.getCredentials() != null) {
                for (UserRepresentation.Credential cred : userRep.getCredentials()) {
                    UserCredentialModel credential = new UserCredentialModel();
                    credential.setType(cred.getType());
                    credential.setValue(cred.getValue());
                    realm.updateCredential(user, credential);
                }
            }
            userMap.put(user.getLoginName(), user);
        }

        Map<String, Role> roles = new HashMap<String, Role>();

        if (rep.getRoles() != null) {
            for (String roleString : rep.getRoles()) {
                SimpleRole role = new SimpleRole(roleString.trim());
                realm.getIdm().add(role);
                roles.put(role.getName(), role);
            }
        }

        if (rep.getRoleMappings() != null) {
            for (RoleMappingRepresentation mapping : rep.getRoleMappings()) {
                User user = userMap.get(mapping.getUsername());
                for (String roleString : mapping.getRoles()) {
                    Role role = roles.get(roleString.trim());
                    if (role == null) {
                        role = new SimpleRole(roleString.trim());
                        realm.getIdm().add(role);
                        roles.put(role.getName(), role);
                    }
                    realm.getIdm().grantRole(user, role);
                }
            }
        }

        if (rep.getScopeMappings() != null) {
            for (ScopeMappingRepresentation scope : rep.getScopeMappings()) {
                for (String roleString : scope.getRoles()) {
                    Role role = roles.get(roleString.trim());
                    if (role == null) {
                        role = new SimpleRole(roleString.trim());
                        realm.getIdm().add(role);
                        roles.put(role.getName(), role);
                    }
                    User user = userMap.get(scope.getUsername());
                    realm.addScope(user, role.getName());
                }

            }
        }

        if (!roles.containsKey("*")) {
            SimpleRole wildcard = new SimpleRole("*");
            realm.getIdm().add(wildcard);
            roles.put("*", wildcard);
        }

        if (rep.getResources() != null) {
            createResources(rep, realm, userMap);
        }
        return realm;
    }

    protected void createResources(RealmRepresentation rep, RealmModel realm, Map<String, User> userMap) {
        for (ResourceRepresentation resourceRep : rep.getResources()) {
            ResourceModel resource = realm.addResource(resourceRep.getName());
            resource.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
            resource.updateResource();
            Map<String, Role> roles = new HashMap<String, Role>();
            if (resourceRep.getRoles() != null) {
                for (String roleString : resourceRep.getRoles()) {
                    SimpleRole role = new SimpleRole(roleString.trim());
                    resource.getIdm().add(role);
                    roles.put(role.getName(), role);
                }
            }
            if (resourceRep.getRoleMappings() != null) {
                for (RoleMappingRepresentation mapping : resourceRep.getRoleMappings()) {
                    User user = userMap.get(mapping.getUsername());
                    for (String roleString : mapping.getRoles()) {
                        Role role = roles.get(roleString.trim());
                        if (role == null) {
                            role = new SimpleRole(roleString.trim());
                            resource.getIdm().add(role);
                            roles.put(role.getName(), role);
                        }
                        Role role1 = resource.getIdm().getRole(role.getName());
                        realm.getIdm().grantRole(user, role1);
                    }
                }
            }
            if (resourceRep.getScopeMappings() != null) {
                for (ScopeMappingRepresentation mapping : resourceRep.getScopeMappings()) {
                    User user = userMap.get(mapping.getUsername());
                    for (String roleString : mapping.getRoles()) {
                        Role role = roles.get(roleString.trim());
                        if (role == null) {
                            role = new SimpleRole(roleString.trim());
                            resource.getIdm().add(role);
                            roles.put(role.getName(), role);
                        }
                        resource.addScope(user, role.getName());
                    }
                }
            }
            if (!roles.containsKey("*")) {
                SimpleRole wildcard = new SimpleRole("*");
                resource.getIdm().add(wildcard);
                roles.put("*", wildcard);
            }

        }
    }

    protected void verifyRealmRepresentation(RealmRepresentation rep) {
        if (rep.getRequiredCredentials() == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Realm credential requirements not defined").type("text/plain").build());

        }

        HashMap<String, UserRepresentation> userReps = new HashMap<String, UserRepresentation>();
        for (UserRepresentation userRep : rep.getUsers()) userReps.put(userRep.getUsername(), userRep);

        // override enabled to false if user does not have at least all of browser or client credentials
        for (UserRepresentation userRep : rep.getUsers()) {
            if (userRep.getCredentials() == null) {
                userRep.setEnabled(false);
            } else {
                boolean hasBrowserCredentials = true;
                for (RequiredCredentialRepresentation credential : rep.getRequiredCredentials()) {
                    boolean hasCredential = false;
                    for (UserRepresentation.Credential cred : userRep.getCredentials()) {
                        if (cred.getType().equals(credential.getType())) {
                            hasCredential = true;
                            break;
                        }
                    }
                    if (!hasCredential) {
                        hasBrowserCredentials = false;
                        break;
                    }
                }
                if (!hasBrowserCredentials) {
                    userRep.setEnabled(false);
                }

            }
        }

        if (rep.getResources() != null) {
            // check mappings
            for (ResourceRepresentation resourceRep : rep.getResources()) {
                if (resourceRep.getRoleMappings() != null) {
                    for (RoleMappingRepresentation mapping : resourceRep.getRoleMappings()) {
                        if (!userReps.containsKey(mapping.getUsername())) {
                            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                    .entity("No users declared for role mapping").type("text/plain").build());

                        }
                    }
                }
            }
        }
    }

}
