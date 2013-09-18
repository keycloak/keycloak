package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.*;
import org.keycloak.services.models.*;
import org.keycloak.services.models.UserModel.RequiredAction;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per request object
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager {
    protected static final Logger logger = Logger.getLogger(RealmManager.class);
    private static AtomicLong counter = new AtomicLong(1);
    public static final String RESOURCE_ROLE = "KEYCLOAK_RESOURCE";
    public static final String IDENTITY_REQUESTER_ROLE = "KEYCLOAK_IDENTITY_REQUESTER";
    public static final String WILDCARD_ROLE = "*";

    public static String generateId() {
        return counter.getAndIncrement() + "-" + System.currentTimeMillis();
    }

    protected KeycloakSession identitySession;

    public RealmManager(KeycloakSession identitySession) {
        this.identitySession = identitySession;
    }

    public RealmModel defaultRealm() {
        return getRealm(RealmModel.DEFAULT_REALM);
    }

    public RealmModel getRealm(String id) {
        return identitySession.getRealm(id);
    }

    public RealmModel createRealm(String name) {
        return createRealm(generateId(), name);
    }

    public RealmModel createRealm(String id, String name) {
        RealmModel realm = identitySession.createRealm(id, name);
        realm.setName(name);
        realm.addRole(WILDCARD_ROLE);
        realm.addRole(RESOURCE_ROLE);
        realm.addRole(IDENTITY_REQUESTER_ROLE);
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
    }

    public void updateRealm(RealmRepresentation rep, RealmModel realm) {
        if (rep.getRealm() != null) realm.setName(rep.getRealm());
        realm.setEnabled(rep.isEnabled());
        realm.setSocial(rep.isSocial());
        realm.setCookieLoginAllowed(rep.isCookieLoginAllowed());
        realm.setRegistrationAllowed(rep.isRegistrationAllowed());
        realm.setVerifyEmail(rep.isVerifyEmail());
        realm.setAutomaticRegistrationAfterSocialLogin(rep.isAutomaticRegistrationAfterSocialLogin());
        realm.setSslNotRequired((rep.isSslNotRequired()));
        realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        realm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        realm.setTokenLifespan(rep.getTokenLifespan());
        if (rep.getRequiredOAuthClientCredentials() != null) {
            realm.updateRequiredOAuthClientCredentials(rep.getRequiredOAuthClientCredentials());
        }
        if (rep.getRequiredCredentials() != null) {
            realm.updateRequiredCredentials(rep.getRequiredCredentials());
        }
        if (rep.getRequiredApplicationCredentials() != null) {
            realm.updateRequiredApplicationCredentials(rep.getRequiredApplicationCredentials());
        }
        if (rep.getDefaultRoles() != null) {
            realm.updateDefaultRoles(rep.getDefaultRoles());
        }
    }

    public RealmModel importRealm(RealmRepresentation rep, UserModel realmCreator) {
        //verifyRealmRepresentation(rep);
        RealmModel realm = createRealm(rep.getRealm());
        importRealm(rep, realm);
        realm.addRealmAdmin(realmCreator);
        return realm;
    }


    public void importRealm(RealmRepresentation rep, RealmModel newRealm) {
        newRealm.setName(rep.getRealm());
        newRealm.setEnabled(rep.isEnabled());
        newRealm.setSocial(rep.isSocial());
        newRealm.setTokenLifespan(rep.getTokenLifespan());
        newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        newRealm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        newRealm.setSslNotRequired(rep.isSslNotRequired());
        newRealm.setCookieLoginAllowed(rep.isCookieLoginAllowed());
        newRealm.setRegistrationAllowed(rep.isRegistrationAllowed());
        newRealm.setVerifyEmail(rep.isVerifyEmail());
        newRealm.setAutomaticRegistrationAfterSocialLogin(rep.isAutomaticRegistrationAfterSocialLogin());
        if (rep.getPrivateKey() == null || rep.getPublicKey() == null) {
            generateRealmKeys(newRealm);
        } else {
            newRealm.setPrivateKeyPem(rep.getPrivateKey());
            newRealm.setPublicKeyPem(rep.getPublicKey());
        }

        Map<String, UserModel> userMap = new HashMap<String, UserModel>();

        if (rep.getRequiredCredentials() != null) {
            for (String requiredCred : rep.getRequiredCredentials()) {
                addRequiredCredential(newRealm, requiredCred);
            }
        }

        if (rep.getRequiredApplicationCredentials() != null) {
            for (String requiredCred : rep.getRequiredApplicationCredentials()) {
                addResourceRequiredCredential(newRealm, requiredCred);
            }
        }

        if (rep.getRequiredOAuthClientCredentials() != null) {
            for (String requiredCred : rep.getRequiredOAuthClientCredentials()) {
                addOAuthClientRequiredCredential(newRealm, requiredCred);
            }
        }

        if (rep.getUsers() != null) {
            for (UserRepresentation userRep : rep.getUsers()) {
                UserModel user = createUser(newRealm, userRep);
                userMap.put(user.getLoginName(), user);
            }
        }

        if (rep.getRoles() != null) {
            for (RoleRepresentation roleRep : rep.getRoles()) {
                createRole(newRealm, roleRep);
            }
        }

        if (rep.getDefaultRoles() != null) {
            for (String roleString : rep.getDefaultRoles()) {
                newRealm.addDefaultRole(roleString.trim());
            }
        }

        if (rep.getApplications() != null) {
            createResources(rep, newRealm);
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

        if (rep.getSocialMappings() != null) {
            for (SocialMappingRepresentation socialMapping : rep.getSocialMappings()) {
                UserModel user = userMap.get(socialMapping.getUsername());
                for (SocialLinkRepresentation link : socialMapping.getSocialLinks()) {
                    SocialLinkModel mappingModel = new SocialLinkModel(link.getSocialProvider(), link.getSocialUsername());
                    newRealm.addSocialLink(user, mappingModel);
                }
            }
        }
    }

    public void createRole(RealmModel newRealm, RoleRepresentation roleRep) {
        RoleModel role = newRealm.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
    }

    public UserModel createUser(RealmModel newRealm, UserRepresentation userRep) {
        UserModel user = newRealm.addUser(userRep.getUsername());
        user.setStatus(UserModel.Status.valueOf(userRep.getStatus()));
        if (userRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : userRep.getAttributes().entrySet()) {
                user.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (String requiredAction : userRep.getRequiredActions()) {
                user.addRequiredAction(RequiredAction.valueOf(requiredAction));
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
        return user;
    }

    public void addRequiredCredential(RealmModel newRealm, String requiredCred) {
        newRealm.addRequiredCredential(requiredCred);
    }
    public void addResourceRequiredCredential(RealmModel newRealm, String requiredCred) {
        newRealm.addRequiredResourceCredential(requiredCred);
    }
    public void addOAuthClientRequiredCredential(RealmModel newRealm, String requiredCred) {
        newRealm.addRequiredOAuthClientCredential(requiredCred);
    }



     protected void createResources(RealmRepresentation rep, RealmModel realm) {
        RoleModel loginRole = realm.getRole(RealmManager.RESOURCE_ROLE);
        ResourceManager manager = new ResourceManager(this);
        for (ApplicationRepresentation resourceRep : rep.getApplications()) {
            manager.createResource(realm, loginRole, resourceRep);
        }
    }

    public RoleRepresentation toRepresentation(RoleModel role) {
        RoleRepresentation rep = new RoleRepresentation();
        rep.setId(role.getId());
        rep.setName(role.getName());
        rep.setDescription(role.getDescription());
        return rep;
    }

    public RealmRepresentation toRepresentation(RealmModel realm) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setId(realm.getId());
        rep.setRealm(realm.getName());
        rep.setEnabled(realm.isEnabled());
        rep.setSocial(realm.isSocial());
        rep.setAutomaticRegistrationAfterSocialLogin(realm.isAutomaticRegistrationAfterSocialLogin());
        rep.setSslNotRequired(realm.isSslNotRequired());
        rep.setCookieLoginAllowed(realm.isCookieLoginAllowed());
        rep.setPublicKey(realm.getPublicKeyPem());
        rep.setRegistrationAllowed(realm.isRegistrationAllowed());
        rep.setVerifyEmail(realm.isVerifyEmail());
        rep.setTokenLifespan(realm.getTokenLifespan());
        rep.setAccessCodeLifespan(realm.getAccessCodeLifespan());
        rep.setAccessCodeLifespanUserAction(realm.getAccessCodeLifespanUserAction());
        List<RequiredCredentialModel> requiredCredentialModels = realm.getRequiredCredentials();
        if (requiredCredentialModels.size() > 0) {
            rep.setRequiredCredentials(new HashSet<String>());
            for (RequiredCredentialModel cred : requiredCredentialModels) {
                rep.getRequiredCredentials().add(cred.getType());
            }
        }
        List<RequiredCredentialModel> requiredResourceCredentialModels = realm.getRequiredApplicationCredentials();
        if (requiredResourceCredentialModels.size() > 0) {
            rep.setRequiredApplicationCredentials(new HashSet<String>());
            for (RequiredCredentialModel cred : requiredResourceCredentialModels) {
                rep.getRequiredApplicationCredentials().add(cred.getType());
            }
        }
        List<RequiredCredentialModel> requiredOAuthCredentialModels = realm.getRequiredOAuthClientCredentials();
        if (requiredOAuthCredentialModels.size() > 0) {
            rep.setRequiredOAuthClientCredentials(new HashSet<String>());
            for (RequiredCredentialModel cred : requiredOAuthCredentialModels) {
                rep.getRequiredOAuthClientCredentials().add(cred.getType());
            }
        }
        return rep;
    }


}
