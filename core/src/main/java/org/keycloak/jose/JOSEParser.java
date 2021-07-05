package org.keycloak.jose;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JOSEParser {

    /**
     * Parses the given encoded {@code jwt} and returns either a {@link JWSInput} or {@link JWE}
     * depending on the JOSE header configuration.
     *
     * @param jwt the encoded JWT
     * @return a {@link JOSE}
     */
    public static JOSE parse(String jwt) {
        String[] parts = jwt.split("\\.");

        if (parts.length == 0) {
            throw new RuntimeException("Could not infer header from JWT");
        }

        JsonNode header;

        try {
            header = JsonSerialization.readValue(Base64Url.decode(parts[0]), JsonNode.class);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to parse JWT header", cause);
        }

        if (header.has("enc")) {
            return new JWE(jwt);
        }

        try {
            return new JWSInput(jwt);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to build JWS", cause);
        }
    }
}
