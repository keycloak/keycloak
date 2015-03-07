package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Add a role to a token
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCAddRoleMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    public static final String ROLE_CONFIG = "role";

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(ROLE_CONFIG);
        property.setLabel("Role");
        property.setHelpText("Role you want added to the token.  To specify an application role the syntax is appname.approle, i.e. myapp.myrole");
        property.setType(ConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-role-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Add Role";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode any role specify into the token.";
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        String role = mappingModel.getConfig().get(ROLE_CONFIG);
        String appName = null;
        int scopeIndex = role.indexOf('.');
        if (scopeIndex > -1) {
            appName = role.substring(0, scopeIndex);
            role = role.substring(scopeIndex + 1);
            token.addAccess(appName).addRole(role);
        } else {
            AccessToken.Access access = token.getRealmAccess();
            if (access == null) {
                access = new AccessToken.Access();
                token.setRealmAccess(access);
            }
            access.addRole(role);
        }
        return token;
    }
}
