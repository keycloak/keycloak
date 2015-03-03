package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Set the 'name' claim to be first + last name.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCFullNameMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    static {
        ConfigProperty property;
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

    public static final String PROVIDER_ID = "oidc-full-name-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User's full name";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps the user's first and last name to the OpenID Connect 'name' claim. Format is <first> + ' ' + <last>";
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) return token;
        setClaim(token, userSession);
        return token;
    }

    protected void setClaim(IDToken token, UserSessionModel userSession) {
        UserModel user = userSession.getUser();
        String first = user.getFirstName() == null ? "" : user.getFirstName() + " ";
        String last = user.getLastName() == null ? "" : user.getLastName();
        token.getOtherClaims().put("name", first + last);
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) return token;
        setClaim(token, userSession);
        return token;
    }
}
