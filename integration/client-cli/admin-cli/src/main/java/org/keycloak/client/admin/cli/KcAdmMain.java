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
package org.keycloak.client.admin.cli;

import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.keycloak.client.admin.cli.aesh.AeshEnhancer;
import org.keycloak.client.admin.cli.aesh.Globals;
import org.keycloak.client.admin.cli.aesh.ValveInputStream;
import org.keycloak.client.admin.cli.commands.KcAdmCmd;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmMain {

    public static void main(String [] args) {

        Globals.stdin = new ValveInputStream();

        Settings settings = new SettingsBuilder()
                .logging(false)
                .readInputrc(false)
                .disableCompletion(true)
                .disableHistory(true)
                .enableAlias(false)
                .enableExport(false)
                .inputStream(Globals.stdin)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(KcAdmCmd.class)
                .create();

        AeshConsoleImpl console = (AeshConsoleImpl) new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""))
//                .commandInvocationProvider(new CommandInvocationServices() {
//
//                })
                .create();

        AeshEnhancer.enhance(console);

        // work around parser issues with quotes and brackets
        ArrayList<String> arguments = new ArrayList<>();
        arguments.add("kcadm");
        arguments.addAll(Arrays.asList(args));
        Globals.args = arguments;

        StringBuilder b = new StringBuilder();
        for (String s : args) {
            // quote if necessary
            boolean needQuote = false;
            needQuote = s.indexOf(' ') != -1 || s.indexOf('\"') != -1 || s.indexOf('\'') != -1;
            b.append(' ');
            if (needQuote) {
                b.append('\'');
            }
            b.append(s);
            if (needQuote) {
                b.append('\'');
            }
        }
        console.setEcho(false);

        console.execute("kcadm" + b.toString());

        console.start();
    }
}
