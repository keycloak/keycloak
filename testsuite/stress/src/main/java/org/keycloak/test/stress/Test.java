package org.keycloak.test.stress;

import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Test extends Callable<Boolean> {
    void init();
}
