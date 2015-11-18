package org.keycloak.client.registration.cli;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationCLI {

    public static void main(String[] args) {

        Settings settings = new SettingsBuilder().logging(true).create();
        AeshConsole aeshConsole = new AeshConsoleBuilder().settings(settings)
                .prompt(new Prompt("[aesh@rules]$ "))
//                .command()
                .create();

        aeshConsole.start();
    }


}

