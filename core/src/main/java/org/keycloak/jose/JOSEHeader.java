package org.keycloak.jose;

import java.io.Serializable;

import org.keycloak.jose.jws.Algorithm;

/**
 * This interface represents a JOSE header.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface JOSEHeader extends Serializable {

    /**
     * Returns the algorithm used to sign or encrypt the JWT from the JOSE header.
     *
     * @return the algorithm from the JOSE header
     */
    String getRawAlgorithm();

    String getKeyId();
}
