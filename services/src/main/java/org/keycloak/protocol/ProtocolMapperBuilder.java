package org.keycloak.protocol;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.ProtocolMapperModel;

public class ProtocolMapperBuilder<B extends ProtocolMapperBuilder<B>> {

    protected final String name;
    protected String protocol;
    protected String protocolMapper;
    protected final Map<String, String> config = new HashMap<>();

    protected ProtocolMapperBuilder(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    protected B self() {
        return (B) this;
    }

    public B protocol(String protocol) {
        this.protocol = protocol;
        return self();
    }

    public B protocolMapper(String protocolMapper) {
        this.protocolMapper = protocolMapper;
        return self();
    }

    public B config(String key, String value) {
        this.config.put(key, value);
        return self();
    }

    public ProtocolMapperModel build() {
        ProtocolMapperModel model = new ProtocolMapperModel();
        model.setName(name);
        model.setProtocol(protocol);
        model.setProtocolMapper(protocolMapper);
        model.setConfig(new HashMap<>(config));
        return model;
    }
}
