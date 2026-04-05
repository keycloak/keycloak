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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

import org.keycloak.client.admin.cli.commands.KcAdmCmd;
import org.keycloak.client.admin.cli.v2.KcAdmV2Cmd;
import org.keycloak.client.admin.cli.v2.KcAdmV2Completer;
import org.keycloak.client.cli.common.CommandState;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.client.cli.util.OsUtil;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmMain {

    public static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + "/.keycloak/kcadm.config";

    public static final String DEFAULT_CONFIG_FILE_STRING = OsUtil.OS_ARCH.isWindows() ? "%HOMEDRIVE%%HOMEPATH%\\.keycloak\\kcadm.config" : "~/.keycloak/kcadm.config";

    public static final String CMD = OsUtil.OS_ARCH.isWindows() ? "kcadm.bat" : "kcadm.sh";

    public static final CommandState COMMAND_STATE = new CommandState() {

        @Override
        public String getCommand() {
            return CMD;
        }

        @Override
        public String getDefaultConfigFilePath() {
            return DEFAULT_CONFIG_FILE_PATH;
        }

        @Override
        public boolean isTokenGlobal() {
            return true;
        };

    };

    public static final String V2_FLAG = "--v2";

    private static final String COMPLETE_FLAG = "__complete";

    public static void main(String[] args) {
        if (!containsArg(args, V2_FLAG)) {
            Globals.main(args, new KcAdmCmd(), CMD, DEFAULT_CONFIG_FILE_STRING);
            return;
        }

        String[] v2Args = stripArgs(args, V2_FLAG);

        if (containsArg(v2Args, COMPLETE_FLAG)) {
            KcAdmV2Completer.complete(stripArgs(v2Args, COMPLETE_FLAG),
                    new PrintWriter(System.out, true));
        } else {
            Globals.main(v2Args, new KcAdmV2Cmd(), CMD, DEFAULT_CONFIG_FILE_STRING);
        }
    }

    private static boolean containsArg(String[] args, String arg) {
        return Arrays.stream(args).anyMatch(arg::equalsIgnoreCase);
    }

    private static String[] stripArgs(String[] args, String... argsToStrip) {
        Set<String> toStrip = Set.of(argsToStrip);
        return Arrays.stream(args)
                .filter(a -> !toStrip.contains(a.toLowerCase()))
                .toArray(String[]::new);
    }
}
