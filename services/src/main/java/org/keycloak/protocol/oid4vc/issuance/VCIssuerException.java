package org.keycloak.protocol.oid4vc.issuance;

/**
 * Exception to be thrown in case credentials issuance fails.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class VCIssuerException extends RuntimeException {

	public VCIssuerException(String message) {
		super(message);
	}

	public VCIssuerException(String message, Throwable cause) {
		super(message, cause);
	}
}
