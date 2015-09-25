package org.keycloak.adapters.saml;

import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SamlSession extends Serializable {
    SamlPrincipal getPrincipal();
    Set<String> getRoles();
    String getSessionIndex();
}
