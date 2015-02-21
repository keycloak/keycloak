package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mappings UserModel.attribute to an ID Token claim.  Token claim name can be a full qualified nested object name,
 * i.e. "address.country".  This will create a nested
 * json object within the toke claim.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCUserAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    public static final String TOKEN_CLAIM_NAME = "Token Claim Name";
    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();
    public static final String USER_MODEL_ATTRIBUTE_NAME = "UserModel Attribute Name";

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(USER_MODEL_ATTRIBUTE_NAME);
        property.setLabel(USER_MODEL_ATTRIBUTE_NAME);
        property.setHelpText("Name of stored user attribute which is the name of an attribute within the UserModel.attribute map.");
        configProperties.add(property);
        property.setName(TOKEN_CLAIM_NAME);
        property.setLabel(TOKEN_CLAIM_NAME);
        property.setHelpText("Name of the claim to insert into the token.  This can be a fully qualified name like 'address.street'.  In this case, a nested json object will be created.");
        configProperties.add(property);

    }


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return "oidc-usermodel-attribute-mapper";
    }

    @Override
    public String getDisplayType() {
        return "UserModel Attribute Mapper";
    }

    @Override
    public AccessToken transformToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String attributeName = mappingModel.getConfig().get(USER_MODEL_ATTRIBUTE_NAME);
        String attributeValue = user.getAttribute(attributeName);
        if (attributeValue == null) return token;
        mapClaim(token, mappingModel, attributeValue);
        return token;
    }

    protected static void mapClaim(AccessToken token, ProtocolMapperModel mappingModel, String attributeValue) {
        if (attributeValue == null) return;
        String protocolClaim = mappingModel.getConfig().get(TOKEN_CLAIM_NAME);
        String[] split = protocolClaim.split(".");
        Map<String, Object> jsonObject = token.getOtherClaims();
        for (int i = 0; i < split.length; i++) {
            if (i == split.length - 1) {
                jsonObject.put(split[i], attributeValue);
            } else {
                Map<String, Object> nested = (Map<String, Object>)jsonObject.get(split[i]);
                if (nested == null) {
                    nested = new HashMap<String, Object>();
                    jsonObject.put(split[i], nested);
                    jsonObject = nested;
                }
            }
        }
    }
}
