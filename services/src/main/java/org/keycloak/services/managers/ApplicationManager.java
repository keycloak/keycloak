package org.keycloak.services.managers;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.adapters.config.BaseRealmConfig;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.UserRoleMappingRepresentation;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationManager {
    protected Logger logger = Logger.getLogger(ApplicationManager.class);

    protected RealmManager realmManager;

    public ApplicationManager(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    public ApplicationManager() {
    }


    /**
     * Does not create scope or role mappings!
     *
     * @param realm
     * @param loginRole
     * @param resourceRep
     * @return
     */
    public ApplicationModel createApplication(RealmModel realm, RoleModel loginRole, ApplicationRepresentation resourceRep) {
        logger.debug("************ CREATE APPLICATION: {0}" + resourceRep.getName());
        ApplicationModel applicationModel = realm.addApplication(resourceRep.getName());
        applicationModel.setEnabled(resourceRep.isEnabled());
        applicationModel.setManagementUrl(resourceRep.getAdminUrl());
        applicationModel.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
        applicationModel.setBaseUrl(resourceRep.getBaseUrl());
        applicationModel.updateApplication();

        UserModel resourceUser = applicationModel.getAgent();
        if (resourceRep.getCredentials() != null && resourceRep.getCredentials().size() > 0) {
            for (CredentialRepresentation cred : resourceRep.getCredentials()) {
                UserCredentialModel credential = new UserCredentialModel();
                credential.setType(cred.getType());
                credential.setValue(cred.getValue());
                realm.updateCredential(resourceUser, credential);
            }
        } else {
            generateSecret(realm, applicationModel);
        }


        if (resourceRep.getRedirectUris() != null) {
            for (String redirectUri : resourceRep.getRedirectUris()) {
                resourceUser.addRedirectUri(redirectUri);
            }
        }
        if (resourceRep.getWebOrigins() != null) {
            for (String webOrigin : resourceRep.getWebOrigins()) {
                logger.debug("Application: {0} webOrigin: {1}", resourceUser.getLoginName(), webOrigin);
                resourceUser.addWebOrigin(webOrigin);
            }
        }

        realm.grantRole(resourceUser, loginRole);


        if (resourceRep.getDefaultRoles() != null) {
            applicationModel.updateDefaultRoles(resourceRep.getDefaultRoles());
        }

        if (resourceRep.getClaims() != null) {
            ClaimManager.setClaims(applicationModel, resourceRep.getClaims());
        } else {
            applicationModel.setAllowedClaimsMask(ClaimMask.USERNAME);
        }

        return applicationModel;
    }

    public void createRoleMappings(RealmModel realm, ApplicationModel applicationModel, List<UserRoleMappingRepresentation> mappings) {
        for (UserRoleMappingRepresentation mapping : mappings) {
            UserModel user = realm.getUser(mapping.getUsername());
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            for (String roleString : mapping.getRoles()) {
                RoleModel role = applicationModel.getRole(roleString.trim());
                if (role == null) {
                    role = applicationModel.addRole(roleString.trim());
                }
                realm.grantRole(user, role);
            }
        }
    }

    public void createScopeMappings(RealmModel realm, ApplicationModel applicationModel, List<ScopeMappingRepresentation> mappings) {
        for (ScopeMappingRepresentation mapping : mappings) {
            UserModel user = realm.getUser(mapping.getUsername());
            for (String roleString : mapping.getRoles()) {
                RoleModel role = applicationModel.getRole(roleString.trim());
                if (role == null) {
                    role = applicationModel.addRole(roleString.trim());
                }
                realm.addScopeMapping(user, role);
            }
        }
    }

    public ApplicationModel createApplication(RealmModel realm, ApplicationRepresentation resourceRep) {
        RoleModel loginRole = realm.getRole(Constants.APPLICATION_ROLE);
        return createApplication(realm, loginRole, resourceRep);
    }

    public ApplicationModel createApplication(RealmModel realm, String name) {
        RoleModel loginRole = realm.getRole(Constants.APPLICATION_ROLE);
        ApplicationModel app = realm.addApplication(name);
        realm.grantRole(app.getAgent(), loginRole);
        generateSecret(realm, app);

        return app;
    }

    public UserCredentialModel generateSecret(RealmModel realm, ApplicationModel app) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        realm.updateCredential(app.getAgent(), secret);
        return secret;
    }

    public void updateApplication(ApplicationRepresentation rep, ApplicationModel resource) {
        resource.setName(rep.getName());
        resource.setEnabled(rep.isEnabled());
        resource.setManagementUrl(rep.getAdminUrl());
        resource.setBaseUrl(rep.getBaseUrl());
        resource.setSurrogateAuthRequired(rep.isSurrogateAuthRequired());
        resource.updateApplication();

        if (rep.getDefaultRoles() != null) {
            resource.updateDefaultRoles(rep.getDefaultRoles());
        }

        List<String> redirectUris = rep.getRedirectUris();
        if (redirectUris != null) {
            resource.getAgent().setRedirectUris(new HashSet<String>(redirectUris));
        }

        List<String> webOrigins = rep.getWebOrigins();
        if (webOrigins != null) {
            resource.getAgent().setWebOrigins(new HashSet<String>(webOrigins));
        }

        if (rep.getClaims() != null) {
            ClaimManager.setClaims(resource, rep.getClaims());
        }
    }

    public ApplicationRepresentation toRepresentation(ApplicationModel applicationModel) {
        ApplicationRepresentation rep = new ApplicationRepresentation();
        rep.setId(applicationModel.getId());
        rep.setName(applicationModel.getName());
        rep.setEnabled(applicationModel.isEnabled());
        rep.setAdminUrl(applicationModel.getManagementUrl());
        rep.setSurrogateAuthRequired(applicationModel.isSurrogateAuthRequired());
        rep.setBaseUrl(applicationModel.getBaseUrl());

        Set<String> redirectUris = applicationModel.getAgent().getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<String>(redirectUris));
        }

        Set<String> webOrigins = applicationModel.getAgent().getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<String>(webOrigins));
        }

        if (!applicationModel.getDefaultRoles().isEmpty()) {
            rep.setDefaultRoles(applicationModel.getDefaultRoles().toArray(new String[0]));
        }

        return rep;

    }

    @JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-not-required",
            "resource", "credentials",
            "use-resource-role-mappings"})
    public static class InstallationAdapterConfig extends BaseRealmConfig {
        @JsonProperty("resource")
        protected String resource;
        @JsonProperty("use-resource-role-mappings")
        protected boolean useResourceRoleMappings;
        @JsonProperty("credentials")
        protected Map<String, String> credentials = new HashMap<String, String>();

        public boolean isUseResourceRoleMappings() {
            return useResourceRoleMappings;
        }

        public void setUseResourceRoleMappings(boolean useResourceRoleMappings) {
            this.useResourceRoleMappings = useResourceRoleMappings;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }
        public Map<String, String> getCredentials() {
            return credentials;
        }

        public void setCredentials(Map<String, String> credentials) {
            this.credentials = credentials;
        }

    }


    public InstallationAdapterConfig toInstallationRepresentation(RealmModel realmModel, ApplicationModel applicationModel, URI baseUri) {
        InstallationAdapterConfig rep = new InstallationAdapterConfig();
        rep.setRealm(realmModel.getName());
        rep.setRealmKey(realmModel.getPublicKeyPem());
        rep.setSslNotRequired(realmModel.isSslNotRequired());

        rep.setAuthServerUrl(baseUri.toString());
        rep.setUseResourceRoleMappings(applicationModel.getRoles().size() > 0);

        rep.setResource(applicationModel.getName());

        Map<String, String> creds = new HashMap<String, String>();
        String cred = realmModel.getSecret(applicationModel.getAgent()).getValue();
        creds.put(CredentialRepresentation.SECRET, cred);
        rep.setCredentials(creds);

        return rep;
    }

    public String toJBossSubsystemConfig(RealmModel realmModel, ApplicationModel applicationModel, URI baseUri) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<secure-deployment name=\"WAR MODULE NAME.war\">\n");
        buffer.append("    <realm>").append(realmModel.getName()).append("</realm>\n");
        buffer.append("    <realm-public-key>").append(realmModel.getPublicKeyPem()).append("</realm-public-key>\n");
        buffer.append("    <auth-server-url>").append(baseUri.toString()).append("</auth-server-url>\n");
        buffer.append("    <ssl-not-required>").append(realmModel.isSslNotRequired()).append("</ssl-not-required>\n");
        buffer.append("    <resource>").append(applicationModel.getName()).append("</resource>\n");
        String cred = realmModel.getSecret(applicationModel.getAgent()).getValue();
        buffer.append("    <credential name=\"secret\">").append(cred).append("</credential>\n");
        buffer.append("</secure-deployment>\n");
        return buffer.toString();
    }

}
