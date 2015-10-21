package org.keycloak.adapters.spi;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AdapterSessionStore {
    void saveRequest();
    boolean restoreRequest();
}
