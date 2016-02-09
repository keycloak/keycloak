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

package org.keycloak.testsuite.util.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.testsuite.KeycloakServer;

/**
 * HOWTO USE THIS:
 *
 * 1) Run KeycloakServer with system properties (assuming mongo up and running on localhost):
 *      -Dkeycloak.realm.provider=mongo -Dkeycloak.user.provider=mongo -Dkeycloak.userSessionPersister.provider=mongo -Dkeycloak.connectionsMongo.db=keycloak -Dkeycloak.connectionsInfinispan.clustered=true -Dresources -DstartInfinispanCLI
 *
 * 2) Write command on STDIN to persist 50000 userSessions to mongo: persistSessions 50000
 *
 * 3) Run command "clear" to ensure infinispan cache is cleared. Doublecheck with command "size" is 0
 *
 * 4) Write command to load sessions from persistent storage - 100 sessions per worker transaction: loadPersistentSessions 100
 *
 * See the progress in log. Finally run command "size" to ensure size is 100001 (50000 userSessions + 50000 clientSessions + 1 initializationState item)
 *
 * 5) Alternative to step 3+4 - Kill the server after step 2 and start two KeycloakServer in parallel on ports 8081 and 8082 . See the progress in logs of loading persistent sessions to infinispan.
 * Kill the coordinator (usually 8081 node) during startup and see the node 8082 became coordinator and took ownership of loading persistent sessions. After node 8082 fully started, the size of infinispan is again 100001
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanCLI {

    private static final Logger log = Logger.getLogger(InfinispanCLI.class);

    private static final Class<?>[] BUILTIN_COMMANDS = {
            ExitCommand.class,
            HelpCommand.class,
            AbstractOfflineCacheCommand.PutCommand.class,
            AbstractOfflineCacheCommand.GetCommand.class,
            AbstractOfflineCacheCommand.GetLocalCommand.class,
            AbstractOfflineCacheCommand.RemoveCommand.class,
            AbstractOfflineCacheCommand.SizeCommand.class,
            AbstractOfflineCacheCommand.ListCommand.class,
            AbstractOfflineCacheCommand.ClearCommand.class,
            PersistSessionsCommand.class,
            LoadPersistentSessionsCommand.class,
            UserCommands.Create.class,
            UserCommands.Remove.class,
            UserCommands.Count.class,
            UserCommands.GetUser.class
    };

    private final KeycloakSessionFactory sessionFactory;
    private final Map<String, Class<? extends AbstractCommand>> commands = new LinkedHashMap<>();

    public InfinispanCLI(KeycloakServer server) {
        this.sessionFactory = server.getSessionFactory();

        // register builtin commands
        for (Class<?> clazz : BUILTIN_COMMANDS) {
            Class<? extends AbstractCommand> commandClazz = (Class<? extends AbstractCommand>) clazz;
            try {
                AbstractCommand command = commandClazz.newInstance();
                commands.put(command.getName(), commandClazz);
            } catch (Exception ex) {
                log.error("Error registering command of class: " + commandClazz.getName(), ex);
            }
        }
    }

    public void registerCommand(String name, Class<? extends AbstractCommand> command) {
        commands.put(name, command);
    }

    // WARNING: Stdin blocking operation
    public void start() throws IOException {
        log.info("Starting infinispan CLI. Exit with 'exit' . Available commands with 'help' ");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                log.info("Command: " + line);

                String[] splits = line.split(" ");
                String commandName = splits[0];
                Class<? extends AbstractCommand> commandClass = commands.get(commandName);
                if (commandClass == null) {
                    log.errorf("Unknown command: %s", commandName);
                } else {
                    try {
                        AbstractCommand command = commandClass.newInstance();
                        List<String> args = new ArrayList<>(Arrays.asList(splits));
                        args.remove(0);
                        command.injectProperties(args, this, sessionFactory);
                        command.runCommand();

                        // Just special handling of ExitCommand
                        if (command instanceof ExitCommand) {
                            return;
                        }

                    } catch (InstantiationException ex) {
                        log.error(ex);
                    } catch (IllegalAccessException ex) {
                        log.error(ex);
                    }
                }
            }
        } finally {
            log.info("Exit infinispan CLI");
            reader.close();
        }
    }

    public static class ExitCommand extends AbstractCommand {

        @Override
        public String getName() {
            return "exit";
        }

        @Override
        public void runCommand() {
            // no need to implement. Exit handled in parent
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            // no need to implement
        }

        @Override
        public String printUsage() {
            return getName();
        }
    }

    public static class HelpCommand extends AbstractCommand {

        private List<String> commandNames = new ArrayList<>();

        @Override
        public void injectProperties(List<String> args, InfinispanCLI cli, KeycloakSessionFactory sessionFactory) {
            for (String commandName : cli.commands.keySet()) {
                commandNames.add(commandName);
            }
        }

        @Override
        public String getName() {
            return "help";
        }

        @Override
        public void runCommand() {
            log.info("Available commands: " + commandNames.toString());
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            // no need to implement
        }
    }
}
