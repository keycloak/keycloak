package org.keycloak.it.windows;

import org.keycloak.it.TestProvider;

public class WindowsServiceStartupTestProvider implements TestProvider {
    @Override
    public Class<?>[] getClasses() {
        return new Class<?>[] { WindowsServiceStartupGate.class };
    }
}
