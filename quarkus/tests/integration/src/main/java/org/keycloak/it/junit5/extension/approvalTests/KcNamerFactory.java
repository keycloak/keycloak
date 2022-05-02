package org.keycloak.it.junit5.extension.approvalTests;

import org.approvaltests.namer.NamedEnvironment;
import org.approvaltests.namer.NamerFactory;

public class KcNamerFactory extends NamerFactory {

    public static NamedEnvironment asWindowsOsSpecificTest()
    {
        return asMachineSpecificTest(new WindowsOrUnixOsEnvironmentLabeller());
    }
}
