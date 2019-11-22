package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Updater for client attributes. See {@link ServerResourceUpdater} for further details.
 * @author hmlnarik
 */
public class ClientAttributeUpdater extends ServerResourceUpdater<ClientAttributeUpdater, ClientResource, ClientRepresentation> {

    private final RealmResource realmResource;

    /**
     * Creates a {@ClientAttributeUpdater} for the given client. The client must exist.
     * @param adminClient
     * @param realm
     * @param clientId
     * @return
     */
    public static ClientAttributeUpdater forClient(Keycloak adminClient, String realm, String clientId) {
        RealmResource realmRes = adminClient.realm(realm);
        ClientsResource clients = realmRes.clients();
        List<ClientRepresentation> foundClients = clients.findByClientId(clientId);
        assertThat(foundClients, hasSize(1));
        ClientResource clientRes = clients.get(foundClients.get(0).getId());
        
        return new ClientAttributeUpdater(clientRes, realmRes);
    }

    private ClientAttributeUpdater(ClientResource resource, RealmResource realmResource) {
        super(resource, resource::toRepresentation, resource::update);
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
        this.realmResource = realmResource;
    }

    @Override
    protected void performUpdate(ClientRepresentation from, ClientRepresentation to) {
        super.performUpdate(from, to);
        updateViaAddRemove(from.getDefaultClientScopes(), to.getDefaultClientScopes(), this::getConversionForScopeNameToId, resource::addDefaultClientScope, resource::removeDefaultClientScope);
        updateViaAddRemove(from.getOptionalClientScopes(), to.getOptionalClientScopes(), this::getConversionForScopeNameToId, resource::addOptionalClientScope, resource::removeOptionalClientScope);
    }

    private Function<String, String> getConversionForScopeNameToId() {
        Map<String, String> scopeNameToIdMap = realmResource.clientScopes().findAll().stream()
          .collect(Collectors.toMap(ClientScopeRepresentation::getName, ClientScopeRepresentation::getId));

        return scopeNameToIdMap::get;
    }

    public ClientAttributeUpdater setClientId(String clientId) {
        this.rep.setClientId(clientId);
        return this;
    }

    public ClientAttributeUpdater setAttribute(String name, String value) {
        this.rep.getAttributes().put(name, value);
        if (value != null && !this.origRep.getAttributes().containsKey(name)) {
            this.origRep.getAttributes().put(name, null);
        }
        return this;
    }

    public ClientAttributeUpdater setRedirectUris(List<String> values) {
        this.rep.setRedirectUris(values);
        return this;
    }

    public ClientAttributeUpdater removeAttribute(String name) {
        this.rep.getAttributes().remove(name);
        return this;
    }

    public ClientAttributeUpdater setConsentRequired(Boolean consentRequired) {
        rep.setConsentRequired(consentRequired);
        return this;
    }

    public ClientAttributeUpdater setFrontchannelLogout(Boolean frontchannelLogout) {
        rep.setFrontchannelLogout(frontchannelLogout);
        return this;
    }

    public ClientAttributeUpdater setFullScopeAllowed(Boolean fullScopeAllowed) {
        rep.setFullScopeAllowed(fullScopeAllowed);
        return this;
    }

    public ClientAttributeUpdater setDefaultClientScopes(List<String> defaultClientScopes) {
        rep.setDefaultClientScopes(defaultClientScopes);
        return this;
    }

    public ClientAttributeUpdater setOptionalClientScopes(List<String> optionalClientScopes) {
        rep.setOptionalClientScopes(optionalClientScopes);
        return this;
    }

    public ProtocolMappersUpdater protocolMappers() {
        return new ProtocolMappersUpdater(resource.getProtocolMappers());
    }

    public RoleScopeUpdater realmRoleScope() {
        return new RoleScopeUpdater(resource.getScopeMappings().realmLevel());
    }

    public RoleScopeUpdater clientRoleScope(String clientUUID) {
        return new RoleScopeUpdater(resource.getScopeMappings().clientLevel(clientUUID));
    }

    public ClientAttributeUpdater setAdminUrl(String adminUrl) {
        rep.setAdminUrl(adminUrl);
        return this;
    }

    public ClientAttributeUpdater addDefaultClientScope(String clientScope) {
        rep.getDefaultClientScopes().add(clientScope);
        return this;
    }
}
