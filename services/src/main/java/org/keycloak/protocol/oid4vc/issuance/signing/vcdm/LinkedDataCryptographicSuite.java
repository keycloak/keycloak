package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

/**
 * Interface for all implementations of LD-Signature Suites
 * <p>
 * {@see https://w3c-ccg.github.io/ld-cryptosuite-registry/}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public interface LinkedDataCryptographicSuite {

    byte[] getSignature(VerifiableCredential verifiableCredential);

    String getProofType();

}