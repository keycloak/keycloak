package org.keycloak.protocol.oid4vc.model;


/**
 * Enum to handle potential errors in issuing credentials
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public enum ProofType {

    JWT("jwt"),
    LD_PROOF("ld_proof");

    private final String value;

    ProofType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}