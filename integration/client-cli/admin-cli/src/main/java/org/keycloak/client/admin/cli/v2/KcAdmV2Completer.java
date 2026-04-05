package org.keycloak.client.admin.cli.v2;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import picocli.AutoComplete;
import picocli.CommandLine;

/**
 * Handles the {@code __complete} request for dynamic shell completion.
 * Delegates to PicoCLI's {@link AutoComplete#complete} for candidate resolution.
 */
public class KcAdmV2Completer {

    private static final String LONG_OPTION_PREFIX = "--";

    public static void complete(String[] args, PrintWriter out) {
        KcAdmV2Cmd rootCmd = new KcAdmV2Cmd();
        CommandLine cli = new CommandLine(rootCmd);
        rootCmd.configureCommandLine(cli);

        String partial = args.length > 0 ? args[args.length - 1] : "";

        if (LONG_OPTION_PREFIX.equals(partial)) {
            completeLongOptions(cli, args, out);
        } else {
            completePicocli(cli, args, partial, out);
        }

        out.flush();
    }

    private static void completePicocli(CommandLine cli, String[] args, String partial, PrintWriter out) {
        int cursor = 0;
        for (String arg : args) {
            cursor += arg.length() + 1;
        }
        if (cursor > 0) {
            cursor--;
        }

        int argIndex = Math.max(0, args.length - 1);
        int posInArg = partial.length();

        List<CharSequence> candidates = new ArrayList<>();
        AutoComplete.complete(cli.getCommandSpec(), args, argIndex, posInArg, cursor, candidates);

        for (CharSequence candidate : candidates) {
            out.println(partial + candidate);
        }
    }

    // PicoCLI's AutoComplete.complete() treats "--" as ambiguous (end-of-options vs partial option),
    // so we handle it ourselves by listing all long options for the resolved command.
    private static void completeLongOptions(CommandLine cli, String[] args, PrintWriter out) {
        CommandLine current = cli;
        for (int i = 0; i < args.length - 1; i++) {
            CommandLine sub = current.getSubcommands().get(args[i]);
            if (sub == null) {
                break;
            }
            current = sub;
        }

        for (var opt : current.getCommandSpec().options()) {
            for (String name : opt.names()) {
                if (name.startsWith(LONG_OPTION_PREFIX)) {
                    out.println(name);
                }
            }
        }
    }
}
