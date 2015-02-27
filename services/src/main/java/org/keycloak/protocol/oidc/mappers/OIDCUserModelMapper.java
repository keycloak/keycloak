package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

import java.lang.reflect.Method;
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
public class OIDCUserModelMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {
    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();
    public static final String USER_MODEL_PROPERTY = "UserModel Property";

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(USER_MODEL_PROPERTY);
        property.setLabel(USER_MODEL_PROPERTY);
        property.setHelpText("Name of the property method in the UserModel interface.  For example, a value of 'email' would reference the UserModel.getEmail() method.");
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(AttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setLabel(AttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setHelpText("Name of the claim to insert into the token.  This can be a fully qualified name like 'address.street'.  In this case, a nested json object will be created.");
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
        return "UserModel Property Mapper";
    }

    @Override
    public AccessToken transformToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String propertyName = mappingModel.getConfig().get(USER_MODEL_PROPERTY);
        String propertyValue = getUserModelValue(user,propertyName);
        AttributeMapperHelper.mapClaim(token, mappingModel, propertyValue);

        return token;
    }

    protected String getUserModelValue(UserModel user, String propertyName) {

        String methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        try {
            Method method = UserModel.class.getMethod(methodName);
            Object val = method.invoke(user);
            if (val != null) return val.toString();
        } catch (Exception ignore) {

        }
        methodName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        try {
            Method method = UserModel.class.getMethod(methodName);
            Object val = method.invoke(user);
            if (val != null) return val.toString();
        } catch (Exception ignore) {

        }
        return null;
    }
}
