package org.keycloak.protocol.oid4vc.issuance;

/**
 * Interface to provide the current time
 */
public interface TimeProvider {

    /**
     * Returns current time in seconds
     *
     * @return see description
     */
    int currentTime();

    /**
     * Returns current time in milliseconds
     *
     * @return see description
     */
    long currentTimeMillis();

}
