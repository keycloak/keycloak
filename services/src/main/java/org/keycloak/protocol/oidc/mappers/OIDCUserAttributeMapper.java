package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.representations.AccessToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappings UserModel.attribute to an ID Token claim.  Token claim name can be a full qualified nested object name,
 * i.e. "address.country".  This will create a nested
 * json object within the toke claim.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCUserAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_HELP_TEXT);
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setLabel(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setHelpText("Name of the claim to insert into the token.  This can be a fully qualified name like 'address.street'.  In this case, a nested json object will be created.");
        configProperties.add(property);

    }

    public static final String PROVIDER_ID = "oidc-usermodel-attribute-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Attribute";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a custom user attribute to a token claim.";
    }

    @Override
    public AccessToken transformToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String attributeName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);
        String attributeValue = user.getAttribute(attributeName);
        if (attributeValue == null) return token;
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeValue);
        return token;
    }

    public static void addClaimMapper(RealmModel realm, String name,
                                      String userAttribute,
                                      String tokenClaimName, String claimType,
                                      boolean consentRequired, String consentText,
                                      boolean appliedByDefault) {
        OIDCAttributeMapperHelper.addClaimMapper(realm, name, userAttribute,
                tokenClaimName, claimType,
                consentRequired, consentText,
                appliedByDefault, PROVIDER_ID);
    }


}
