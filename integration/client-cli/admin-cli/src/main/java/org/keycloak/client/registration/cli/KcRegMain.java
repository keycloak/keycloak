package org.keycloak.client.registration.cli;

import org.keycloak.client.cli.common.CommandState;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.client.cli.util.OsUtil;
import org.keycloak.client.registration.cli.commands.KcRegCmd;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegMain {

    public static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + "/.keycloak/kcreg.config";

    public static final String DEFAULT_CONFIG_FILE_STRING = OsUtil.OS_ARCH.isWindows() ? "%HOMEDRIVE%%HOMEPATH%\\.keycloak\\kcreg.config" : "~/.keycloak/kcreg.config";

    public static final String CMD = OsUtil.OS_ARCH.isWindows() ? "kcreg.bat" : "kcreg.sh";

    public static final CommandState COMMAND_STATE = new CommandState() {

        @Override
        public String getCommand() {
            return CMD;
        }

        @Override
        public String getDefaultConfigFilePath() {
            return DEFAULT_CONFIG_FILE_PATH;
        }

        @Override
        public boolean isTokenGlobal() {
            return false;
        };

    };

    public static void main(String [] args) {
        Globals.main(args, new KcRegCmd(), CMD, DEFAULT_CONFIG_FILE_STRING);
    }
}
