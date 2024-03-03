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

import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.admin.cli.util.IoUtil.printOut;

@Command(name = "help", description = "This Help")
public class HelpCmd implements Runnable {

    @Parameters
    List<String> args;

    @Override
    public void run() {
        if (args == null || args.size() == 0) {
            printOut(KcAdmCmd.usage());
        } else {
            outer: switch (args.get(0)) {
            case "config": {
                if (args.size() > 1) {
                    switch (args.get(1)) {
                    case "credentials": {
                        printOut(ConfigCredentialsCmd.usage());
                        break outer;
                    }
                    case "truststore": {
                        printOut(ConfigTruststoreCmd.usage());
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
            case "get-roles": {
                printOut(GetRolesCmd.usage());
                break;
            }
            case "add-roles": {
                printOut(AddRolesCmd.usage());
                break;
            }
            case "remove-roles": {
                printOut(RemoveRolesCmd.usage());
                break;
            }
            case "set-password": {
                printOut(SetPasswordCmd.usage());
                break;
            }
            case "new-object": {
                printOut(NewObjectCmd.usage());
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown command: " + args.get(0));
            }
            }
        }
    }
}
