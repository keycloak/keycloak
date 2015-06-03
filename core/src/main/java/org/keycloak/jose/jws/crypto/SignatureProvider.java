package org.keycloak.jose.jws.crypto;

import org.keycloak.jose.jws.JWSInput;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SignatureProvider {
    boolean verify(JWSInput input, String key);
}
