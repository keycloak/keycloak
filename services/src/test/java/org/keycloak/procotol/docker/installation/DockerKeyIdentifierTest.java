package org.keycloak.procotol.docker.installation;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.utils.Base32;
import org.keycloak.protocol.docker.DockerKeyIdentifier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Docker gets really unhappy if the key identifier is not in the format documented here:
 * @see https://github.com/docker/libtrust/blob/master/key.go#L24
 */
public class DockerKeyIdentifierTest {

    String keyIdentifierString;
    PublicKey publicKey;

    @Before
    public void shouldBlah() throws Exception {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());

        final KeyPair keypair = keyGen.generateKeyPair();
        publicKey = keypair.getPublic();
        final DockerKeyIdentifier identifier = new DockerKeyIdentifier(publicKey);
        keyIdentifierString = identifier.toString();
    }

    @Test
    public void shoulProduceExpectedKeyFormat() {
        assertThat("Every 4 chars are not delimted by colon", keyIdentifierString.matches("([\\w]{4}:){11}[\\w]{4}"), equalTo(true));
    }
}
