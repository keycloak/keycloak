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

    public static void main(String [] args) {
        Globals.main(args, new KcAdmCmd(), CMD, DEFAULT_CONFIG_FILE_STRING);
    }

}
