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
import static org.keycloak.client.cli.util.OsUtil.PROMPT;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "delete", description = "CLIENT [GLOBAL_OPTIONS]")
public class DeleteCmd extends CreateCmd {

    public DeleteCmd() {
        this.httpVerb = "delete";
    }

    @Override
    protected String help() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " delete ENDPOINT_URI [ARGUMENTS]");
        out.println();
        out.println("Command to delete resources on the server.");
        out.println();
        out.println("Use '" + CMD + " config credentials' to establish an authenticated sessions, or use CREDENTIALS OPTIONS");
        out.println("to perform one time authentication.");
        globalOptions(out);
        out.println("    ENDPOINT_URI              URI used to compose a target resource url. Commonly used values start with:");
        out.println("                              realms/, users/, roles/, groups/, clients/, keys/, components/ ...");
        out.println("                              If it starts with 'http://' then it will be used as target resource url");
        out.println("    -r, --target-realm REALM  Target realm to issue requests against if not the one authenticated against");
        out.println("    -s, --set NAME=VALUE      Send a body with request - set a specific attribute NAME to a specified value VALUE");
        out.println("    -d, --delete NAME         Remove a specific attribute NAME from JSON request body");
        out.println("    -f, --file FILENAME       Send a body with request - read object from file or standard input if FILENAME is set to '-'");
        out.println("    -b, --body CONTENT        Content to be sent as-is or used as a JSON object template");
        out.println("    -q, --query NAME=VALUE    Add to request URI a NAME query parameter with value VALUE, for example --query q=username:admin");
        out.println("    -h, --header NAME=VALUE   Set request header NAME to VALUE");
        out.println();
        out.println("    -H, --print-headers       Print response headers");
        out.println("    -o, --output              After delete output any response to standard output");
        out.println("    -F, --fields FILTER       A filter pattern to specify which fields of a JSON response to output");
        out.println("                              Use '" + CMD + " get --help' for more info on FILTER syntax.");
        out.println("    -c, --compressed          Don't pretty print the output");
        out.println("    -a, --admin-root URL      URL of Admin REST endpoint root if not default - e.g. http://localhost:8080/admin");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Delete a realm role:");
        out.println("  " + PROMPT + " " + CMD + " delete roles/manage-all -r demorealm");
        out.println();
        out.println("Delete a user (replace USER_ID with the value of user's 'id' attribute):");
        out.println("  " + PROMPT + " " + CMD + " delete users/USER_ID -r demorealm");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
