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
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "get", description = "[ARGUMENTS]")
public class GetCmd extends  AbstractRequestCmd {

    @Option(name = "noquotes", description = "", hasValue = false)
    boolean unquoted;

    @Option(shortName = 'F', name = "fields", description = "A pattern specifying which attributes of JSON response body to actually display as result - causes mismatch with Content-Length header")
    String fields;

    @Option(shortName = 'H', name = "print-headers", description = "Print response headers", hasValue = false)
    boolean printHeaders;

    @Option(shortName = 'c', name = "compressed", description = "Don't pretty print the output", hasValue = false)
    boolean compressed;

    @Option(shortName = 'o', name = "offset", description = "Number of results from beginning of resultset to skip")
    Integer offset;

    @Option(shortName = 'l', name = "limit", description = "Maksimum number of results to return")
    Integer limit;

    @Option(name = "format", description = "Output format - one of: json, csv", defaultValue = "json")
    String format;


    @Override
    void initOptions() {
        // set options on parent
        super.fields = fields;
        super.printHeaders = printHeaders;
        super.returnId = false;
        super.outputResult = true;
        super.compressed = compressed;
        super.offset = offset;
        super.limit = limit;
        super.format = format;
        super.unquoted = unquoted;
        super.httpVerb = "get";
    }

    @Override
    protected boolean nothingToDo() {
        return noOptions() && (args == null || args.size() == 0);
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help get' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " get ENDPOINT_URI [ARGUMENTS]");
        out.println();
        out.println("Command to retrieve existing resources from the server.");
        out.println();
        out.println("Use '" + CMD + " config credentials' to establish an authenticated session, or use CREDENTIALS OPTIONS");
        out.println("to perform one time authentication.");
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
        out.println("    -q, --query NAME=VALUE    Add to request URI a NAME query parameter with value VALUE");
        out.println("    -h, --header NAME=VALUE   Set request header NAME to VALUE");
        out.println("    -o, --offset OFFSET       Set paging offset - adds a query parameter 'first' which some endpoints recognize");
        out.println("    -l, --limit LIMIT         Set limit to number of items in result - adds a query parameter 'max' ");
        out.println("                              which some endpoints recognize");
        out.println();
        out.println("    -H, --print-headers       Print response headers");
        out.println("    -F, --fields FILTER       A filter pattern to specify which fields of a JSON response to output");
        out.println("    -c, --compressed          Don't pretty print the output");
        out.println("    --format FORMAT           Set output format to comma-separated-values by using 'csv'. Default format is 'json'");
        out.println("    --noquotes                Don't quote strings when output format is 'csv'");
        out.println("    -a, --admin-root URL      URL of Admin REST endpoint root if not default - e.g. http://localhost:8080/auth/admin");
        out.println();
        out.println("Output results can be filtered by using '--fields' and a filter pattern. Filtering is performed by processing each item in a result set");
        out.println("and applying filter on it. A pattern is defined as a comma separated list of attribute specifiers. Use '*' in a specifier to include all ");
        out.println("attributes. Use attribute name, to include individual attributes. Use '-' prefix to exclude individual attributes.");
        out.println("Use brackets after attribute specifier to specify a pattern for child attributes. For example: ");
        out.println();
        out.println("   'protocolMappers(id,config)'          only return attributes 'id' and 'config' of protocolMapper top level attribute");
        out.println("   '*(*(*))'                             return all attributes three levels deep");
        out.println("   '*(*),-id,-protocolMappers'           return all attributes two levels deep, excluding 'id', and 'protocolMappers' top level attributes");
        out.println();
        out.println("If attribute of object type is included, but its children are not specified by using brackets, then an empty object will be returned - '{}'.");
        out.println("Usually you will want to specify object attributes with brackets to display them fully - e.g. 'protocolMappers(*(*))'");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Get all realms, displaying only some of the attributes:");
        out.println("  " + PROMPT + " " + CMD + " get realms --fields id,realm,enabled");
        out.println();
        out.println("Get 'demorealm':");
        out.println("  " + PROMPT + " " + CMD + " get realms/demorealm");
        out.println();
        out.println("Get all configured identity providers in demorealm, displaying only some of the attributes:");
        out.println("  " + PROMPT + " " + CMD + " get identity-provider/instances -r demorealm --fields alias,providerId,enabled");
        out.println();
        out.println("Get all clients in demorealm, displaying only some of the attributes:");
        out.println("  " + PROMPT + " " + CMD + " get clients -r demorealm --fields 'id,clientId,protocolMappers(id,name,protocol,protocolMapper)'");
        out.println();
        out.println("Get specific client in demorealm, and remove 'id', and 'protocolMappers' attributes in order to use");
        out.println("it as a template (replace ID with client's 'id'):");
        out.println("  " + PROMPT + " " + CMD + " get clients/ID -r demorealm --fields '*(*),-id,-protocolMappers' > realm-template.json");
        out.println();
        out.println("Display first level attributes available on 'serverinfo' resource:");
        out.println("  " + PROMPT + " " + CMD + " get serverinfo -r demorealm --fields '*'");
        out.println();
        out.println("Display system info and memory info:");
        out.println("  " + PROMPT + " " + CMD + " get serverinfo -r demorealm --fields 'systemInfo(*),memoryInfo(*)'");
        out.println();
        out.println("Get adapter configuration for the client (replace ID with client's 'id'):");
        out.println("  " + PROMPT + " " + CMD + " get clients/ID/installation/providers/keycloak-oidc-keycloak-json -r demorealm");
        out.println();
        out.println("Get first 100 users at the most:");
        out.println("  " + PROMPT + " " + CMD + " get users -r demorealm --offset 0 --limit 100");
        out.println();
        out.println("Note: 'users' endpoint knows how to handle --offset and --limit. Most other endpoints don't.");
        out.println();
        out.println("Get all users whose 'username' matches '*test*' pattern, and 'email' matches '*@google.com*':");
        out.println("  " + PROMPT + " " + CMD + " get users -r demorealm -q username=test -q email=@google.com");
        out.println();
        out.println("Note: it is the 'users' endpoint that interprets query parameters 'username', and 'email' in such a way that");
        out.println("it results in the described semantics. Another endpoint may provide a different semantics.");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
