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

import org.keycloak.client.admin.cli.commands.KcAdmCmd;
import org.keycloak.client.admin.cli.util.ClassLoaderUtil;
import org.keycloak.client.admin.cli.util.OsUtil;
import org.keycloak.common.crypto.CryptoIntegration;

import java.io.PrintWriter;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmMain {

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
        CommandSpec spec = CommandSpec.forAnnotatedObject(new KcAdmCmd()).name(OsUtil.CMD);

        CommandLine cmd = new CommandLine(spec);

        cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        cmd.setErr(new PrintWriter(System.err, true));

        return cmd;
    }

}
