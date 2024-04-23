package org.keycloak.services.clienttype.client;

import org.jboss.logging.Logger;
import org.keycloak.client.clienttype.ClientType;
import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.common.util.ObjectUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

enum TypedClientAttribute implements TypedClientAttributeInterface {
    // Top Level client attributes
    STANDARD_FLOW_ENABLED("standardFlowEnabled", false),
    BEARER_ONLY("bearerOnly", false),
    CONSENT_REQUIRED("consentRequired", false),
    DIRECT_ACCESS_GRANTS_ENABLED("directAccessGrantsEnabled", false),
    ALWAYS_DISPLAY_IN_CONSOLE("alwaysDisplayInConsole", false),
    FRONTCHANNEL_LOGOUT("frontchannelLogout", false),
    IMPLICIT_FLOW_ENABLED("implicitFlowEnabled", false),
    PROTOCOL("protocol", null),
    PUBLIC_CLIENT("publicClient", false),
    REDIRECT_URIS("redirectUris", Set.of()),
    SERVICE_ACCOUNTS_ENABLED("serviceAccountsEnabled", false),
    WEB_ORIGINS("webOrigins", Set.of()),
    ;

    private final String propertyName;
    private final Object nonApplicableValue;

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
}

enum TypedClientExtendedAttribute implements TypedClientAttributeInterface {
    // Extended Client Type attributes defined as client attribute entities.
    DEVICE_AUTHORIZATION_GRANT_ENABLED("oauth2.device.authorization.grant.enabled", "false"),
    CIBA_GRANT_ENABLED("oidc.ciba.grant.enabled", "false"),
    LOGIN_THEME("login_theme", ""),
    LOGO_URI("logoUri", null),
    POLICY_URI("policyUri", null);

    private static final Map<String, TypedClientExtendedAttribute> attributesByName = new HashMap<>();

    static {
        Arrays.stream(TypedClientExtendedAttribute.values())
                .forEach(attribute -> attributesByName.put(attribute.getPropertyName(), attribute));
    }

    private final String propertyName;
    private final Object nonApplicableValue;

    TypedClientExtendedAttribute(String propertyName, Object nonApplicableValue) {
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

    public static Map<String, TypedClientExtendedAttribute> getAttributesByName() {
        return attributesByName;
    }
}

interface TypedClientAttributeInterface {
    Logger logger = Logger.getLogger(TypedClientAttributeInterface.class);

    default <T> T getClientAttribute(ClientType clientType, Supplier<T> clientGetter, Class<T> tClass) {
        String propertyName = getPropertyName();
        Object nonApplicableValue = getNonApplicableValue();

        // Check if clientType supports the feature.
        if (!clientType.isApplicable(propertyName)) {
            try {
                return tClass.cast(nonApplicableValue);
            } catch (ClassCastException e) {
                logger.error("Could not apply client type property %s: %s", propertyName, e);
                throw e;
            }
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