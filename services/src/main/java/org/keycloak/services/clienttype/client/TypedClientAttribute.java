package org.keycloak.services.clienttype.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.keycloak.client.clienttype.ClientType;
import org.keycloak.client.clienttype.ClientTypeException;

import org.jboss.logging.Logger;

enum TypedClientSimpleAttribute implements TypedClientAttribute {
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

    TypedClientSimpleAttribute(String propertyName, Object nonApplicableValue) {
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

enum TypedClientExtendedAttribute implements TypedClientAttribute {
    // Extended Client Type attributes defined as client attribute entities.
    DEVICE_AUTHORIZATION_GRANT_ENABLED("oauth2.device.authorization.grant.enabled", "false"),
    CIBA_GRANT_ENABLED("oidc.ciba.grant.enabled", "false"),
    LOGIN_THEME("login_theme", null),
    LOGO_URI("logoUri", null),
    POLICY_URI("policyUri", null),

    SAML_ALLOW_ECP_FLOW("saml.allow.ecp.flow", "false"),
    SAML_ARTIFACT_BINDING("saml.artifact.binding", "false"),
    SAML_ARTIFACT_BINDING_IDENTIFIER("saml.artifact.binding.identifier", null),
    SAML_ARTIFACT_BINDING_URL("saml_artifact_binding_url", null),
    SAML_ARTIFACT_RESOLUTION_SERVICE_URL("saml_artifact_resolution_service_url", null),
    SAML_ASSERTION_CONSUMER_URL_POST("saml_assertion_consumer_url_post", null),
    SAML_ASSERTION_CONSUMER_URL_REDIRECT("saml_assertion_consumer_url_redirect", null),
    SAML_ASSERTION_LIFESPAN("saml.assertion.lifespan", null),
    SAML_ASSERTION_SIGNATURE("saml.assertion.signature", "false"),
    SAML_AUTHNSTATEMENT("saml.authnstatement", "false"),
    SAML_CLIENT_SIGNATURE("saml.client.signature", "false"),
    SAML_ENCRYPT("saml.encrypt", "false"),
    SAML_ENCRYPTION_CERTIFICATE("saml.encryption.certificate", null),
    SAML_ENCRYPTION_PRIVATE_KEY("saml.encryption.private.key", null),
    SAML_FORCE_POST_BINDING("saml.force.post.binding", "false"),
    SAML_FORCE_NAME_ID_FORMAT("saml_force_name_id_format", "false"),
    SAML_IDP_INITIATED_SSO_RELAY_STATE("saml_idp_initiated_sso_relay_state", null),
    SAML_IDP_INITIATED_SSO_URL_NAME("saml_idp_initiated_sso_url_name", null),
    SAML_ONETIMEUSE_CONDITION("saml.onetimeuse.condition", "false"),
    SAML_SERVER_SIGNATURE("saml.server.signature", "false"),
    SAML_SERVER_SIGNATURE_KEYINFO_EXT("saml.server.signature.keyinfo.ext", "false"),
    SAML_SERVER_SIGNATURE_KEYINFO_XMLSIGKEYINFOKEYNAMETRANSFORMER("saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer", null),
    SAML_SIGNATURE_ALGORITHM("saml.signature.algorithm", null),
    SAML_SIGNATURE_CANONICALIZATION_METHOD("saml_signature_canonicalization_method", null),
    SAML_SIGNING_CERTIFICATE("saml.signing.certificate", null),
    SAML_SIGNING_PRIVATE_KEY("saml.signing.private.key", null),
    SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT("saml_single_logout_service_url_artifact", null),
    SAML_SINGLE_LOGOUT_SERVICE_URL_POST("saml_single_logout_service_url_post", null),
    SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT("saml_single_logout_service_url_redirect", null),
    SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP("saml_single_logout_service_url_soap", null);


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

interface TypedClientAttribute {
    Logger logger = Logger.getLogger(TypedClientAttribute.class);

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

        T typeValue = clientType.getTypeValue(propertyName, tClass);
        // If the value is not supplied by the client type, delegate to the client getter.
        return typeValue == null ? clientGetter.get() : typeValue;
    }

    default <T> void setClientAttribute(ClientType clientType, T newValue, Consumer<T> clientSetter, Class<T> tClass) {
        String propertyName = getPropertyName();
        // If feature is set as non-applicable, return directly
        if (!clientType.isApplicable(propertyName)) {
            if(!Objects.equals(getNonApplicableValue(), newValue)) {
                logger.warnf("Property %s is not-applicable to client type %s and can not be modified.", propertyName, clientType.getName());
            }
            return;
        }

        // If there is an attempt to change a value for an applicable field with a read-only value set, then throw an exception.
        T readOnlyValue = clientType.getTypeValue(propertyName, tClass);
        if (readOnlyValue != null && !readOnlyValue.equals(newValue)) {
            throw ClientTypeException.Message.CLIENT_UPDATE_FAILED_CLIENT_TYPE_VALIDATION.exception(propertyName);
        }

        // Delegate to clientSetter
        clientSetter.accept(newValue);
    }

    String getPropertyName();
    Object getNonApplicableValue();
}
