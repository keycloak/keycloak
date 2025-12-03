package org.keycloak.testframework.realm;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author rmartinc
 */
public class ClientProfileBuilder {

    private final ClientProfileRepresentation rep;

    private ClientProfileBuilder(ClientProfileRepresentation rep) {
        this.rep = rep;
    }

    public static ClientProfileBuilder create() {
        ClientProfileRepresentation rep = new ClientProfileRepresentation();
        return new ClientProfileBuilder(rep);
    }

    public static ClientProfileBuilder update(ClientProfileRepresentation rep) {
        return new ClientProfileBuilder(rep);
    }

    public ClientProfileBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public ClientProfileBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public ClientProfileBuilder executor(String providerId, ClientPolicyExecutorConfigurationRepresentation config) {
        ClientPolicyExecutorRepresentation executor = new ClientPolicyExecutorRepresentation();
        executor.setExecutorProviderId(providerId);
        if (config == null) {
            config = new ClientPolicyExecutorConfigurationRepresentation();
        }
        try {
            executor.setConfiguration(JsonSerialization.mapper.readValue(JsonSerialization.mapper.writeValueAsBytes(config), JsonNode.class));
        } catch(IOException e) {
            throw new IllegalArgumentException("Invalid configuration", e);
        }
        List<ClientPolicyExecutorRepresentation> executors = rep.getExecutors();
        if (executors == null) {
            executors = new LinkedList<>();
            rep.setExecutors(executors);
        }
        executors.add(executor);
        return this;
    }

    public ClientProfileRepresentation build() {
        return rep;
    }
}
