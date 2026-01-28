package org.keycloak.client.registration.cli.commands;

import org.keycloak.client.cli.common.BaseConfigCredentialsCmd;
import org.keycloak.client.registration.cli.KcRegMain;

import picocli.CommandLine.Command;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "credentials", description = "--server SERVER_URL --realm REALM [ARGUMENTS]")
public class ConfigCredentialsCmd extends BaseConfigCredentialsCmd {

    public ConfigCredentialsCmd() {
        super(KcRegMain.COMMAND_STATE);
    }

}
