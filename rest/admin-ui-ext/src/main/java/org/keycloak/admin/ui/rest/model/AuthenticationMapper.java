package org.keycloak.admin.ui.rest.model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

public class AuthenticationMapper {
    private static final int MAX_USED_BY = 9;

    public static Authentication convertToModel(AuthenticationFlowModel flow, RealmModel realm) {

        final Stream<IdentityProviderModel> identityProviders = realm.getIdentityProvidersStream();
        final Stream<ClientModel> clients = realm.getClientsStream();

        final Authentication authentication = new Authentication();
        authentication.setId(flow.getId());
        authentication.setAlias(flow.getAlias());
        authentication.setBuiltIn(flow.isBuiltIn());
        authentication.setDescription(flow.getDescription());

        final List<String> usedByIdp = identityProviders.filter(idp -> idp.getFirstBrokerLoginFlowId().equals(flow.getId()))
                .map(IdentityProviderModel::getAlias).limit(MAX_USED_BY).collect(Collectors.toList());
        if (!usedByIdp.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.SPECIFIC_PROVIDERS, usedByIdp));
        }

        final List<String> usedClients = clients.filter(
                        c -> c.getAuthenticationFlowBindingOverrides().get("browser") != null && c.getAuthenticationFlowBindingOverrides()
                                .get("browser").equals(flow.getId()) || c.getAuthenticationFlowBindingOverrides()
                                .get("direct_grant") != null && c.getAuthenticationFlowBindingOverrides().get("direct_grant").equals(flow.getId()))
                .map(ClientModel::getClientId).limit(MAX_USED_BY).collect(Collectors.toList());

        if (!usedClients.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.SPECIFIC_CLIENTS, usedClients));
        }

        final List<String> useAsDefault = Stream.of(realm.getBrowserFlow(), realm.getRegistrationFlow(), realm.getDirectGrantFlow(),
                        realm.getResetCredentialsFlow(), realm.getClientAuthenticationFlow(), realm.getDockerAuthenticationFlow())
                .filter(f -> flow.getAlias().equals(f.getAlias())).map(AuthenticationFlowModel::getAlias).collect(Collectors.toList());

        if (!useAsDefault.isEmpty()) {
            authentication.setUsedBy(new UsedBy(UsedBy.UsedByType.DEFAULT, useAsDefault));
        }

        return authentication;
    }
}
