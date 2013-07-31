package org.keycloak.services.managers;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.representations.idm.ResourceRepresentation;
import org.keycloak.representations.idm.RoleMappingRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.ResourceModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleAgent;
import org.picketlink.idm.model.SimpleRole;

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
    public static final String RESOURCE_ROLE = "KEYCLOAK_RESOURCE";
    public static final String IDENTITY_REQUESTER_ROLE = "KEYCLOAK_IDENTITY_REQUESTER";
    public static final String WILDCARD_ROLE = "*";

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
        idm.add(new SimpleRole(WILDCARD_ROLE));
        idm.add(new SimpleRole(RESOURCE_ROLE));
        idm.add(new SimpleRole(IDENTITY_REQUESTER_ROLE));
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

    public RealmModel importRealm(RealmRepresentation rep, UserModel realmCreator) {
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


        Map<String, UserModel> userMap = new HashMap<String, UserModel>();

        for (RequiredCredentialRepresentation requiredCred : rep.getRequiredCredentials()) {
            RequiredCredentialModel credential = new RequiredCredentialModel();
            credential.setType(requiredCred.getType());
            credential.setInput(requiredCred.isInput());
            credential.setSecret(requiredCred.isSecret());
            newRealm.addRequiredCredential(credential);
        }

        for (UserRepresentation userRep : rep.getUsers()) {
            UserModel user = newRealm.addUser(userRep.getUsername());
            user.setEnabled(userRep.isEnabled());
            if (userRep.getAttributes() != null) {
                for (Map.Entry<String, String> entry : userRep.getAttributes().entrySet()) {
                    user.setAttribute(entry.getKey(), entry.getValue());
                }
            }
            if (userRep.getCredentials() != null) {
                for (CredentialRepresentation cred : userRep.getCredentials()) {
                    UserCredentialModel credential = new UserCredentialModel();
                    credential.setType(cred.getType());
                    credential.setValue(cred.getValue());
                    newRealm.updateCredential(user, credential);
                }
            }
            userMap.put(user.getLoginName(), user);
        }

        if (rep.getRoles() != null) {
            for (RoleRepresentation roleRep : rep.getRoles()) {
                RoleModel role = newRealm.addRole(roleRep.getName());
                if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
            }
        }

        if (rep.getResources() != null) {
            createResources(rep, newRealm, userMap);
        }

        if (rep.getRoleMappings() != null) {
            for (RoleMappingRepresentation mapping : rep.getRoleMappings()) {
                UserModel user = userMap.get(mapping.getUsername());
                for (String roleString : mapping.getRoles()) {
                    RoleModel role = newRealm.getRole(roleString.trim());
                    if (role == null) {
                        role = newRealm.addRole(roleString.trim());
                    }
                    newRealm.grantRole(user, role);
                }
            }
        }

        if (rep.getScopeMappings() != null) {
            for (ScopeMappingRepresentation scope : rep.getScopeMappings()) {
                for (String roleString : scope.getRoles()) {
                    RoleModel role = newRealm.getRole(roleString.trim());
                    if (role == null) {
                        role = newRealm.addRole(roleString.trim());
                    }
                    UserModel user = userMap.get(scope.getUsername());
                    newRealm.addScope(user, role.getName());
                }

            }
        }
    }

    protected void createResources(RealmRepresentation rep, RealmModel realm, Map<String, UserModel> userMap) {
        RoleModel loginRole = realm.getRole(RealmManager.RESOURCE_ROLE);
        for (ResourceRepresentation resourceRep : rep.getResources()) {
            ResourceModel resource = realm.addResource(resourceRep.getName());
            resource.setManagementUrl(resourceRep.getAdminUrl());
            resource.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
            resource.updateResource();

            UserModel resourceUser = resource.getResourceUser();
            if (resourceRep.getCredentials() != null) {
                for (CredentialRepresentation cred : resourceRep.getCredentials()) {
                    UserCredentialModel credential = new UserCredentialModel();
                    credential.setType(cred.getType());
                    credential.setValue(cred.getValue());
                    realm.updateCredential(resourceUser, credential);
                }
            }
            userMap.put(resourceUser.getLoginName(), resourceUser);
            realm.grantRole(resourceUser, loginRole);


            if (resourceRep.getRoles() != null) {
                for (RoleRepresentation roleRep : resourceRep.getRoles()) {
                    RoleModel role = resource.addRole(roleRep.getName());
                    if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
                }
            }
            if (resourceRep.getRoleMappings() != null) {
                for (RoleMappingRepresentation mapping : resourceRep.getRoleMappings()) {
                    UserModel user = userMap.get(mapping.getUsername());
                    for (String roleString : mapping.getRoles()) {
                        RoleModel role = resource.getRole(roleString.trim());
                        if (role == null) {
                            role = resource.addRole(roleString.trim());
                        }
                        realm.grantRole(user, role);
                    }
                }
            }
            if (resourceRep.getScopeMappings() != null) {
                for (ScopeMappingRepresentation mapping : resourceRep.getScopeMappings()) {
                    UserModel user = userMap.get(mapping.getUsername());
                    for (String roleString : mapping.getRoles()) {
                        RoleModel role = resource.getRole(roleString.trim());
                        if (role == null) {
                            role = resource.addRole(roleString.trim());
                        }
                        resource.addScope(user, role.getName());
                    }
                }
            }
            if (resourceRep.isUseRealmMappings()) realm.addScope(resource.getResourceUser(), "*");
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
                    for (CredentialRepresentation cred : userRep.getCredentials()) {
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
