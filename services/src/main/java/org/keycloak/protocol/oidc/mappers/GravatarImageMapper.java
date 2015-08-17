package org.keycloak.protocol.oidc.mappers;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
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

    static
    {
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties);
    }

    public static final String MD5  = "MD5";
    public static final String UTF8 = "UTF8";

    public List<ProviderConfigProperty> getConfigProperties()
    {
        return configProperties;
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType()
    {
        return "Gravatar Mapper";
    }

    @Override
    public String getDisplayCategory()
    {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText()
    {
        return "Add the generated gravatar link, based on email, to a token claim.";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession)
    {
        UserModel user = userSession.getUser();
        String propertyName = OIDCLoginProtocolFactory.EMAIL;
        String propertyValue = ProtocolMapperUtils.getUserModelValue(user, propertyName);
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, generateGravatarLink(propertyValue));
    }

    public static ProtocolMapperModel createClaimMapper(String name,
            String userAttribute,
            String tokenClaimName, String claimType,
            boolean consentRequired, String consentText,
            boolean accessToken, boolean idToken)
    {
        return OIDCAttributeMapperHelper.createClaimMapper(name, userAttribute,
                tokenClaimName, claimType,
                consentRequired, consentText,
                accessToken, idToken,
                PROVIDER_ID);
    }

    private String generateGravatarLink(String email)
    {
        String emailHash = generateMD5hash(email.toLowerCase());
        return String.format(GRAVATAR_LINK_FORMAT, emailHash);
    }

    private String generateMD5hash(String value)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance(MD5);
            md5.reset();
            md5.update(value.getBytes(Charset.forName(UTF8)));
            byte[] digest = md5.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            String hashValue = bigInt.toString(16);

            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashValue.length() < 32)
            {
                hashValue = "0" + hashValue;
            }

            return hashValue;
        }
        catch (Exception e)
        {
            return value;
        }
    }
}
