package org.keycloak.protocol.oidc.mappers;

import java.util.EnumSet;
import java.util.Set;

import org.keycloak.protocol.ProtocolMapperBuilder;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

public class OIDCProtocolMapperBuilder<B extends OIDCProtocolMapperBuilder<B>> extends ProtocolMapperBuilder<B> {

    public enum ClaimType {
        STRING("String"),
        LONG("long"),
        INT("int"),
        BOOLEAN("boolean"),
        JSON("JSON");

        private final String value;

        ClaimType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum IncludeIn {
        ACCESS_TOKEN(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN),
        ID_TOKEN(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN),
        USERINFO(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO),
        INTROSPECTION(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION),
        ACCESS_TOKEN_RESPONSE(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_RESPONSE),
        LIGHTWEIGHT_ACCESS_TOKEN(OIDCAttributeMapperHelper.INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN);

        private final String configKey;

        IncludeIn(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    protected OIDCProtocolMapperBuilder(String name, String providerId) {
        super(name);
        protocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        protocolMapper(providerId);
    }

    public B includeIn(IncludeIn first, IncludeIn... rest) {
        return includeIn(EnumSet.of(first, rest));
    }

    public B includeIn(Set<IncludeIn> selected) {
        for (IncludeIn target : IncludeIn.values()) {
            config(target.getConfigKey(), Boolean.toString(selected.contains(target)));
        }
        return self();
    }

    public B claimName(String claimName) {
        return config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, claimName);
    }

    public B type(ClaimType claimType) {
        return config(OIDCAttributeMapperHelper.JSON_TYPE, claimType.getValue());
    }

    public B type(String claimType) {
        return config(OIDCAttributeMapperHelper.JSON_TYPE, claimType);
    }

    public B userAttribute(String userAttribute) {
        return config(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
    }

    public B multivalued() {
        return multivalued(true);
    }

    public B multivalued(boolean multivalued) {
        return config(ProtocolMapperUtils.MULTIVALUED, Boolean.toString(multivalued));
    }

    public static OIDCProtocolMapperBuilder<?> builder(String name, String providerId) {
        return new Default(name, providerId);
    }

    private static final class Default extends OIDCProtocolMapperBuilder<Default> {
        private Default(String name, String providerId) {
            super(name, providerId);
        }
    }
}
