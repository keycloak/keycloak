package org.keycloak.admin.ui.rest.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class AuthenticationMapper {
    private static final int MAX_USED_BY = 9;

    public static Authentication convertToModel(KeycloakSession session, AuthenticationFlowModel flow, RealmModel realm) {

        final Authentication authentication = new Authentication();
        authentication.setId(flow.getId());
        authentication.setAlias(flow.getAlias());
        authentication.setBuiltIn(flow.isBuiltIn());
        authentication.setDescription(flow.getDescription());

        final List<String> usedByIdp = session.identityProviders().getByFlow(flow.getId(), null,0, MAX_USED_BY).toList();
        if (!usedByIdp.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.SPECIFIC_PROVIDERS, usedByIdp));
        }


        Stream<ClientModel> browserFlowOverridingClients = realm.searchClientByAuthenticationFlowBindingOverrides(Collections.singletonMap("browser", flow.getId()), 0, MAX_USED_BY);
        Stream<ClientModel> directGrantFlowOverridingClients = realm.searchClientByAuthenticationFlowBindingOverrides(Collections.singletonMap("direct_grant", flow.getId()), 0, MAX_USED_BY);
        Map<String, ClientModel> clientsByInternalId = new LinkedHashMap<>();
        Stream.concat(browserFlowOverridingClients, directGrantFlowOverridingClients).forEach(c -> clientsByInternalId.putIfAbsent(c.getId(), c));

        final List<ClientModel> clientModels = clientsByInternalId.values().stream().limit(MAX_USED_BY).collect(Collectors.toList());
        final List<String> usedClients = clientModels.stream().map(ClientModel::getClientId).collect(Collectors.toList());
        final List<UsedByClientRef> clientRefs = clientModels.stream()
                .map(c -> new UsedByClientRef(c.getId(), c.getClientId())).collect(Collectors.toList());

        if (!usedClients.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.SPECIFIC_CLIENTS, usedClients, clientRefs));
        }

        final List<String> useAsDefault = Stream.of(realm.getBrowserFlow(), realm.getRegistrationFlow(), realm.getDirectGrantFlow(),
                        realm.getResetCredentialsFlow(), realm.getClientAuthenticationFlow(), realm.getDockerAuthenticationFlow(), realm.getFirstBrokerLoginFlow())
                .filter(f -> f != null && flow.getAlias().equals(f.getAlias())).map(AuthenticationFlowModel::getAlias).collect(Collectors.toList());

        if (!useAsDefault.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.DEFAULT, useAsDefault));
        }

        return authentication;
    }
}
