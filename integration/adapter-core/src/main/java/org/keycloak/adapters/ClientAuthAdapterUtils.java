package org.keycloak.adapters;

import java.io.InputStream;
import java.security.PrivateKey;

import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.FindFile;
import org.keycloak.util.KeystoreUtil;
import org.keycloak.util.Time;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAuthAdapterUtils {

    public static String createSignedJWT(KeycloakDeployment deployment) {
        // TODO: Read all the config from KeycloakDeployment and call below
        return null;
    }


    public static String createSignedJWT(String clientId, String realmInfoUrl,
                                         String keystoreFile, String storePassword, String keyPassword, String alias, KeystoreUtil.KeystoreFormat type,
                                         int tokenTimeout) {
        JsonWebToken jwt = createRequestToken(clientId, realmInfoUrl, tokenTimeout);
        PrivateKey privateKey = KeystoreUtil.loadPrivateKeyFromKeystore(keystoreFile, storePassword, keyPassword, alias, type);

        String signedToken = new JWSBuilder()
                .jsonContent(jwt)
                .rsa256(privateKey);

        return signedToken;
    }

    private static JsonWebToken createRequestToken(String clientId, String realmInfoUrl, int tokenTimeout) {
        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(AdapterUtils.generateId());
        reqToken.issuer(clientId);
        reqToken.audience(realmInfoUrl);

        int now = Time.currentTime();
        reqToken.issuedAt(now);
        reqToken.expiration(now + tokenTimeout);
        reqToken.notBefore(now);

        return reqToken;
    }

}
