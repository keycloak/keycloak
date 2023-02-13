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
package org.keycloak.client.admin.cli.aesh;

import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.invocation.AeshCommandInvocation;
import org.jboss.aesh.console.command.invocation.AeshCommandInvocationProvider;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.aesh.parser.ParserStatus;

import java.lang.reflect.Method;

class AeshConsoleCallbackImpl extends AeshConsoleCallback {

    private final AeshConsoleImpl console;
    private CommandResult result;

    AeshConsoleCallbackImpl(AeshConsoleImpl aeshConsole) {
        this.console = aeshConsole;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int execute(ConsoleOperation output) throws InterruptedException {
        if (output != null && output.getBuffer().trim().length() > 0) {
            ResultHandler resultHandler = null;
            //AeshLine aeshLine = Parser.findAllWords(output.getBuffer());
            AeshLine aeshLine = new AeshLine(output.getBuffer(), Globals.args, ParserStatus.OK, "");
            try (CommandContainer commandContainer = getCommand(output, aeshLine)) {
                resultHandler = commandContainer.getParser().getProcessedCommand().getResultHandler();
                CommandContainerResult ccResult =
                        commandContainer.executeCommand(aeshLine, console.getInvocationProviders(), console.getAeshContext(),
                                new AeshCommandInvocationProvider().enhanceCommandInvocation(
                                new AeshCommandInvocation(console,
                                    output.getControlOperator(), output.getPid(), this)));

                result = ccResult.getCommandResult();

                if(result == CommandResult.SUCCESS && resultHandler != null)
                    resultHandler.onSuccess();
                else if(resultHandler != null)
                    resultHandler.onFailure(result);

                if (result == CommandResult.FAILURE) {
                    // we assume the command has already output any error messages
                    System.exit(1);
                }
            } catch (Exception e) {
                console.stop();

                if (e instanceof OptionParserException) {
                    System.err.println("Unknown command: " + aeshLine.getWords().get(0));
                } else {
                    System.err.println(e.getMessage());
                }
                if (Globals.dumpTrace) {
                    e.printStackTrace();
                }

                System.exit(1);
            }
        }
        // empty line
        else if (output != null) {
            result = CommandResult.FAILURE;
        }
        else {
            //stop();
            result = CommandResult.FAILURE;
        }

        if (result == CommandResult.SUCCESS) {
            return 0;
        } else {
            return 1;
        }
    }

    private CommandContainer getCommand(ConsoleOperation output, AeshLine aeshLine) throws CommandNotFoundException {
        Method m;
        try {
            m = console.getClass().getDeclaredMethod("getCommand", AeshLine.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unexpected error: ", e);
        }

        m.setAccessible(true);

        try {
            return (CommandContainer) m.invoke(console, aeshLine, output.getBuffer());
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error: ", e);
        }
    }
}
