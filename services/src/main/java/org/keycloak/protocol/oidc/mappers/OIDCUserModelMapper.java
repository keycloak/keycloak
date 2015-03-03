package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappings UserModel property (the property name of a getter method) to an ID Token claim.  Token claim name can be a full qualified nested object name,
 * i.e. "address.country".  This will create a nested
 * json object within the toke claim.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCUserModelMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {
    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_PROPERTY_LABEL);
        property.setType(ConfigProperty.STRING_TYPE);
        property.setHelpText(ProtocolMapperUtils.USER_MODEL_PROPERTY_HELP_TEXT);
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setLabel(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setType(ConfigProperty.STRING_TYPE);
        property.setHelpText("Name of the claim to insert into the token.  This can be a fully qualified name like 'address.street'.  In this case, a nested json object will be created.");
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(OIDCAttributeMapperHelper.JSON_TYPE);
        property.setLabel(OIDCAttributeMapperHelper.JSON_TYPE);
        property.setType(ConfigProperty.STRING_TYPE);
        property.setDefaultValue(ConfigProperty.STRING_TYPE);
        property.setHelpText("JSON type that should be used to populate the json claim in the token.  long, int, boolean, and String are valid values.");
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN);
        property.setLabel(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_LABEL);
        property.setType(ConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_HELP_TEXT);
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        property.setLabel(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_LABEL);
        property.setType(ConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-usermodel-property-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Property";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a built in user property to a token claim.";
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) return token;
        setClaim(token, mappingModel, userSession);

        return token;
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) return token;
        setClaim(token, mappingModel, userSession);

        return token;
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        UserModel user = userSession.getUser();
        String propertyName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);
        String propertyValue = ProtocolMapperUtils.getUserModelValue(user, propertyName);
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, propertyValue);
    }

    public static void addClaimMapper(RealmModel realm, String name,
                                      String userAttribute,
                                      String tokenClaimName, String claimType,
                                      boolean consentRequired, String consentText,
                                      boolean appliedByDefault,
                                      boolean accessToken, boolean idToken) {
        OIDCAttributeMapperHelper.addClaimMapper(realm, name, userAttribute,
                tokenClaimName, claimType,
                consentRequired, consentText,
                appliedByDefault, accessToken, idToken,
                PROVIDER_ID);
    }


}
