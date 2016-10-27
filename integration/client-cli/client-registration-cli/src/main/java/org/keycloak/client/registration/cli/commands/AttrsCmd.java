package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.registration.cli.common.AttributeKey;
import org.keycloak.client.registration.cli.common.EndpointType;
import org.keycloak.client.registration.cli.util.ReflectionUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.registration.cli.util.ReflectionUtil.getAttributeListWithJSonTypes;
import static org.keycloak.client.registration.cli.util.ReflectionUtil.isBasicType;
import static org.keycloak.client.registration.cli.util.ReflectionUtil.isListType;
import static org.keycloak.client.registration.cli.util.ReflectionUtil.isMapType;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "attrs", description = "[ATTRIBUTE] [--endpoint TYPE]")
public class AttrsCmd extends AbstractGlobalOptionsCmd {

    @Option(shortName = 'e', name = "endpoint", description = "Endpoint type to use", hasValue = true)
    protected String endpoint;

    @Arguments
    protected List<String> args;

    protected String attr;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            processGlobalOptions();

            if (printHelp()) {
                return CommandResult.SUCCESS;
            }

            EndpointType regType = EndpointType.DEFAULT;
            PrintStream out = commandInvocation.getShell().out();

            if (endpoint != null) {
                regType = EndpointType.of(endpoint);
            }

            if (args != null) {
                if (args.size() > 1) {
                    throw new IllegalArgumentException("Invalid option: " + args.get(1));
                }
                attr = args.get(0);
            }

            Class type = regType == EndpointType.DEFAULT ? ClientRepresentation.class : (regType == EndpointType.OIDC ? OIDCClientRepresentation.class : null);
            if (type == null) {
                throw new IllegalArgumentException("Endpoint not supported: " + regType);
            }
            AttributeKey key = attr == null ? new AttributeKey() : new AttributeKey(attr);

            Field f = ReflectionUtil.resolveField(type, key);
            String ts = f != null ? ReflectionUtil.getTypeString(null, f) : null;

            if (f == null) {
                out.printf("Attributes for %s format:\n", regType.getEndpoint());

                LinkedHashMap<String, String> items = getAttributeListWithJSonTypes(type, key);
                for (Map.Entry<String, String> item : items.entrySet()) {
                    out.printf("  %-40s %s\n", item.getKey(), item.getValue());
                }

            } else {
                out.printf("%-40s %s", attr, ts);
                boolean eol = false;

                Type t = f.getGenericType();
                if (isListType(f.getType()) && t instanceof ParameterizedType) {
                    t = ((ParameterizedType) t).getActualTypeArguments()[0];
                    if (!isBasicType(t) && t instanceof Class) {
                        eol = true;
                        System.out.printf(", where value is:\n", ts);
                        LinkedHashMap<String, String> items = ReflectionUtil.getAttributeListWithJSonTypes((Class) t, null);
                        for (Map.Entry<String, String> item : items.entrySet()) {
                            out.printf("    %-36s %s\n", item.getKey(), item.getValue());
                        }
                    }
                } else if (isMapType(f.getType()) && t instanceof ParameterizedType) {
                    t = ((ParameterizedType) t).getActualTypeArguments()[1];
                    if (!isBasicType(t) && t instanceof Class) {
                        eol = true;
                        out.printf(", where value is:\n", ts);
                        LinkedHashMap<String, String> items = ReflectionUtil.getAttributeListWithJSonTypes((Class) t, null);
                        for (Map.Entry<String, String> item : items.entrySet()) {
                            out.printf("    %-36s %s\n", item.getKey(), item.getValue());
                        }
                    }
                }

                if (!eol) {
                    // add end of line
                    out.println();
                }
            }

            return CommandResult.SUCCESS;

        } finally {
            commandInvocation.stop();
        }
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " attrs [ATTRIBUTE] [ARGUMENTS]");
        out.println();
        out.println("List available configuration attributes.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                   Print full stack trace when exiting with error");
        out.println();
        out.println("  Command specific options:");
        out.println("    ATTRIBUTE            Attribute key (if omitted all attributes for the endpoint type are listed)");
        out.println("                         Dot notation can be used to target sub-attributes.");
        out.println("    -e, --endpoint TYPE  Endpoint type to use - one of: 'default', 'oidc' (if omitted 'default' is used)");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("List all attributes for default endpoint:");
        out.println("  " + PROMPT + " " + CMD + " attrs");
        out.println();
        out.println("List (sub)attributes of 'protocolMappers' attribute for default endpoint:");
        out.println("  " + PROMPT + " " + CMD + " attrs protocolMappers");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
