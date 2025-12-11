package org.keycloak.client.admin.cli.commands;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.keycloak.client.cli.common.Globals;

import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;

public class KcAdmCliTest {

    @Test
    public void testAtFileDisabled() {
        KcAdmCmd command = new KcAdmCmd();
        CommandLine cli = Globals.createCommandLine(command, "kcadm", new PrintWriter(new ByteArrayOutputStream()));
        cli.execute("config", "credentials", "--user", "user", "--password", "@@password", "--server", "server");
        ConfigCredentialsCmd cmd = command.spec.subcommands().get(ConfigCmd.NAME).getSubcommands().get(ConfigCredentialsCmd.NAME).getCommand();
        assertEquals("@@password", cmd.getPassword());
    }
}