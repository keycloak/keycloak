package org.keycloak.jose.jwk;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.crypto.KeyType;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public abstract class JWKParserTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void toPublicKey_JWK_EC() throws IOException {

        JWK jwk = JsonSerialization.readValue("{\n" +
                                              "    \"kty\": \"EC\",\n" +
                                              "    \"use\": \"sig\",\n" +
                                              "    \"crv\": \"P-384\",\n" +
                                              "    \"kid\": \"KTGEM0qFeO9VGjTLjmXiE_R_eSBUkU87xmytygI1pFQ\",\n" +
                                              "    \"x\": \"_pYSppQj0JkrXFQdJPOTiktUxy_giDnqc-PEmNShrWrZm8Ol6E5qB3m1kmZJ7HUF\",\n" +
                                              "    \"y\": \"BVlstiJytsgOxrsC1VuNYdx86KKMeJg5WvJhEi-5kMpF2aMHZqbJCcIq0uRdzi7Q\",\n" +
                                              "    \"alg\": \"ES256\"\n" +
                                              "}", JWK.class);

        JWKParser sut = JWKParser.create(jwk);

        Assert.assertNotNull(sut.toPublicKey());
    }

    @Test
    public void toPublicKey_JWK_RSA() throws IOException {

        JWK jwk = JsonSerialization.readValue("{\n" +
                                    "    \"kty\": \"RSA\",\n" +
                                    "    \"e\": \"AQAB\",\n" +
                                    "    \"use\": \"sig\",\n" +
                                    "    \"kid\": \"8UXSEBJiCBZ_QxzFh8MdOKpJOv3M4WxgqFlfeE1bUtc\",\n" +
                                    "    \"alg\": \"RS256\",\n" +
                                    "    \"n\": \"sksCls5hDkkBTzWe4rydVxNvfbl6gF2IULzMilLxkYCVGj41IU-kGPzleIaEB5mhtFhsvKb9Z0F_BOi68gOEO9fx-Dj1sN3z-KySf4LOKToo5T3hP0sd1-Zc0qT51Mu90n3Qa8nGHH9KjNL-O4oQx9I3gfHuJZOe3FQHLqTBMlYTnhtpAdqs6nrusTy3jUkVx020h0vgs4zmbmHw4PYloH2yCPIrx1nX0l5nWvDj_epBsnW5HHp08Ua655K_RtjfpePmO6N_yDTNCs2bzmgU2EIJ7CpGQzfjbIjai4YtixlBMqBWEWtAIckOSNIUi06c0P3kW1-8fkj1tHGQnUet3w\"\n" +
                                    "}", JWK.class);

        JWKParser sut = JWKParser.create(jwk);

        Assert.assertNotNull(sut.toPublicKey());
    }

    @Test
    public void toPublicKey_RSA() {

        RSAPublicJWK rsaJwk = new RSAPublicJWK();
        rsaJwk.setKeyType(KeyType.RSA);
        rsaJwk.setAlgorithm("RS256");
        rsaJwk.setModulus("sksCls5hDkkBTzWe4rydVxNvfbl6gF2IULzMilLxkYCVGj41IU-kGPzleIaEB5mhtFhsvKb9Z0F_BOi68gOEO9fx-Dj1sN3z-KySf4LOKToo5T3hP0sd1-Zc0qT51Mu90n3Qa8nGHH9KjNL-O4oQx9I3gfHuJZOe3FQHLqTBMlYTnhtpAdqs6nrusTy3jUkVx020h0vgs4zmbmHw4PYloH2yCPIrx1nX0l5nWvDj_epBsnW5HHp08Ua655K_RtjfpePmO6N_yDTNCs2bzmgU2EIJ7CpGQzfjbIjai4YtixlBMqBWEWtAIckOSNIUi06c0P3kW1-8fkj1tHGQnUet3w");
        rsaJwk.setPublicExponent("AQAB");

        JWKParser sut = JWKParser.create(rsaJwk);

        Assert.assertNotNull(sut.toPublicKey());
    }

    @Test
    public void toPublicKey_EC() {

        ECPublicJWK ecJwk = new ECPublicJWK();
        ecJwk.setKeyType(KeyType.EC);
        ecJwk.setCrv("P-256");
        ecJwk.setX("zHXlTZt3yU_oNnLIjgpt-ZaiStrYIzR2oxxq53J0uIs");
        ecJwk.setY("cOsAvnh6olE8KHWPHmB-pJawRWmTtbChmWtSeWZRJdc");

        JWKParser sut = JWKParser.create(ecJwk);

        Assert.assertNotNull(sut.toPublicKey());
    }
}
