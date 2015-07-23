package org.keycloak.services.managers;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.logging.Logger;
import org.keycloak.constants.ServiceAccountConstants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
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
public class ClientManager {
    protected Logger logger = Logger.getLogger(ClientManager.class);

    protected RealmManager realmManager;

    public ClientManager(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    public ClientManager() {
    }

    public ClientModel createClient(RealmModel realm, String name) {
        return KeycloakModelUtils.createClient(realm, name);
    }

    public boolean removeClient(RealmModel realm, ClientModel client) {
        if (realm.removeClient(client.getId())) {
            UserSessionProvider sessions = realmManager.getSession().sessions();
            if (sessions != null) {
                sessions.onClientRemoved(realm, client);
            }

            UserModel serviceAccountUser = realmManager.getSession().users().getUserByServiceAccountClient(client);
            if (serviceAccountUser != null) {
                realmManager.getSession().users().removeUser(realm, serviceAccountUser);
            }

            return true;
        } else {
            return false;
        }
    }

    public Set<String> validateRegisteredNodes(ClientModel client) {
        Map<String, Integer> registeredNodes = client.getRegisteredNodes();
        if (registeredNodes == null || registeredNodes.isEmpty()) {
            return Collections.emptySet();
        }

        int currentTime = Time.currentTime();

        Set<String> validatedNodes = new TreeSet<String>();
        if (client.getNodeReRegistrationTimeout() > 0) {
            List<String> toRemove = new LinkedList<String>();
            for (Map.Entry<String, Integer> entry : registeredNodes.entrySet()) {
                Integer lastReRegistration = entry.getValue();
                if (lastReRegistration + client.getNodeReRegistrationTimeout() < currentTime) {
                    toRemove.add(entry.getKey());
                } else {
                    validatedNodes.add(entry.getKey());
                }
            }

            // Remove time-outed nodes
            for (String node : toRemove) {
                client.unregisterNode(node);
            }
        } else {
            // Periodic node reRegistration is disabled, so allow all nodes
            validatedNodes.addAll(registeredNodes.keySet());
        }

        return validatedNodes;
    }

    public void enableServiceAccount(ClientModel client) {
        client.setServiceAccountsEnabled(true);

        // Add dedicated user for this service account
        if (realmManager.getSession().users().getUserByServiceAccountClient(client) == null) {
            String username = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId();
            logger.infof("Creating service account user '%s'", username);

            // Don't use federation for service account user
            UserModel user = realmManager.getSession().userStorage().addUser(client.getRealm(), username);
            user.setEnabled(true);
            user.setEmail(username + "@placeholder.org");
            user.setServiceAccountClientLink(client.getId());
        }

        // Add protocol mappers to retrieve clientId in access token
        if (client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, ServiceAccountConstants.CLIENT_ID_PROTOCOL_MAPPER) == null) {
            logger.debugf("Creating service account protocol mapper '%s' for client '%s'", ServiceAccountConstants.CLIENT_ID_PROTOCOL_MAPPER, client.getClientId());
            ProtocolMapperModel protocolMapper = UserSessionNoteMapper.createClaimMapper(ServiceAccountConstants.CLIENT_ID_PROTOCOL_MAPPER,
                    ServiceAccountConstants.CLIENT_ID,
                    ServiceAccountConstants.CLIENT_ID, "String",
                    false, "",
                    true, true);
            client.addProtocolMapper(protocolMapper);
        }

        // Add protocol mappers to retrieve hostname and IP address of client in access token
        if (client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, ServiceAccountConstants.CLIENT_HOST_PROTOCOL_MAPPER) == null) {
            logger.debugf("Creating service account protocol mapper '%s' for client '%s'", ServiceAccountConstants.CLIENT_HOST_PROTOCOL_MAPPER, client.getClientId());
            ProtocolMapperModel protocolMapper = UserSessionNoteMapper.createClaimMapper(ServiceAccountConstants.CLIENT_HOST_PROTOCOL_MAPPER,
                    ServiceAccountConstants.CLIENT_HOST,
                    ServiceAccountConstants.CLIENT_HOST, "String",
                    false, "",
                    true, true);
            client.addProtocolMapper(protocolMapper);
        }

        if (client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, ServiceAccountConstants.CLIENT_ADDRESS_PROTOCOL_MAPPER) == null) {
            logger.debugf("Creating service account protocol mapper '%s' for client '%s'", ServiceAccountConstants.CLIENT_ADDRESS_PROTOCOL_MAPPER, client.getClientId());
            ProtocolMapperModel protocolMapper = UserSessionNoteMapper.createClaimMapper(ServiceAccountConstants.CLIENT_ADDRESS_PROTOCOL_MAPPER,
                    ServiceAccountConstants.CLIENT_ADDRESS,
                    ServiceAccountConstants.CLIENT_ADDRESS, "String",
                    false, "",
                    true, true);
            client.addProtocolMapper(protocolMapper);
        }
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


    public InstallationAdapterConfig toInstallationRepresentation(RealmModel realmModel, ClientModel clientModel, URI baseUri) {
        InstallationAdapterConfig rep = new InstallationAdapterConfig();
        rep.setAuthServerUrl(baseUri.toString());
        rep.setRealm(realmModel.getName());
        rep.setRealmKey(realmModel.getPublicKeyPem());
        rep.setSslRequired(realmModel.getSslRequired().name().toLowerCase());

        if (clientModel.isPublicClient() && !clientModel.isBearerOnly()) rep.setPublicClient(true);
        if (clientModel.isBearerOnly()) rep.setBearerOnly(true);
        if (clientModel.getRoles().size() > 0) rep.setUseResourceRoleMappings(true);

        rep.setResource(clientModel.getClientId());

        if (!clientModel.isBearerOnly() && !clientModel.isPublicClient()) {
            Map<String, String> creds = new HashMap<String, String>();
            String cred = clientModel.getSecret();
            creds.put(CredentialRepresentation.SECRET, cred);
            rep.setCredentials(creds);
        }

        return rep;
    }

    public String toJBossSubsystemConfig(RealmModel realmModel, ClientModel clientModel, URI baseUri) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<secure-deployment name=\"WAR MODULE NAME.war\">\n");
        buffer.append("    <realm>").append(realmModel.getName()).append("</realm>\n");
        buffer.append("    <realm-public-key>").append(realmModel.getPublicKeyPem()).append("</realm-public-key>\n");
        buffer.append("    <auth-server-url>").append(baseUri.toString()).append("</auth-server-url>\n");
        if (clientModel.isBearerOnly()){
            buffer.append("    <bearer-only>true</bearer-only>\n");

        } else if (clientModel.isPublicClient()) {
            buffer.append("    <public-client>true</public-client>\n");
        }
        buffer.append("    <ssl-required>").append(realmModel.getSslRequired().name()).append("</ssl-required>\n");
        buffer.append("    <resource>").append(clientModel.getClientId()).append("</resource>\n");
        String cred = clientModel.getSecret();
        if (!clientModel.isBearerOnly() && !clientModel.isPublicClient()) {
            buffer.append("    <credential name=\"secret\">").append(cred).append("</credential>\n");
        }
        if (clientModel.getRoles().size() > 0) {
            buffer.append("    <use-resource-role-mappings>true</use-resource-role-mappings>\n");
        }
        buffer.append("</secure-deployment>\n");
        return buffer.toString();
    }

}
