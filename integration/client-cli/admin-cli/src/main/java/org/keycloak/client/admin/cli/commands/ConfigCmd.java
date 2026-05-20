/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.client.admin.cli.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import picocli.CommandLine.Command;

import static org.keycloak.client.admin.cli.KcAdmMain.CMD;
import static org.keycloak.client.admin.cli.KcAdmMain.V2_FLAG;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@Command(name = ConfigCmd.NAME, description = "COMMAND [ARGUMENTS]", subcommands = {
        ConfigCredentialsCmd.class,
        ConfigTruststoreCmd.class
} )
public class ConfigCmd extends AbstractAuthOptionsCmd {

    public static final String NAME = "config";

    private final boolean v2;

    public ConfigCmd() {
        this.v2 = false;
    }

    public ConfigCmd(boolean v2) {
        this.v2 = v2;
    }

    @Override
    protected void process() {

    }

    @Override
    protected boolean nothingToDo() {
        return true;
    }

    @Override
    protected String help() {
        return usage(v2);
    }

    public static String usage() {
        return usage(false);
    }

    private static String usage(boolean v2) {
        String cmd = v2 ? CMD + " " + V2_FLAG : CMD;
        String subcommands = v2 ? "'credentials', 'truststore', 'openapi'" : "'credentials', 'truststore'";
        String helpHint = v2 ? cmd + " config SUB_COMMAND --help" : CMD + " help config SUB_COMMAND";

        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + cmd + " config SUB_COMMAND [ARGUMENTS]");
        out.println();
        out.println("Where SUB_COMMAND is one of: " + subcommands);
        out.println();
        out.println();
        out.println("Use '" + helpHint + "' for more info.");
        out.println("Use '" + cmd + " help' for general information and a list of commands.");
        return sb.toString();
    }
}
