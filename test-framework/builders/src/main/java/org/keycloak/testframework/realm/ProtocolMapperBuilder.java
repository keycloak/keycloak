package org.keycloak.testframework.realm;

import java.util.HashMap;

import org.keycloak.representations.idm.ProtocolMapperRepresentation;

/**
 * Builder to help with protocol mappers in the test-suite.
 *
 * @author rmartinc
 */
public class ProtocolMapperBuilder extends Builder<ProtocolMapperRepresentation> {

    private ProtocolMapperBuilder(ProtocolMapperRepresentation rep) {
        super(rep);
    }

    public static ProtocolMapperBuilder create() {
        return new ProtocolMapperBuilder(new ProtocolMapperRepresentation());
    }

    public static ProtocolMapperBuilder update(ProtocolMapperRepresentation rep) {
        return new ProtocolMapperBuilder(rep);
    }

    public ProtocolMapperBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public ProtocolMapperBuilder protocol(String protocol) {
        rep.setProtocol(protocol);
        return this;
    }

    public ProtocolMapperBuilder protocolMapper(String protocolMapper) {
        rep.setProtocolMapper(protocolMapper);
        return this;
    }

    public ProtocolMapperBuilder config(String key, String value) {
        rep.setConfig(Builder.createIfNull(rep.getConfig(), HashMap::new));
        rep.getConfig().put(key, value);
        return this;
    }
}
