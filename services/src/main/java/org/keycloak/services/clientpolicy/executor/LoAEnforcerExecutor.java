package org.keycloak.services.clientpolicy.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;

import java.util.List;
import java.util.Map;

import static org.keycloak.services.clientpolicy.executor.LoAEnforcerExecutorFactory.MIN_ACR;
import static org.keycloak.services.clientpolicy.executor.LoAEnforcerExecutorFactory.PROVIDER_ID;
import static org.keycloak.services.clientpolicy.executor.LoAEnforcerExecutorFactory.USE_CLIENT_ACRS;

public class LoAEnforcerExecutor implements ClientPolicyExecutorProvider<LoAEnforcerExecutor.Configuration> {

    public static final String CONTEXT_DATA_ENFORCED_ACR = "ENFORCED_ACR";

    private final KeycloakSession session;
    private Configuration configuration;

    public LoAEnforcerExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty(MIN_ACR)
        protected String minAcr;

        public String getMinAcr() {
            return minAcr;
        }

        public void setMinAcr(String minAcr) {
            this.minAcr = minAcr;
        }

        @JsonProperty(USE_CLIENT_ACRS)
        protected Boolean useClientAcrs;

        public Boolean isUseClientAcrs() {
            return useClientAcrs;
        }

        public void setUseClientAcrs(Boolean useClientAcrs) {
            this.useClientAcrs = useClientAcrs;
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
                AuthorizationRequestContext authorizationRequestContext = (AuthorizationRequestContext) context;
                executeOnAuthorizationRequest(authorizationRequestContext);
                return;
            default:
                return;
        }
    }

    private void executeOnAuthorizationRequest(AuthorizationRequestContext authorizationRequestContext) {
        String enforcedAcr = getEnforcedAcr();
        authorizationRequestContext.getAuthorizationEndpointRequest().addContextData(CONTEXT_DATA_ENFORCED_ACR, enforcedAcr);
    }

    private String getEnforcedAcr() {
        String enforcedAcr;
        if (configuration.useClientAcrs) {
            ClientModel client = session.getContext().getClient();
            List<String> defaultAcrValues = AcrUtils.getDefaultAcrValues(client);
            Map<String, Integer> acrToLoaMap = AcrUtils.getAcrLoaMap(client);
            if (acrToLoaMap.isEmpty()) {
                acrToLoaMap = AcrUtils.getAcrLoaMap(client.getRealm());
            }
            enforcedAcr = acrToLoaMap.entrySet().stream()
                    .filter(it -> defaultAcrValues.contains(it.getKey()))
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(configuration.getMinAcr());
        } else {
            enforcedAcr = configuration.getMinAcr();
        }
        return enforcedAcr;
    }

}
