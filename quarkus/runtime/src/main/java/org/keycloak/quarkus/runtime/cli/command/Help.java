package org.keycloak.quarkus.runtime.cli.command;

import picocli.CommandLine;

@CommandLine.Command(name = Help.NAME, header = Help.HEADER, description = "%n" + Help.HEADER,
        subcommands = {HelpFeatures.class})
public class Help {
    public static final String NAME = "help";
    public static final String HEADER = "Get more help about specific components";
}
