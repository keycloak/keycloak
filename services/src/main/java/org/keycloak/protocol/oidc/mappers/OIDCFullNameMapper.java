package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Set the 'name' claim to be first + last name.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCFullNameMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    static {

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
        return "Full name Mapper";
    }

    @Override
    public AccessToken transformToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String first = user.getFirstName() == null ? "" : user.getFirstName() + " ";
        String last = user.getLastName() == null ? "" : user.getLastName();
        token.getOtherClaims().put("name", first + last);
        return token;
    }

}
