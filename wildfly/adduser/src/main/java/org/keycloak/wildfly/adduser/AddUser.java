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

package org.keycloak.wildfly.adduser;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.keycloak.common.util.Base64;
import org.keycloak.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AddUser {

    private static final String COMMAND_NAME = "add-user";
    private static final int DEFAULT_HASH_ITERATIONS = 100000;

    public static void main(String[] args) throws Exception {
        AddUserCommand command = new AddUserCommand();
        try {
            ParserGenerator.parseAndPopulate(command, COMMAND_NAME, args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        if (command.isHelp()) {
            printHelp(command);
        } else {
            try {
                String password = command.getPassword();
                checkRequired(command, "user");

                if(isEmpty(command, "password")){
                    password = promptForInput();
                }

                File addUserFile = getAddUserFile(command);

                createUser(addUserFile, command.getRealm(), command.getUser(), password, command.getRoles(), command.getIterations());
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
    }

    private static File getAddUserFile(AddUserCommand command) throws Exception {
        File configDir;
        if (command.isDomain()) {
            if (command.getDc() != null) {
                configDir = new File(command.getDc());
            } else if (System.getProperty("jboss.domain.config.user.dir") != null) {
                configDir = new File(System.getProperty("jboss.domain.config.user.dir"));
            } else if (System.getenv("JBOSS_HOME") != null) {
                configDir = new File(System.getenv("JBOSS_HOME") + File.separator + "domain" + File.separator + "configuration");
            } else {
                throw new Exception("Could not find domain configuration directory");
            }
        } else {
            if (command.getSc() != null) {
                configDir = new File(command.getSc());
            } else if (System.getProperty("jboss.server.config.user.dir") != null) {
                configDir = new File(System.getProperty("jboss.server.config.user.dir"));
            } else if (System.getenv("JBOSS_HOME") != null) {
                configDir = new File(System.getenv("JBOSS_HOME") + File.separator + "standalone" + File.separator + "configuration");
            } else {
                throw new Exception("Could not find standalone configuration directory");
            }
        }

        if (!configDir.isDirectory()) {
            throw new Exception("'" + configDir + "' does not exist or is not a directory");
        }

        File addUserFile = new File(configDir, "keycloak-add-user.json");
        return addUserFile;
    }

    private static void createUser(File addUserFile, String realmName, String userName, String password, String rolesString, int iterations) throws Exception {
        List<RealmRepresentation> realms;
        if (addUserFile.isFile()) {
            realms = JsonSerialization.readValue(new FileInputStream(addUserFile), new TypeReference<List<RealmRepresentation>>() {});
        } else {
            realms = new LinkedList<>();
        }

        if (realmName == null) {
            realmName = "master";
        }

        RealmRepresentation realm = null;
        for (RealmRepresentation r : realms) {
            if (r.getRealm().equals(realmName)) {
                realm = r;
            }
        }

        if (realm == null) {
            realm = new RealmRepresentation();
            realm.setRealm(realmName);
            realms.add(realm);
            realm.setUsers(new LinkedList<UserRepresentation>());
        }

        for (UserRepresentation u : realm.getUsers()) {
            if (u.getUsername().equals(userName)) {
                throw new Exception("User with username '" + userName + "' already added to '" + addUserFile + "'");
            }
        }

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(userName);
        user.setCredentials(new LinkedList<CredentialRepresentation>());

        UserCredentialValueModel credentialValueModel = new Pbkdf2PasswordHashProvider().encode(password, iterations > 0 ? iterations : DEFAULT_HASH_ITERATIONS);

        CredentialRepresentation credentials = new CredentialRepresentation();
        credentials.setType(credentialValueModel.getType());
        credentials.setAlgorithm(credentialValueModel.getAlgorithm());
        credentials.setHashIterations(credentialValueModel.getHashIterations());
        credentials.setSalt(Base64.encodeBytes(credentialValueModel.getSalt()));
        credentials.setHashedSaltedValue(credentialValueModel.getValue());

        user.getCredentials().add(credentials);

        String[] roles;
        if (rolesString != null) {
            roles = rolesString.split(",");
        } else {
            if (realmName.equals("master")) {
                roles = new String[] { "admin" };
            } else {
                roles = new String[] { "realm-management/realm-admin" };
            }
        }

        for (String r : roles) {
            if (r.indexOf('/') != -1) {
                String[] cr = r.split("/");
                String client = cr[0];
                String clientRole = cr[1];

                if (user.getClientRoles() == null) {
                    user.setClientRoles(new HashMap<String, List<String>>());
                }

                if (user.getClientRoles().get(client) == null) {
                    user.getClientRoles().put(client, new LinkedList<String>());
                }

                user.getClientRoles().get(client).add(clientRole);
            } else {
                if (user.getRealmRoles() == null) {
                    user.setRealmRoles(new LinkedList<String>());
                }
                user.getRealmRoles().add(r);
            }
        }

        realm.getUsers().add(user);

        JsonSerialization.writeValuePrettyToStream(new FileOutputStream(addUserFile), realms);
        System.out.println("Added '" + userName + "' to '" + addUserFile + "', restart server to load user");
    }

    private static void checkRequired(Command command, String field) throws Exception {
        if (isEmpty(command, field)) {
            Option option = command.getClass().getDeclaredField(field).getAnnotation(Option.class);
            String optionName;
            if (option != null && option.shortName() != '\u0000') {
                optionName = "-" + option.shortName() + ", --" + field;
            } else {
                optionName = "--" + field;
            }
            throw new Exception("Option: " + optionName + " is required");
        }
    }

    private static Boolean isEmpty(Command command, String field) throws Exception {
        Method m = command.getClass().getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
        if (m.invoke(command) == null) {
            return true;
        }
        return false;
    }

    private static String promptForInput() throws Exception {
        Console console = System.console();
        if (console == null) {
            throw new Exception("Couldn't get Console instance");
        }
        console.printf("Press ctrl-d (Unix) or ctrl-z (Windows) to exit\n");
        char passwordArray[] = console.readPassword("Password: ");

        if(passwordArray == null) System.exit(0);

        return new String(passwordArray);
    }

    private static void printHelp(Command command) throws CommandNotFoundException {
        CommandRegistry registry = new AeshCommandRegistryBuilder().command(command).create();
        CommandContainer commandContainer = registry.getCommand(command.getClass().getAnnotation(CommandDefinition.class).name(), null);
        String help = commandContainer.printHelp(null);
        System.out.println(help);
    }

    @CommandDefinition(name= COMMAND_NAME, description = "[options...]")
    public static class AddUserCommand implements Command {

        @Option(shortName = 'r', hasValue = true, description = "Name of realm to add user to")
        private String realm;

        @Option(shortName = 'u', hasValue = true, description = "Name of the user")
        private String user;

        @Option(shortName = 'p', hasValue = true, description = "Password of the user")
        private String password;

        @Option(hasValue = true, description = "Roles to add to the user")
        private String roles;

        @Option(hasValue = true, description = "Hash iterations")
        private int iterations;

        @Option(hasValue = false, description = "Enable domain mode")
        private boolean domain;

        @Option(hasValue = true, description = "Define the location of the server config directory")
        private String sc;

        @Option(hasValue = true, description = "Define the location of the domain config directory")
        private String dc;

        @Option(shortName = 'h', hasValue = false, description = "Display this help and exit")
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            return CommandResult.SUCCESS;
        }

        public String getRealm() {
            return realm;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public String getRoles() {
            return roles;
        }

        public int getIterations() {
            return iterations;
        }

        public boolean isDomain() {
            return domain;
        }

        public String getSc() {
            return sc;
        }

        public String getDc() {
            return dc;
        }

        public boolean isHelp() {
            return help;
        }
    }

}
