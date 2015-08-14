package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

public class GravatarImageMapper extends UserAttributeMapper
{
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String GRAVATAR_LINK_FORMAT = "https://gravatar.com/avatar/%s?d=mm";

    public static final String PROVIDER_ID = "oidc-email-to-gravatar-link-mapper";

    static {
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties);
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Gravatar Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Add the generated gravatar link, based on email, to a token claim.";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        UserModel user = userSession.getUser();
        String propertyName = OIDCLoginProtocolFactory.EMAIL;
        String propertyValue = ProtocolMapperUtils.getUserModelValue(user, propertyName);
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, generateGravatarLink(propertyValue));
    }

    public static ProtocolMapperModel createClaimMapper(String name,
            String userAttribute,
            String tokenClaimName, String claimType,
            boolean consentRequired, String consentText,
            boolean accessToken, boolean idToken) {
        return OIDCAttributeMapperHelper.createClaimMapper(name, userAttribute,
                tokenClaimName, claimType,
                consentRequired, consentText,
                accessToken, idToken,
                PROVIDER_ID);
    }

    private String generateGravatarLink(String email)
    {
        String emailHash = DigestUtils.md5Hex(email.toLowerCase());
        return String.format(GRAVATAR_LINK_FORMAT, emailHash);
    }


}
