package org.keycloak.services.managers;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.representations.adapters.config.BaseRealmConfig;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.UserRoleMappingRepresentation;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * @param resourceRep
     * @return
     */
    public ApplicationModel createApplication(RealmModel realm, ApplicationRepresentation resourceRep) {
        logger.debug("************ CREATE APPLICATION: {0}" + resourceRep.getName());
        ApplicationModel applicationModel = realm.addApplication(resourceRep.getName());
        if (resourceRep.isEnabled() != null) applicationModel.setEnabled(resourceRep.isEnabled());
        applicationModel.setManagementUrl(resourceRep.getAdminUrl());
        if (resourceRep.isSurrogateAuthRequired() != null)
            applicationModel.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
        applicationModel.setBaseUrl(resourceRep.getBaseUrl());
        if (resourceRep.isBearerOnly() != null) applicationModel.setBearerOnly(resourceRep.isBearerOnly());
        if (resourceRep.isPublicClient() != null) applicationModel.setPublicClient(resourceRep.isPublicClient());
        applicationModel.updateApplication();

        if (resourceRep.getNotBefore() != null) {
            applicationModel.setNotBefore(resourceRep.getNotBefore());
        }

        applicationModel.setSecret(resourceRep.getSecret());
        if (applicationModel.getSecret() == null) {
            generateSecret(applicationModel);
        }


        if (resourceRep.getRedirectUris() != null) {
            for (String redirectUri : resourceRep.getRedirectUris()) {
                applicationModel.addRedirectUri(redirectUri);
            }
        }
        if (resourceRep.getWebOrigins() != null) {
            for (String webOrigin : resourceRep.getWebOrigins()) {
                logger.debugv("Application: {0} webOrigin: {1}", resourceRep.getName(), webOrigin);
                applicationModel.addWebOrigin(webOrigin);
            }
        } else {
            // add origins from redirect uris
            if (resourceRep.getRedirectUris() != null) {
                Set<String> origins = new HashSet<String>();
                for (String redirectUri : resourceRep.getRedirectUris()) {
                    logger.info("add redirectUri to origin: " + redirectUri);
                    if (redirectUri.startsWith("http:")) {
                        URI uri = URI.create(redirectUri);
                        String origin = uri.getScheme() + "://" + uri.getHost();
                        if (uri.getPort() != -1) {
                            origin += ":" + uri.getPort();
                        }
                        logger.debugv("adding default application origin: {0}" , origin);
                        origins.add(origin);
                    }
                }
                if (origins.size() > 0) {
                    applicationModel.setWebOrigins(origins);
                }
            }
        }

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

    public void createRoleMappings(ApplicationModel applicationModel, UserModel user, List<String> roleNames) {
        for (String roleName : roleNames) {
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            RoleModel role = applicationModel.getRole(roleName.trim());
            if (role == null) {
                role = applicationModel.addRole(roleName.trim());
            }
            user.grantRole(role);

        }
    }

    public void createScopeMappings(RealmModel realm, ApplicationModel applicationModel, List<ScopeMappingRepresentation> mappings) {
        for (ScopeMappingRepresentation mapping : mappings) {
            for (String roleString : mapping.getRoles()) {
                RoleModel role = applicationModel.getRole(roleString.trim());
                if (role == null) {
                    role = applicationModel.addRole(roleString.trim());
                }
                ClientModel client = realm.findClient(mapping.getClient());
                client.addScopeMapping(role);
            }
        }
    }

    public ApplicationModel createApplication(RealmModel realm, String name) {
        ApplicationModel app = realm.addApplication(name);
        generateSecret(app);

        return app;
    }

    public boolean removeApplication(RealmModel realm, ApplicationModel application) {
        if (realm.removeApplication(application.getId())) {
            UserSessionProvider sessions = realmManager.getSession().sessions();
            if (sessions != null) {
                sessions.onClientRemoved(realm, application);
            }
            return true;
        } else {
            return false;
        }
    }

    public UserCredentialModel generateSecret(ApplicationModel app) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        app.setSecret(secret.getValue());
        return secret;
    }

    public void updateApplication(ApplicationRepresentation rep, ApplicationModel resource) {
        if (rep.getName() != null) resource.setName(rep.getName());
        if (rep.isEnabled() != null) resource.setEnabled(rep.isEnabled());
        if (rep.isBearerOnly() != null) resource.setBearerOnly(rep.isBearerOnly());
        if (rep.isPublicClient() != null) resource.setPublicClient(rep.isPublicClient());
        if (rep.getAdminUrl() != null) resource.setManagementUrl(rep.getAdminUrl());
        if (rep.getBaseUrl() != null) resource.setBaseUrl(rep.getBaseUrl());
        if (rep.isSurrogateAuthRequired() != null) resource.setSurrogateAuthRequired(rep.isSurrogateAuthRequired());
        resource.updateApplication();

        if (rep.getNotBefore() != null) {
            resource.setNotBefore(rep.getNotBefore());
        }
        if (rep.getDefaultRoles() != null) {
            resource.updateDefaultRoles(rep.getDefaultRoles());
        }

        List<String> redirectUris = rep.getRedirectUris();
        if (redirectUris != null) {
            resource.setRedirectUris(new HashSet<String>(redirectUris));
        }

        List<String> webOrigins = rep.getWebOrigins();
        if (webOrigins != null) {
            resource.setWebOrigins(new HashSet<String>(webOrigins));
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
        rep.setPublicClient(applicationModel.isPublicClient());
        rep.setBearerOnly(applicationModel.isBearerOnly());
        rep.setSurrogateAuthRequired(applicationModel.isSurrogateAuthRequired());
        rep.setBaseUrl(applicationModel.getBaseUrl());
        rep.setNotBefore(applicationModel.getNotBefore());

        Set<String> redirectUris = applicationModel.getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<String>(redirectUris));
        }

        Set<String> webOrigins = applicationModel.getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<String>(webOrigins));
        }

        if (!applicationModel.getDefaultRoles().isEmpty()) {
            rep.setDefaultRoles(applicationModel.getDefaultRoles().toArray(new String[0]));
        }

        return rep;

    }

    @JsonPropertyOrder({"realm", "realm-public-key", "bearer-only", "auth-server-url", "ssl-not-required",
            "resource", "public-client", "credentials",
            "use-resource-role-mappings"})
    public static class InstallationAdapterConfig extends BaseRealmConfig {
        @JsonProperty("resource")
        protected String resource;
        @JsonProperty("use-resource-role-mappings")
        protected Boolean useResourceRoleMappings;
        @JsonProperty("bearer-only")
        protected Boolean bearerOnly;
        @JsonProperty("public-client")
        protected Boolean publicClient;
        @JsonProperty("credentials")
        protected Map<String, String> credentials;

        public Boolean isUseResourceRoleMappings() {
            return useResourceRoleMappings;
        }

        public void setUseResourceRoleMappings(Boolean useResourceRoleMappings) {
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

        public Boolean getPublicClient() {
            return publicClient;
        }

        public void setPublicClient(Boolean publicClient) {
            this.publicClient = publicClient;
        }

        public Boolean getBearerOnly() {
            return bearerOnly;
        }

        public void setBearerOnly(Boolean bearerOnly) {
            this.bearerOnly = bearerOnly;
        }
    }


    public InstallationAdapterConfig toInstallationRepresentation(RealmModel realmModel, ApplicationModel applicationModel, URI baseUri) {
        InstallationAdapterConfig rep = new InstallationAdapterConfig();
        rep.setRealm(realmModel.getName());
        rep.setRealmKey(realmModel.getPublicKeyPem());
        rep.setSslNotRequired(realmModel.isSslNotRequired());

        if (applicationModel.isPublicClient() && !applicationModel.isBearerOnly()) rep.setPublicClient(true);
        if (applicationModel.isBearerOnly()) rep.setBearerOnly(true);
        if (!applicationModel.isBearerOnly()) rep.setAuthServerUrl(baseUri.toString());
        if (applicationModel.getRoles().size() > 0) rep.setUseResourceRoleMappings(true);

        rep.setResource(applicationModel.getName());

        if (!applicationModel.isBearerOnly() && !applicationModel.isPublicClient()) {
            Map<String, String> creds = new HashMap<String, String>();
            String cred = applicationModel.getSecret();
            creds.put(CredentialRepresentation.SECRET, cred);
            rep.setCredentials(creds);
        }

        return rep;
    }

    public String toJBossSubsystemConfig(RealmModel realmModel, ApplicationModel applicationModel, URI baseUri) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<secure-deployment name=\"WAR MODULE NAME.war\">\n");
        buffer.append("    <realm>").append(realmModel.getName()).append("</realm>\n");
        buffer.append("    <realm-public-key>").append(realmModel.getPublicKeyPem()).append("</realm-public-key>\n");
        if (applicationModel.isBearerOnly()){
            buffer.append("    <bearer-only>true</bearer-only>\n");

        } else {
            buffer.append("    <auth-server-url>").append(baseUri.toString()).append("</auth-server-url>\n");
            if (applicationModel.isPublicClient() && !applicationModel.isBearerOnly()) {
                buffer.append("    <public-client>true</public-client>\n");
            }
        }
        buffer.append("    <ssl-not-required>").append(realmModel.isSslNotRequired()).append("</ssl-not-required>\n");
        buffer.append("    <resource>").append(applicationModel.getName()).append("</resource>\n");
        String cred = applicationModel.getSecret();
        if (!applicationModel.isBearerOnly() && !applicationModel.isPublicClient()) {
            buffer.append("    <credential name=\"secret\">").append(cred).append("</credential>\n");
        }
        buffer.append("</secure-deployment>\n");
        return buffer.toString();
    }

}
