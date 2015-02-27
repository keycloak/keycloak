package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.UserClaimSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Set the 'name' claim to be first + last name.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCAddressMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    static {

    }

    public static final String PROVIDER_ID = "oidc-address-mapper";


    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Address";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps user address attributes (street, locality, region, postal_code, and country) to the OpenID Connect 'address' claim.";
    }

    @Override
    public AccessToken transformToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        UserClaimSet.AddressClaimSet addressSet = new UserClaimSet.AddressClaimSet();
        addressSet.setStreetAddress(user.getAttribute("street"));
        addressSet.setLocality(user.getAttribute("locality"));
        addressSet.setRegion(user.getAttribute("region"));
        addressSet.setPostalCode(user.getAttribute("postal_code"));
        addressSet.setCountry(user.getAttribute("country"));
        token.getOtherClaims().put("address", addressSet);
        return token;
    }

}
