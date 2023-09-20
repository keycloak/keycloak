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

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "create", description = "Command to create new resources")
public class CreateCmd extends AbstractRequestCmd {

    @Option(shortName = 'f', name = "file", description = "Read object from file or standard input if FILENAME is set to '-'")
    String file;

    @Option(shortName = 'b', name = "body", description = "JSON object to be sent as-is or used as a template")
    String body;

    @Option(shortName = 'F', name = "fields", description = "A pattern specifying which attributes of JSON response body to actually display as result - causes mismatch with Content-Length header", hasValue = true)
    String fields;

    @Option(shortName = 'H', name = "print-headers", description = "Print response headers", hasValue = false)
    boolean printHeaders;

    @Option(shortName = 'i', name = "id", description = "After creation only print id of created resource to standard output", hasValue = false)
    boolean returnId = false;

    @Option(shortName = 'o', name = "output", description = "After creation output the new resource to standard output", hasValue = false)
    boolean outputResult = false;

    @Option(shortName = 'c', name = "compressed", description = "Don't pretty print the output", hasValue = false)
    boolean compressed = false;

    //@OptionGroup(shortName = 's', name = "set", description = "Set attribute to the specified value")
    //Map<String, String> attributes = new LinkedHashMap<>();

    @Override
    void initOptions() {
        // set options on parent
        super.file = file;
        super.body = body;
        super.fields = fields;
        super.printHeaders = printHeaders;
        super.returnId = returnId;
        super.outputResult = outputResult;
        super.compressed = compressed;
        super.httpVerb = "post";
    }

    @Override
    protected boolean nothingToDo() {
        return noOptions() && file == null && body == null && (args == null || args.size() == 0);
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help create' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " create ENDPOINT_URI [ARGUMENTS]");
        out.println();
        out.println("Command to create new resources on the server.");
        out.println();
        out.println("Use '" + CMD + " config credentials' to establish an authenticated sessions, or use --no-config with ");
        out.println("CREDENTIALS OPTIONS to perform one time authentication.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                    Print full stack trace when exiting with error");
        out.println("    --config              Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println("    --no-config           Don't use config file - no authentication info is loaded or saved");
        out.println("    --token               Token to use to invoke on Keycloak.  Other credential may be ignored if this flag is set.");
        out.println("    --truststore PATH     Path to a truststore containing trusted certificates");
        out.println("    --trustpass PASSWORD  Truststore password (prompted for if not specified and --truststore is used)");
        out.println("    CREDENTIALS OPTIONS   Same set of options as accepted by '" + CMD + " config credentials' in order to establish");
        out.println("                          an authenticated sessions. In combination with --no-config option this allows transient");
        out.println("                          (on-the-fly) authentication to be performed which leaves no tokens in config file.");
        out.println();
        out.println("  Command specific options:");
        out.println("    ENDPOINT_URI              URI used to compose a target resource url. Commonly used values are:");
        out.println("                              realms, users, roles, groups, clients, keys, serverinfo, components ...");
        out.println("                              If it starts with 'http://' then it will be used as target resource url");
        out.println("    -r, --target-realm REALM  Target realm to issue requests against if not the one authenticated against");
        out.println("    -s, --set NAME=VALUE      Set a specific attribute NAME to a specified value VALUE");
        out.println("    -d, --delete NAME         Remove a specific attribute NAME from JSON request body");
        out.println("    -f, --file FILENAME       Read object from file or standard input if FILENAME is set to '-'");
        out.println("    -b, --body CONTENT        Content to be sent as-is or used as a JSON object template");
        out.println("    -q, --query NAME=VALUE    Add to request URI a NAME query parameter with value VALUE");
        out.println("    -h, --header NAME=VALUE   Set request header NAME to VALUE");
        out.println();
        out.println("    -H, --print-headers       Print response headers");
        out.println("    -o, --output              After creation output the new resource to standard output");
        out.println("    -i, --id                  After creation only print id of the new resource to standard output");
        out.println("    -F, --fields FILTER       A filter pattern to specify which fields of a JSON response to output");
        out.println("                              Use '" + CMD + " get --help' for more info on FILTER syntax.");
        out.println("    -c, --compressed          Don't pretty print the output");
        out.println("    -a, --admin-root URL      URL of Admin REST endpoint root if not default - e.g. http://localhost:8080/admin");
        out.println();
        out.println();
        out.println("Nested attributes are supported by using '.' to separate components of a KEY. Optionaly, the KEY components ");
        out.println("can be quoted with double quotes - e.g. my_client.attributes.\"external.user.id\". If VALUE starts with [ and ");
        out.println("ends with ] the attribute will be set as a JSON array. If VALUE starts with { and ends with } the attribute ");
        out.println("will be set as a JSON object. If KEY ends with an array index - e.g. clients[3]=VALUE - then the specified item");
        out.println("of the array is updated. If KEY+=VALUE syntax is used, then KEY is assumed to be an array, and another item is");
        out.println("added to it.");
        out.println();
        out.println("Attributes can also be deleted. If KEY ends with an array index, then the targeted item of an array is removed");
        out.println("and the following items are shifted.");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Create a new realm:");
        out.println("  " + PROMPT + " " + CMD + " create realms -s realm=demorealm -s enabled=true");
        out.println();
        out.println("Create a new realm role in realm 'demorealm' returning newly created role:");
        out.println("  " + PROMPT + " " + CMD + " create roles -r demorealm -s name=manage-all -o");
        out.println();
        out.println("Create a new user in realm 'demorealm' returning only 'id', and 'username' attributes:");
        out.println("  " + PROMPT + " " + CMD + " create users -r demorealm -s username=testuser -s enabled=true -o --fields id,username");
        out.println();
        out.println("Create a new client using configuration read from standard input:");
        if (OS_ARCH.isWindows()) {
            out.println("  " + PROMPT + " echo { \"clientId\": \"my_client\" } | " + CMD + " create clients -r demorealm -f -");
        } else {
            out.println("  " + PROMPT + " " + CMD + " create clients -r demorealm  -f - << EOF");
            out.println("  {");
            out.println("    \"clientId\": \"my_client\"");
            out.println("  }");
            out.println("  EOF");
        }
        out.println();
        out.println("Create a new group using configuration JSON passed as 'body' argument:");
        if (OS_ARCH.isWindows()) {
            out.println("  " + PROMPT + " " + CMD + " create groups -r demorealm -b \"{ \\\"name\\\": \\\"Admins\\\" }\"");
        } else {
            out.println("  " + PROMPT + " " + CMD + " create groups -r demorealm -b '{ \"name\": \"Admins\" }'");
        }
        out.println();
        out.println("Create a client using file as a template, and override some attributes - return an 'id' of new client:");
        if (OS_ARCH.isWindows()) {
            out.println("  " + PROMPT + " " + CMD + " create clients -r demorealm -f my_client.json -s clientId=my_client2 -s \"redirectUris=[\\\"http://localhost:8980/myapp/*\\\"]\" -i");
        } else {
            out.println("  " + PROMPT + " " + CMD + " create clients -r demorealm -f my_client.json -s clientId=my_client2 -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]' -i");
        }
        out.println();
        out.println("Create a new client role for client my_client in realm 'demorealm' (replace ID with output of previous example command):");
        out.println("  " + PROMPT + " " + CMD + " create clients/ID/roles -r demorealm -s name=client_role");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }

}
