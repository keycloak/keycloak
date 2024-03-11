package org.keycloak.protocol.oid4vc.issuance.mappers;

/**
 * Exception to be used in case anything fails on OID4VP Protocol Mapping
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VPMapperException extends RuntimeException {
    public OID4VPMapperException(String message) {
        super(message);
    }

    public OID4VPMapperException(String message, Throwable cause) {
        super(message, cause);
    }
}