package org.keycloak.adapters.authentication;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;

/**
 * Client authentication based on JWT signed by client secret instead of private key .
 * See <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">specs</a> for more details.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class JWTClientSecretCredentialsProvider implements ClientCredentialsProvider {
    
	private static final Logger logger = Logger.getLogger(JWTClientSecretCredentialsProvider.class);
	
    public static final String PROVIDER_ID = "secret-jwt";
    
    private SecretKey clientSecret;
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public void init(KeycloakDeployment deployment, Object config) {
        if (!(config instanceof Map)) {
            throw new RuntimeException("Configuration of jwt credentials by client secret is missing or incorrect for client '" + deployment.getResourceName() + "'. Check your adapter configuration");
        }
        
        Map<String, Object> cfg = (Map<String, Object>) config;
        String clientSecretString = (String) cfg.get("secret");
        if (clientSecretString == null) {
            throw new RuntimeException("Missing parameter secret-jwt in configuration of jwt for client " + deployment.getResourceName());
        }
        setClientSecret(clientSecretString); 
    }
    
    @Override
    public void setClientCredentials(KeycloakDeployment deployment, Map<String, String> requestHeaders, Map<String, String> formParams) {
        String signedToken = createSignedRequestToken(deployment.getResourceName(), deployment.getRealmInfoUrl());
        formParams.put(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);
        formParams.put(OAuth2Constants.CLIENT_ASSERTION, signedToken);
    }
   
    public void setClientSecret(String clientSecretString) {
        // Get client secret and validate signature
        // According to <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">OIDC's client authentication spec</a>,
        // The HMAC (Hash-based Message Authentication Code) is calculated using the octets of the UTF-8 representation of the client_secret as the shared key. 
        // Use "HmacSHA256" consulting <a href="https://docs.oracle.com/javase/jp/8/docs/api/javax/crypto/Mac.html">java8 api</a>
        // because it must be implemented in every java platform.
        clientSecret = new SecretKeySpec(clientSecretString.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
    
    public String createSignedRequestToken(String clientId, String realmInfoUrl) {
        JsonWebToken jwt = createRequestToken(clientId, realmInfoUrl);
        // JOSE header {"alg":"HS256","typ" : "JWT"} no need "kid" due to using only one registered client secret.
        // Use "HmacSHA256" consulting <a href="https://docs.oracle.com/javase/jp/8/docs/api/javax/crypto/Mac.html">java8 api</a>.
        // because it must be implemented in every java platform.
        return new JWSBuilder().jsonContent(jwt).hmac256(clientSecret);
    }

    private JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
        // According to <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">OIDC's client authentication spec</a>,
        // JWT claims is the same as one by private_key_jwt
        
        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(AdapterUtils.generateId());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);
        reqToken.audience(realmInfoUrl);

        int now = Time.currentTime();
        reqToken.issuedAt(now);
        // the same as in KEYCLOAK-2986, JWTClientCredentialsProvider's timeout field
        reqToken.expiration(now + 10);
        reqToken.notBefore(now);
        return reqToken;
    }

}
