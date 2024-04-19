package org.keycloak.services.clienttype.client;

import org.keycloak.client.clienttype.ClientType;
import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.common.util.ObjectUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;


enum ExtendedTypedClientAttribute implements TypedClientAttributeInterface {
    // Extended Client Type attributes defined as client attribute entities.
    DEVICE_AUTHORIZATION_GRANT_ENABLED("oauth2.device.authorization.grant.enabled", "false"),
    CIBA_GRANT_ENABLED("oidc.ciba.grant.enabled", "false");

    private static final Map<String, ExtendedTypedClientAttribute> attributesByName = new HashMap<>();

    static {
        Arrays.stream(ExtendedTypedClientAttribute.values())
                .forEach(attribute -> attributesByName.put(attribute.getPropertyName(), attribute));
    }

    private final String propertyName;
    private final Object nonApplicableValue;

    ExtendedTypedClientAttribute(String propertyName, Object nonApplicableValue) {
        this.propertyName = propertyName;
        this.nonApplicableValue = nonApplicableValue;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Object getNonApplicableValue() {
        return nonApplicableValue;
    }

    public static Map<String, ExtendedTypedClientAttribute> getAttributesByName() {
        return attributesByName;
    }
}

enum TypedClientRepresentationAttribute implements TypedClientAttributeInterface {
    // Client type attributes specific to client representations.
    AUTHORIZATION_SERVICES_ENABLED("authorizationServicesEnabled", false),
    LOGIN_THEME("login_theme", ""),
    LOGO_URI("logoUri", null),
    POLICY_URI("policyUri", null);

    private final String propertyName;
    private final Object nonApplicableValue;

     TypedClientRepresentationAttribute(String propertyName, Object nonApplicableValue) {
        this.propertyName = propertyName;
        this.nonApplicableValue = nonApplicableValue;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Object getNonApplicableValue() {
        return nonApplicableValue;
    }
}

enum TypedClientAttribute implements TypedClientAttributeInterface {
    // Client Type attributes shared by client model and representation.
    STANDARD_FLOW_ENABLED("standardFlowEnabled", false),
    BEARER_ONLY("bearerOnly", false),
    CONSENT_REQUIRED("consentRequired", false),
    DIRECT_ACCESS_GRANTS_ENABLED("directAccessGrantsEnabled", false),
    ALWAYS_DISPLAY_IN_CONSOLE("alwaysDisplayInConsole", false),
    FRONTCHANNEL_LOGOUT("frontchannelLogout", false),
    IMPLICIT_FLOW_ENABLED("implicitFlowEnabled", false),
    PROTOCOL("protocol", null),
    PUBLIC_CLIENT("publicClient", false),
    REDIRECT_URIS("redirectUris", false);

    private final String propertyName;
    private final Object nonApplicableValue;

    private static final Map<String, TypedClientAttribute> attributesByName = new HashMap<>();

    static {
        Arrays.stream(TypedClientAttribute.values())
                .forEach(attribute -> attributesByName.put(attribute.getPropertyName(), attribute));
    }

    TypedClientAttribute(String propertyName, Object nonApplicableValue) {
        this.propertyName = propertyName;
        this.nonApplicableValue = nonApplicableValue;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Object getNonApplicableValue() {
        return nonApplicableValue;
    }

    public static Map<String, TypedClientAttribute> getAttributesByName() {
        return attributesByName;
    }
}

interface TypedClientAttributeInterface {

    default <T> T getClientAttribute(ClientType clientType, Supplier<T> clientGetter, Class<T> tClass) {
        String propertyName = getPropertyName();
        Object nonApplicableValue = getNonApplicableValue();

        // Check if clientType supports the feature.
        if (!clientType.isApplicable(propertyName)) {
            return tClass.cast(nonApplicableValue);
        }

        //  Check if this is read-only. If yes, then we just directly delegate to return stuff from the clientType rather than from client
        if (clientType.isReadOnly(propertyName)) {
            return clientType.getDefaultValue(propertyName, tClass);
        }

        // Delegate to clientGetter
        return clientGetter.get();
    }

    default <T> void setClientAttribute(ClientType clientType, T newValue, Consumer<T> clientSetter, Class<T> tClass) {
        String propertyName = getPropertyName();
        Object nonApplicableValue = getNonApplicableValue();
        // Check if clientType supports the feature. If not, return directly
        if (!clientType.isApplicable(propertyName) && !Objects.equals(nonApplicableValue, newValue)) {
            throw new ClientTypeException(
                    "Property is not-applicable to client type " + clientType.getName()
                            + " and can not be modified.", propertyName);
        }

        // Check if this is read-only. If yes and there is an attempt to change some stuff, then throw an exception
        if (clientType.isReadOnly(propertyName)) {
            T oldVal = clientType.getDefaultValue(propertyName, tClass);
            if (!ObjectUtil.isEqualOrBothNull(oldVal, newValue)) {
                throw new ClientTypeException(
                        "Property " + propertyName + " is read-only due to client type " + clientType.getName(),
                        propertyName);
            }
        }

        // Delegate to clientSetter
        clientSetter.accept(newValue);
    }

    String getPropertyName();
    Object getNonApplicableValue();
}