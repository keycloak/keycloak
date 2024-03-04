package org.keycloak.client.registration.cli.commands;

import org.keycloak.client.registration.cli.Globals;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import static org.keycloak.client.registration.cli.util.IoUtil.printOut;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractGlobalOptionsCmd implements Runnable {

    @Option(names = "--help",
            description = "Print command specific help")
    public void setHelp(boolean help) {
        Globals.help = help;
    }

    @Option(names = "-x",
            description = "Print full stack trace when exiting with error")
    public void setDumpTrace(boolean dumpTrace) {
        Globals.dumpTrace = dumpTrace;
    }

    protected void printHelpIfNeeded() {
        if (Globals.help) {
            printOut(help());
            System.exit(CommandLine.ExitCode.OK);
        } else if (nothingToDo()) {
            printOut(help());
            System.exit(CommandLine.ExitCode.USAGE);
        }
    }

    protected boolean nothingToDo() {
        return false;
    }

    protected String help() {
        return KcRegCmd.usage();
    }

    @Override
    public void run() {
        printHelpIfNeeded();

        checkUnsupportedOptions(getUnsupportedOptions());

        processOptions();

        process();
    }

    protected String[] getUnsupportedOptions() {
        return new String[0];
    }

    protected void processOptions() {

    }

    protected void process() {

    }

    protected void checkUnsupportedOptions(String ... options) {
        if (options.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of argument required");
        }

        for (int i = 0; i < options.length; i++) {
            String name = options[i];
            String value = options[++i];

            if (value != null) {
                throw new IllegalArgumentException("Unsupported option: " + name);
            }
        }
    }

    protected static String booleanOptionForCheck(boolean value) {
        return value ? "true" : null;
    }

}
