package org.keycloak.client.registration.cli.commands;

import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.cli.util.IoUtil.printOut;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "help", description = "This help")
public class HelpCmd implements Runnable {

    @Parameters
    private List<String> args;

    @Override
    public void run() {
        if (args == null || args.size() == 0) {
            printOut(KcRegCmd.usage());
        } else {
            outer:
            switch (args.get(0)) {
                case "config": {
                    if (args.size() > 1) {
                        switch (args.get(1)) {
                            case "credentials": {
                                printOut(new ConfigCredentialsCmd().help());
                                break outer;
                            }
                            case "initial-token": {
                                printOut(ConfigInitialTokenCmd.usage());
                                break outer;
                            }
                            case "registration-token": {
                                printOut(ConfigRegistrationTokenCmd.usage());
                                break outer;
                            }
                            case "truststore": {
                                printOut(new ConfigTruststoreCmd().help());
                                break outer;
                            }
                        }
                    }
                    printOut(ConfigCmd.usage());
                    break;
                }
                case "create": {
                    printOut(CreateCmd.usage());
                    break;
                }
                case "get": {
                    printOut(GetCmd.usage());
                    break;
                }
                case "update": {
                    printOut(UpdateCmd.usage());
                    break;
                }
                case "delete": {
                    printOut(DeleteCmd.usage());
                    break;
                }
                case "attrs": {
                    printOut(AttrsCmd.usage());
                    break;
                }
                case "update-token": {
                    printOut(UpdateTokenCmd.usage());
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown command: " + args.get(0));
                }
            }
        }
    }
}
