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
package org.keycloak.client.cli.common;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import static org.keycloak.client.cli.util.IoUtil.printOut;

public abstract class BaseGlobalOptionsCmd implements Runnable {

    @Option(names = "--help",
            description = "Print command specific help")
    public void setHelp(boolean help) {
        Globals.help = help;
    }

    @Option(names = "-x",
            description = "Print full stack trace when exiting with error")
    public void setDumpTrace(boolean dumpTrace) {
        Globals.dumpTrace = dumpTrace;
    }

    protected void printHelpIfNeeded() {
        if (Globals.help) {
            printOut(help());
            System.exit(CommandLine.ExitCode.OK);
        } else if (nothingToDo()) {
            printOut(help());
            System.exit(CommandLine.ExitCode.USAGE);
        }
    }

    protected boolean nothingToDo() {
        return false;
    }

    protected abstract String help();

    @Override
    public void run() {
        printHelpIfNeeded();

        checkUnsupportedOptions(getUnsupportedOptions());

        processOptions();

        process();
    }

    protected String[] getUnsupportedOptions() {
        return new String[0];
    }

    protected void processOptions() {

    }

    protected void process() {

    }

    protected void checkUnsupportedOptions(String ... options) {
        if (options.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of argument required");
        }

        for (int i = 0; i < options.length; i++) {
            String name = options[i];
            String value = options[++i];

            if (value != null) {
                throw new IllegalArgumentException("Unsupported option: " + name);
            }
        }
    }

    protected static String booleanOptionForCheck(boolean value) {
        return value ? "true" : null;
    }

}
