package org.keycloak.testsuite.arquillian.karaf;

import org.jboss.arquillian.container.osgi.karaf.managed.KarafManagedContainerConfiguration;

/**
 *
 * @author tkyjovsk
 */
public class CustomKarafContainerConfiguration extends KarafManagedContainerConfiguration {

    private String commandsAfterStart;

    public String getCommandsAfterStart() {
        return commandsAfterStart;
    }

    public String[] getCommandsAfterStartAsArray() {
        return getCommandsAfterStart().trim().split(",");
    }

    public void setCommandsAfterStart(String commandsAfterStart) {
        this.commandsAfterStart = commandsAfterStart;
    }

}
