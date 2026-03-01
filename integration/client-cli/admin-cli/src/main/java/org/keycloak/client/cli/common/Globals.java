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

import java.io.PrintWriter;

import org.keycloak.client.cli.util.ClassLoaderUtil;
import org.keycloak.common.crypto.CryptoIntegration;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class Globals {

    public static boolean dumpTrace = false;

    public static boolean help = false;

    public static void main(String [] args, BaseGlobalOptionsCmd rootCommand, String command, String defaultConfigFile) {
        String libDir = System.getProperty("kc.lib.dir");
        if (libDir == null) {
            throw new RuntimeException("System property kc.lib.dir needs to be set");
        }
        ClassLoader cl = ClassLoaderUtil.resolveClassLoader(libDir);
        Thread.currentThread().setContextClassLoader(cl);

        CryptoIntegration.init(cl);

        System.setProperty(BaseAuthOptionsCmd.DEFAULT_CONFIG_PATH_STRING_KEY, defaultConfigFile);
        CommandLine cli = createCommandLine(rootCommand, command, new PrintWriter(System.err, true));
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }

    public static CommandLine createCommandLine(BaseGlobalOptionsCmd rootCommand, String command, PrintWriter errorWriter) {
        CommandSpec spec = CommandSpec.forAnnotatedObject(rootCommand).name(command);

        CommandLine cmd = new CommandLine(spec);
        cmd.setExpandAtFiles(false);
        cmd.setPosixClusteredShortOptionsAllowed(false);
        cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        cmd.setErr(errorWriter);

        return cmd;
    }


}
