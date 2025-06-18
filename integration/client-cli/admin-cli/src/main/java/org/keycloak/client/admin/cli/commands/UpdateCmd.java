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
import picocli.CommandLine.Option;

import static org.keycloak.client.admin.cli.KcAdmMain.CMD;
import static org.keycloak.client.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "update", description = "CLIENT_ID [ARGUMENTS]")
public class UpdateCmd extends AbstractRequestCmd {

    public UpdateCmd() {
        this.httpVerb = "put";
    }

    @Option(names = {"-f", "--file"}, description = "Read object from file or standard input if FILENAME is set to '-'")
    public void setFile(String file) {
        this.file = file;
    }

    @Option(names = {"-b", "--body"}, description = "JSON object to be sent as-is or used as a template")
    public void setBody(String body) {
        this.body = body;
    }

    @Option(names = {"-F", "--fields"}, description = "A pattern specifying which attributes of JSON response body to actually display as result - causes mismatch with Content-Length header")
    public void setFields(String fields) {
        this.fields = fields;
    }

    @Option(names = {"-H", "--print-headers"}, description = "Print response headers")
    public void setPrintHeaders(boolean printHeaders) {
        this.printHeaders = printHeaders;
    }

    @Option(names = {"-m", "--merge"}, description = "Merge new values with existing configuration on the server - for when the default is not to merge (i.e. if --file is used)")
    public void setMergeMode(boolean mergeMode) {
        this.mergeMode = mergeMode;
    }

    @Option(names = {"-n", "--no-merge"}, description = "Don't merge new values with existing configuration on the server - for when the default is to merge (i.e. is --set is used while --file is not used)")
    public void setNoMerge(boolean noMerge) {
        this.noMerge = noMerge;
    }

    @Option(names = {"-o", "--output"}, description = "After update output the new client configuration")
    public void setOutputResult(boolean outputResult) {
        this.outputResult = outputResult;
    }

    @Option(names = {"-c", "--compressed"}, description = "Don't pretty print the output")
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    @Override
    protected String help() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " update ENDPOINT_URI [ARGUMENTS]");
        out.println();
        out.println("Command to update existing resources on the server.");
        out.println();
        out.println("Use '" + CMD + " config credentials' to establish an authenticated sessions, or use CREDENTIALS OPTIONS");
        out.println("to perform one time authentication.");
        globalOptions(out);
        out.println("    ENDPOINT_URI              URI used to compose a target resource url. Commonly used values start with:");
        out.println("                              realms/, users/, roles/, groups/, clients/, keys/, components/ ...");
        out.println("                              If it starts with 'http://' then it will be used as target resource url");
        out.println("    -r, --target-realm REALM  Target realm to issue requests against if not the one authenticated against");
        out.println("    -s, --set NAME=VALUE      Set a specific attribute NAME to a specified value VALUE");
        out.println("              NAME+=VALUE     Add item VALUE to list attribute NAME");
        out.println("    -d, --delete NAME         Remove a specific attribute NAME from JSON request body");
        out.println("    -f, --file FILENAME       Read object from file or standard input if FILENAME is set to '-'");
        out.println("    -b, --body CONTENT        Content to be sent as-is or used as a JSON object template");
        out.println("    -q, --query NAME=VALUE    Add to request URI a NAME query parameter with value VALUE, for example --query q=username:admin");
        out.println("    -h, --header NAME=VALUE   Set request header NAME to VALUE");
        out.println("    -m, --merge               Merge new values with existing configuration on the server");
        out.println("                              Merge is automatically enabled unless --file is specified");
        out.println("    -n, --no-merge            Suppress merge mode");
        out.println();
        out.println("    -H, --print-headers       Print response headers");
        out.println("    -o, --output              After update output the new resource to standard output");
        out.println("    -F, --fields FILTER       A filter pattern to specify which fields of a JSON response to output");
        out.println("                              Use '" + CMD + " get --help' for more info on FILTER syntax.");
        out.println("    -c, --compressed          Don't pretty print the output");
        out.println("    -a, --admin-root URL      URL of Admin REST endpoint root if not default - e.g. http://localhost:8080/admin");
        out.println();
        out.println();
        out.println("Nested attributes are supported by using '.' to separate components of a KEY. Optionally, the KEY components ");
        out.println("can be quoted with double quotes - e.g. my_client.attributes.\"external.user.id\". If VALUE starts with [ and ");
        out.println("ends with ] the attribute will be set as a JSON array. If VALUE starts with { and ends with } the attribute ");
        out.println("will be set as a JSON object. If KEY ends with an array index - e.g. clients[3]=VALUE - then the specified item");
        out.println("of the array is updated. If KEY+=VALUE syntax is used, then KEY is assumed to be an array, and another item is");
        out.println("added to it.");
        out.println();
        out.println("Attributes can also be deleted. If KEY ends with an array index, then the targeted item of an array is removed");
        out.println("and the following items are shifted.");
        out.println();
        out.println("Merged mode fetches target resource item from the server, applies attribute changes to it, and sends it");
        out.println("back to the server.");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Update a target realm by fetching current configuration from the server, and applying specified changes");
        out.println("  " + PROMPT + " " + CMD + " update realms/demorealm -s registrationAllowed=true");
        out.println();
        out.println("Update a client by overwriting existing configuration using local file as a template (replace ID with client's 'id'):");
        if (OS_ARCH.isWindows()) {
            out.println("  " + PROMPT + " " + CMD + " update clients/ID -f new_my_client.json -s \"redirectUris=[\\\"http://localhost:8080/myapp/*\\\"]\"");
        } else {
            out.println("  " + PROMPT + " " + CMD + " update clients/ID -f new_my_client.json -s 'redirectUris=[\"http://localhost:8080/myapp/*\"]'");
        }
        out.println();
        out.println("Update client by fetching current configuration from server and merging with specified changes (replace ID with client's 'id'):");
        out.println("  " + PROMPT + " " + CMD + " update clients/ID -f new_my_client.json -s enabled=true --merge");
        out.println();
        out.println("Reset user's password (replace ID with user's 'id'):");
        out.println("  " + PROMPT + " " + CMD + " update users/ID/reset-password -r demorealm -s type=password -s value=NEWPASSWORD -s temporary=true -n");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
