package org.keycloak.services.managers;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.representations.idm.ResourceRepresentation;
import org.keycloak.representations.idm.RoleMappingRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.ResourceModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.resources.RegistrationService;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.SimpleAgent;
import org.picketlink.idm.model.SimpleRole;
import org.picketlink.idm.model.SimpleUser;
import org.picketlink.idm.model.User;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per request object
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager {
    private static AtomicLong counter = new AtomicLong(1);

    public static String generateId() {
        return counter.getAndIncrement() + "-" + System.currentTimeMillis();
    }

    protected IdentitySession identitySession;

    public RealmManager(IdentitySession identitySession) {
        this.identitySession = identitySession;
    }

    public RealmModel defaultRealm() {
        return getRealm(Realm.DEFAULT_REALM);
    }

    public RealmModel getRealm(String id) {
        Realm existing = identitySession.findRealm(id);
        if (existing == null) {
            return null;
        }
        return new RealmModel(existing, identitySession);
    }

    public RealmModel createRealm(String name) {
        return createRealm(generateId(), name);
    }

    public RealmModel createRealm(String id, String name) {
        Realm newRealm = identitySession.createRealm(id);
        IdentityManager idm = identitySession.createIdentityManager(newRealm);
        SimpleAgent agent = new SimpleAgent(RealmModel.REALM_AGENT_ID);
        idm.add(agent);
        RealmModel realm = new RealmModel(newRealm, identitySession);
        return realm;
    }

    public void generateRealmKeys(RealmModel realm) {
        KeyPair keyPair = null;
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        realm.setPrivateKey(keyPair.getPrivate());
        realm.setPublicKey(keyPair.getPublic());
        realm.updateRealm();
    }

    public RealmModel importRealm(RealmRepresentation rep, User realmCreator) {
        verifyRealmRepresentation(rep);
        RealmModel realm = createRealm(rep.getRealm());
        importRealm(rep, realm);
        realm.addRealmAdmin(realmCreator);
        realm.updateRealm();
        return realm;
    }


    public void importRealm(RealmRepresentation rep, RealmModel newRealm) {
        newRealm.setName(rep.getRealm());
        newRealm.setEnabled(rep.isEnabled());
        newRealm.setTokenLifespan(rep.getTokenLifespan());
        newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        newRealm.setSslNotRequired(rep.isSslNotRequired());
        newRealm.setCookieLoginAllowed(rep.isCookieLoginAllowed());
        if (rep.getPrivateKey() == null || rep.getPublicKey() == null) {
           generateRealmKeys(newRealm);
        } else {
            newRealm.setPrivateKeyPem(rep.getPrivateKey());
            newRealm.setPublicKeyPem(rep.getPublicKey());
        }

        newRealm.updateRealm();


        Map<String, User> userMap = new HashMap<String, User>();

        for (RequiredCredentialRepresentation requiredCred : rep.getRequiredCredentials()) {
            RequiredCredentialModel credential = new RequiredCredentialModel();
            credential.setType(requiredCred.getType());
            credential.setInput(requiredCred.isInput());
            credential.setSecret(requiredCred.isSecret());
            newRealm.addRequiredCredential(credential);
        }

        for (UserRepresentation userRep : rep.getUsers()) {
            User user = new SimpleUser(userRep.getUsername());
            user.setEnabled(userRep.isEnabled());
            if (userRep.getAttributes() != null) {
                for (Map.Entry<String, String> entry : userRep.getAttributes().entrySet()) {
                    user.setAttribute(new Attribute<String>(entry.getKey(), entry.getValue()));
                }
            }
            newRealm.getIdm().add(user);
            if (userRep.getCredentials() != null) {
                for (UserRepresentation.Credential cred : userRep.getCredentials()) {
                    UserCredentialModel credential = new UserCredentialModel();
                    credential.setType(cred.getType());
                    credential.setValue(cred.getValue());
                    newRealm.updateCredential(user, credential);
                }
            }
            userMap.put(user.getLoginName(), user);
        }

        Map<String, Role> roles = new HashMap<String, Role>();

        if (rep.getRoles() != null) {
            for (String roleString : rep.getRoles()) {
                SimpleRole role = new SimpleRole(roleString.trim());
                newRealm.getIdm().add(role);
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
                        newRealm.getIdm().add(role);
                        roles.put(role.getName(), role);
                    }
                    newRealm.getIdm().grantRole(user, role);
                }
            }
        }

        if (rep.getScopeMappings() != null) {
            for (ScopeMappingRepresentation scope : rep.getScopeMappings()) {
                for (String roleString : scope.getRoles()) {
                    Role role = roles.get(roleString.trim());
                    if (role == null) {
                        role = new SimpleRole(roleString.trim());
                        newRealm.getIdm().add(role);
                        roles.put(role.getName(), role);
                    }
                    User user = userMap.get(scope.getUsername());
                    newRealm.addScope(user, role.getName());
                }

            }
        }

        if (!roles.containsKey("*")) {
            SimpleRole wildcard = new SimpleRole("*");
            newRealm.getIdm().add(wildcard);
            roles.put("*", wildcard);
        }

        if (rep.getResources() != null) {
            createResources(rep, newRealm, userMap);
        }
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
