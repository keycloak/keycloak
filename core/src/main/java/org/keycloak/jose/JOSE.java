package org.keycloak.jose;

/**
 * An interface to represent signed (JWS) and encrypted (JWE) JWTs.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface JOSE {

    /**
     * Returns the JWT header.
     *
     * @return the JWT header
     */
    <H extends JOSEHeader> H getHeader();
}
