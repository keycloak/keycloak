package org.keycloak.client.registration.cli;

import org.keycloak.client.admin.cli.ExecutionExceptionHandler;
import org.keycloak.client.admin.cli.ShortErrorMessageHandler;
import org.keycloak.client.registration.cli.commands.KcRegCmd;
import org.keycloak.client.registration.cli.util.ClassLoaderUtil;
import org.keycloak.client.registration.cli.util.OsUtil;
import org.keycloak.common.crypto.CryptoIntegration;

import java.io.PrintWriter;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegMain {

    public static void main(String [] args) {
        String libDir = System.getProperty("kc.lib.dir");
        if (libDir == null) {
            throw new RuntimeException("System property kc.lib.dir needs to be set");
        }
        ClassLoader cl = ClassLoaderUtil.resolveClassLoader(libDir);
        Thread.currentThread().setContextClassLoader(cl);

        CryptoIntegration.init(cl);

        CommandLine cli = createCommandLine();
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }

    public static CommandLine createCommandLine() {
        CommandSpec spec = CommandSpec.forAnnotatedObject(new KcRegCmd()).name(OsUtil.CMD);

        CommandLine cmd = new CommandLine(spec);

        cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        cmd.setErr(new PrintWriter(System.err, true));

        return cmd;
    }
}
