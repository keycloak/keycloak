package org.keycloak.adapters.saml.config.parsers;

import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ResourceLoader {
    InputStream getResourceAsStream(String resource);
}
