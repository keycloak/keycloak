package org.keycloak.services.managers;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.adapters.config.BaseRealmConfig;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.util.Time;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    public ApplicationModel createApplication(RealmModel realm, String name) {
        return KeycloakModelUtils.createApplication(realm, name);
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

    public Set<String> validateRegisteredNodes(ApplicationModel application) {
        Map<String, Integer> registeredNodes = application.getRegisteredNodes();
        if (registeredNodes == null || registeredNodes.isEmpty()) {
            return Collections.emptySet();
        }

        int currentTime = Time.currentTime();

        Set<String> validatedNodes = new TreeSet<String>();
        if (application.getNodeReRegistrationTimeout() > 0) {
            List<String> toRemove = new LinkedList<String>();
            for (Map.Entry<String, Integer> entry : registeredNodes.entrySet()) {
                Integer lastReRegistration = entry.getValue();
                if (lastReRegistration + application.getNodeReRegistrationTimeout() < currentTime) {
                    toRemove.add(entry.getKey());
                } else {
                    validatedNodes.add(entry.getKey());
                }
            }

            // Remove time-outed nodes
            for (String node : toRemove) {
                application.unregisterNode(node);
            }
        } else {
            // Periodic node reRegistration is disabled, so allow all nodes
            validatedNodes.addAll(registeredNodes.keySet());
        }

        return validatedNodes;
    }

    @JsonPropertyOrder({"realm", "realm-public-key", "bearer-only", "auth-server-url", "ssl-required",
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
        rep.setSslRequired(realmModel.getSslRequired().name().toLowerCase());

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
        buffer.append("    <ssl-required>").append(realmModel.getSslRequired().name()).append("</ssl-required>\n");
        buffer.append("    <resource>").append(applicationModel.getName()).append("</resource>\n");
        String cred = applicationModel.getSecret();
        if (!applicationModel.isBearerOnly() && !applicationModel.isPublicClient()) {
            buffer.append("    <credential name=\"secret\">").append(cred).append("</credential>\n");
        }
        if (applicationModel.getRoles().size() > 0) {
            buffer.append("    <use-resource-role-mappings>true</use-resource-role-mappings>\n");
        }
        buffer.append("</secure-deployment>\n");
        return buffer.toString();
    }

}
