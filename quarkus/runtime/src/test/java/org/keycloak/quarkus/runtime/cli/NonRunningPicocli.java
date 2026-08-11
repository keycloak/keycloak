package org.keycloak.quarkus.runtime.cli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.KeycloakMain;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;

import io.smallrye.config.SmallRyeConfig;

// TODO: could utilize CLIResult
class NonRunningPicocli extends Picocli {

    final StringWriter err = new StringWriter();
    final StringWriter out = new StringWriter();
    SmallRyeConfig config;
    int exitCode = Integer.MAX_VALUE;
    boolean reaug;
    private Properties buildProps;

    String getErrString() {
        return normalize(err);
    }

    private String normalize(StringWriter writer) {
        return System.lineSeparator().equals("\n") ? writer.toString()
                : writer.toString().replace(System.lineSeparator(), "\n");
    }

    String getOutString() {
        return normalize(out);
    }

    @Override
    public PrintWriter getErrWriter() {
        return new PrintWriter(err, true);
    }

    @Override
    public PrintWriter getOutWriter() {
        return new PrintWriter(out, true);
    }

    @Override
    public void exit(int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public void start() {
    }

    @Override
    public void initConfig(AbstractCommand command) {
        boolean checkBuild = Environment.isRebuildCheck();
        super.initConfig(command);
        if (!checkBuild && PersistedConfigSource.getInstance().getConfigValueProperties().isEmpty()) {
            System.getProperties().remove(Environment.KC_CONFIG_REBUILD_CHECK);
        }
        config = Configuration.getConfig();
    }

    @Override
    public void build() throws Throwable {
        reaug = true;
        this.buildProps = getNonPersistedBuildTimeOptions();
    }

    Properties getBuildProps() {
        return buildProps;
    }

    NonRunningPicocli launch(String... args) {
        KeycloakMain.main(args, this);
        return this;
    }
}
