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

import org.keycloak.client.cli.config.ConfigData;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static org.keycloak.client.admin.cli.KcAdmMain.CMD;
import static org.keycloak.client.admin.cli.operations.UserOperations.getIdFromUsername;
import static org.keycloak.client.admin.cli.operations.UserOperations.resetUserPassword;
import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;
import static org.keycloak.common.util.IoUtils.readPasswordFromConsole;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "set-password", description = "[ARGUMENTS]")
public class SetPasswordCmd extends AbstractAuthOptionsCmd {

    @Option(names = "--username", description = "Username")
    String username;

    @Option(names = "--userid", description = "User ID")
    String userid;

    @Option(names = {"-p", "--new-password"}, description = "New password", defaultValue = "${env:KC_CLI_PASSWORD}")
    String pass;

    @Option(names = {"-t", "--temporary"}, description = "is password temporary")
    boolean temporary;

    @Override
    protected void process() {
        if (userid == null && username == null) {
            throw new IllegalArgumentException("No user specified. Use --username or --userid to specify user");
        }

        if (userid != null && username != null) {
            throw new IllegalArgumentException("Options --userid and --username are mutually exclusive");
        }

        if (pass == null) {
            pass = readPasswordFromConsole("password");
        }

        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);

        setupTruststore(config);

        String auth = null;

        config = ensureAuthInfo(config);
        config = copyWithServerInfo(config);
        if (credentialsAvailable(config)) {
            auth = ensureToken(config);
        }

        auth = auth != null ? "Bearer " + auth : null;

        final String server = config.getServerUrl();
        final String realm = getTargetRealm(config);
        final String adminRoot = adminRestRoot != null ? adminRestRoot : composeAdminRoot(server);

        // if username is specified resolve id
        if (username != null) {
            userid = getIdFromUsername(adminRoot, realm, auth, username);
        }

        resetUserPassword(adminRoot, realm, auth, userid, pass, temporary);
    }

    @Override
    protected boolean nothingToDo() {
        return super.nothingToDo() && username == null && userid == null && pass == null;
    }

    @Override
    protected String help() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " set-password (--username USERNAME | --userid ID) [--new-password PASSWORD] [ARGUMENTS]");
        out.println();
        out.println("Command to reset user's password.");
        out.println();
        out.println("Use `" + CMD + " config credentials` to establish an authenticated session, or use CREDENTIALS OPTIONS");
        out.println("to perform one time authentication.");
        globalOptions(out);
        out.println("    --username USERNAME       Identify target user by 'username'");
        out.println("    --userid ID               Identify target user by 'id'");
        out.println("    -p, --new-password        New password to set. If not specified and the env variable KC_CLI_PASSWORD is not defined, you will be prompted for it.");
        out.println("    -t, --temporary           Make the new password temporary - user has to change it on next logon");
        out.println("    -a, --admin-root URL      URL of Admin REST endpoint root if not default - e.g. http://localhost:8080/admin");
        out.println("    -r, --target-realm REALM  Target realm to issue requests against if not the one authenticated against");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Set new temporary password for the user:");
        out.println("  " + PROMPT + " " + CMD + " set-password -r demorealm --username testuser --new-password NEWPASS -t");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }

}
