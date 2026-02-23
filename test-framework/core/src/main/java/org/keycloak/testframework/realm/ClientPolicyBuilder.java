package org.keycloak.testframework.realm;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.services.clientpolicy.condition.GrantTypeCondition;
import org.keycloak.services.clientpolicy.condition.IdentityProviderCondition;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author rmartinc
 */
public class ClientPolicyBuilder {

    private final ClientPolicyRepresentation rep;

    private ClientPolicyBuilder(ClientPolicyRepresentation rep) {
        this.rep = rep;
    }

    public static ClientPolicyBuilder create() {
        ClientPolicyRepresentation rep = new ClientPolicyRepresentation();
        rep.setEnabled(true);
        return new ClientPolicyBuilder(rep);
    }

    public static GrantTypeCondition.Configuration grantTypeConditionConfiguration(boolean negativeLogic, String... types) {
        GrantTypeCondition.Configuration config = new GrantTypeCondition.Configuration();
        config.setNegativeLogic(negativeLogic);
        if (types != null && types.length > 0) {
            config.setGrantTypes(List.of(types));
        }
        return config;
    }

    public static IdentityProviderCondition.Configuration identityProviderConditionConfiguration(boolean negativeLogic, String... aliases) {
        IdentityProviderCondition.Configuration config = new IdentityProviderCondition.Configuration();
        config.setNegativeLogic(negativeLogic);
        if (aliases != null && aliases.length > 0) {
            config.setIdentityProviderAliases(List.of(aliases));
        }
        return config;
    }

    public static ClientPolicyBuilder update(ClientPolicyRepresentation rep) {
        return new ClientPolicyBuilder(rep);
    }

    public ClientPolicyBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public ClientPolicyBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public ClientPolicyBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public ClientPolicyBuilder condition(String providerId, ClientPolicyConditionConfigurationRepresentation config) {
        ClientPolicyConditionRepresentation condition = new ClientPolicyConditionRepresentation();
        condition.setConditionProviderId(providerId);
        if (config == null) {
            config = new ClientPolicyConditionConfigurationRepresentation();
        }
        try {
            condition.setConfiguration(JsonSerialization.mapper.readValue(JsonSerialization.mapper.writeValueAsBytes(config), JsonNode.class));
        } catch(IOException e) {
            throw new IllegalArgumentException("Invalid configuration", e);
        }
        List<ClientPolicyConditionRepresentation> conditions = rep.getConditions();
        if (conditions == null) {
            conditions = new LinkedList<>();
            rep.setConditions(conditions);
        }
        conditions.add(condition);
        return this;
    }

    public ClientPolicyBuilder profile(String... profile) {
        List<String> profiles = rep.getProfiles();
        if (profiles == null) {
            profiles = new LinkedList<>();
            rep.setProfiles(profiles);
        }
        profiles.addAll(List.of(profile));
        return this;
    }

    public ClientPolicyRepresentation build() {
        return rep;
    }
}
