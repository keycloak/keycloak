package org.keycloak.protocol.oidc.mappers;

import org.jboss.logging.Logger;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCAttributeMapperHelper {
    private static final Logger logger = Logger.getLogger(OIDCAttributeMapperHelper.class);

    public static final String TOKEN_CLAIM_NAME = "claim.name";
    public static final String TOKEN_CLAIM_NAME_LABEL = "Token Claim Name";
    public static final String JSON_TYPE = "Claim JSON Type";
    public static final String INCLUDE_IN_ACCESS_TOKEN = "access.token.claim";
    public static final String INCLUDE_IN_ACCESS_TOKEN_LABEL = "Add to access token";
    public static final String INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT = "Should the claim be added to the access token?";
    public static final String INCLUDE_IN_ID_TOKEN = "id.token.claim";
    public static final String INCLUDE_IN_ID_TOKEN_LABEL = "Add to ID token";
    public static final String INCLUDE_IN_ID_TOKEN_HELP_TEXT = "Should the claim be added to the ID token?";

    public static Object mapAttributeValue(ProtocolMapperModel mappingModel, Object attributeValue) {
        if (attributeValue == null) return null;

        if (attributeValue instanceof List) {
            List<Object> valueAsList = (List<Object>) attributeValue;
            if (valueAsList.size() == 0) return null;

            if (isMultivalued(mappingModel)) {
                List<Object> result = new ArrayList<>();
                for (Object valueItem : valueAsList) {
                    result.add(mapAttributeValue(mappingModel, valueItem));
                }
                return result;
            } else {
                if (valueAsList.size() > 1) {
                    logger.warnf("Multiple values found '%s' for protocol mapper '%s' but expected just single value", attributeValue.toString(), mappingModel.getName());
                }

                attributeValue = valueAsList.get(0);
            }
        }

        String type = mappingModel.getConfig().get(JSON_TYPE);
        if (type == null) return attributeValue;
        if (type.equals("boolean")) {
            if (attributeValue instanceof Boolean) return attributeValue;
            if (attributeValue instanceof String) return Boolean.valueOf((String)attributeValue);
            throw new RuntimeException("cannot map type for token claim");
        } else if (type.equals("String")) {
            if (attributeValue instanceof String) return attributeValue;
            return attributeValue.toString();
        } else if (type.equals("long")) {
            if (attributeValue instanceof Long) return attributeValue;
            if (attributeValue instanceof String) return Long.valueOf((String)attributeValue);
            throw new RuntimeException("cannot map type for token claim");
        } else if (type.equals("int")) {
            if (attributeValue instanceof Integer) return attributeValue;
            if (attributeValue instanceof String) return Integer.valueOf((String)attributeValue);
            throw new RuntimeException("cannot map type for token claim");
        }
        return attributeValue;
    }

    public static void mapClaim(IDToken token, ProtocolMapperModel mappingModel, Object attributeValue) {
        attributeValue = mapAttributeValue(mappingModel, attributeValue);
        if (attributeValue == null) return;

        String protocolClaim = mappingModel.getConfig().get(TOKEN_CLAIM_NAME);
        String[] split = protocolClaim.split("\\.");
        Map<String, Object> jsonObject = token.getOtherClaims();
        for (int i = 0; i < split.length; i++) {
            if (i == split.length - 1) {
                jsonObject.put(split[i], attributeValue);
            } else {
                Map<String, Object> nested = (Map<String, Object>)jsonObject.get(split[i]);

                if (nested == null) {
                    nested = new HashMap<String, Object>();
                    jsonObject.put(split[i], nested);
                }

                jsonObject = nested;
            }
        }
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                  String userAttribute,
                                  String tokenClaimName, String claimType,
                                  boolean consentRequired, String consentText,
                                  boolean accessToken, boolean idToken,
                                  String mapperId) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConsentRequired(consentRequired);
        mapper.setConsentText(consentText);
        Map<String, String> config = new HashMap<String, String>();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
        config.put(TOKEN_CLAIM_NAME, tokenClaimName);
        config.put(JSON_TYPE, claimType);
        if (accessToken) config.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        return mapper;
    }

    public static boolean includeInIDToken(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(INCLUDE_IN_ID_TOKEN));
    }

    public static boolean includeInAccessToken(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(INCLUDE_IN_ACCESS_TOKEN));
    }


    public static boolean isMultivalued(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(ProtocolMapperUtils.MULTIVALUED));
    }

    public static void addAttributeConfig(List<ProviderConfigProperty> configProperties) {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(TOKEN_CLAIM_NAME);
        property.setLabel(TOKEN_CLAIM_NAME_LABEL);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Name of the claim to insert into the token.  This can be a fully qualified name like 'address.street'.  In this case, a nested json object will be created.");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(JSON_TYPE);
        property.setLabel(JSON_TYPE);
        List<String> types = new ArrayList(3);
        types.add("String");
        types.add("long");
        types.add("int");
        types.add("boolean");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setDefaultValue(types);
        property.setHelpText("JSON type that should be used to populate the json claim in the token.  long, int, boolean, and String are valid values.");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(INCLUDE_IN_ID_TOKEN);
        property.setLabel(INCLUDE_IN_ID_TOKEN_LABEL);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText(INCLUDE_IN_ID_TOKEN_HELP_TEXT);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(INCLUDE_IN_ACCESS_TOKEN);
        property.setLabel(INCLUDE_IN_ACCESS_TOKEN_LABEL);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText(INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT);
        configProperties.add(property);
    }
}
