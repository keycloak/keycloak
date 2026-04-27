package org.keycloak.client.admin.cli.commands;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.cli.common.BaseAuthOptionsCmd;
import org.keycloak.client.cli.config.ConfigData;

import picocli.CommandLine.Option;

/**
 * Extends {@link BaseAuthOptionsCmd} with token authentication and target realm support.
 */
public abstract class AbstractTargetAuthOptionsCmd extends BaseAuthOptionsCmd implements GlobalOptionsCmdHelper {

    @Option(names = {"-r", "--target-realm"}, description = "Realm to target - when it's different than the realm we authenticate against")
    protected String targetRealm;

    @Option(names = "--token", description = "Token to use for invocations.  With this option set, every other authentication option is ignored")
    public void setToken(String token) {
        this.externalToken = token;
    }

    public AbstractTargetAuthOptionsCmd() {
        super(KcAdmMain.COMMAND_STATE);
    }

    protected String getTargetRealm(ConfigData config) {
        return targetRealm != null ? targetRealm : config.getRealm();
    }
}
