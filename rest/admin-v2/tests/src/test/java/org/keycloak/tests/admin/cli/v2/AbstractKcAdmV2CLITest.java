package org.keycloak.tests.admin.cli.v2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.v2.KcAdmV2Cmd;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.server.KeycloakUrls;

import picocli.CommandLine;

abstract class AbstractKcAdmV2CLITest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    protected CommandResult kcAdmV2Cmd(Path cacheDir, String configFile, String... args) {
        String[] fullArgs = withConfigArg(configFile, args);
        KcAdmV2Cmd cmd = cacheDir != null ? new KcAdmV2Cmd(cacheDir, fullArgs) : new KcAdmV2Cmd(fullArgs);
        return execute(cmd, fullArgs);
    }

    protected CommandResult kcAdmV2CmdNoConfig(String... args) {
        String[] fullArgs = new String[args.length + 1];
        fullArgs[0] = "--no-config";
        System.arraycopy(args, 0, fullArgs, 1, args.length);

        return execute(new KcAdmV2Cmd(fullArgs), fullArgs);
    }

    protected CommandResult kcAdmV2CmdRaw(String... args) {
        return execute(new KcAdmV2Cmd(args), args);
    }

    static CommandResult execute(KcAdmV2Cmd cmd, String[] args) {
        CommandLine cli = Globals.createCommandLine(cmd, KcAdmMain.CMD, new PrintWriter(System.err, true));

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute(args);
        return new CommandResult(exitCode, out.toString(), err.toString());
    }

    static String[] withConfigArg(String configFile, String... args) {
        String[] fullArgs = new String[args.length + 2];
        if (args.length > 0 && "config".equals(args[0])) {
            System.arraycopy(args, 0, fullArgs, 0, args.length);
            fullArgs[args.length] = "--config";
            fullArgs[args.length + 1] = configFile;
        } else {
            fullArgs[0] = "--config";
            fullArgs[1] = configFile;
            System.arraycopy(args, 0, fullArgs, 2, args.length);
        }
        return fullArgs;
    }

    protected String managementBaseUrl() {
        String metricUrl = keycloakUrls.getMetric();
        return metricUrl.substring(0, metricUrl.lastIndexOf("/metrics"));
    }

    record CommandResult(int exitCode, String out, String err) {
    }
}
