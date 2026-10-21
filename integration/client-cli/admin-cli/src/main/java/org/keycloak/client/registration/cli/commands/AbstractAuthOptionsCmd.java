package org.keycloak.client.registration.cli.commands;

import org.keycloak.client.cli.common.BaseAuthOptionsCmd;
import org.keycloak.client.registration.cli.KcRegMain;

import picocli.CommandLine.Option;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractAuthOptionsCmd extends BaseAuthOptionsCmd {

    @Option(names = {"-t", "--token"}, description = "Initial / Registration access token to use)")
    public void setToken(String token) {
        this.externalToken = token;
    }

    public AbstractAuthOptionsCmd() {
        super(KcRegMain.COMMAND_STATE);
    }

}
