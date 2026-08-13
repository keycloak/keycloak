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

package org.keycloak.client.registration.cli.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import picocli.CommandLine.Command;

import static org.keycloak.client.registration.cli.KcRegMain.CMD;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@Command(name = "config", description = "COMMAND [ARGUMENTS]", subcommands = {
        ConfigCredentialsCmd.class,
        ConfigInitialTokenCmd.class,
        ConfigRegistrationTokenCmd.class,
        ConfigTruststoreCmd.class
})
public class ConfigCmd extends AbstractAuthOptionsCmd {

    @Override
    protected void process() {

    }

    @Override
    protected boolean nothingToDo() {
        return true;
    }

    @Override
    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " config SUB_COMMAND [ARGUMENTS]");
        out.println();
        out.println("Where SUB_COMMAND is one of: 'credentials', 'truststore', 'initial-token', 'registration-token'");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help config SUB_COMMAND' for more info.");
        out.println("Use '" + CMD + " help' for general information and a list of commands.");
        return sb.toString();
    }
}
