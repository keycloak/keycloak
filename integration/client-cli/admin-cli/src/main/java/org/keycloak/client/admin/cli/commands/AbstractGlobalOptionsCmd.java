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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.keycloak.client.admin.cli.aesh.Globals;
import org.keycloak.client.admin.cli.util.FilterUtil;
import org.keycloak.client.admin.cli.util.ReturnFields;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.keycloak.client.admin.cli.util.HttpUtil.normalize;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractGlobalOptionsCmd implements Command {

    @Option(shortName = 'x', description = "Print full stack trace when exiting with error", hasValue = false)
    boolean dumpTrace;

    @Option(name = "help", description = "Print command specific help", hasValue = false)
    boolean help;


    // we don't want Aesh to handle illegal options
    @Arguments
    List<String> args;


    protected void initFromParent(AbstractGlobalOptionsCmd parent) {
        dumpTrace = parent.dumpTrace;
        help = parent.help;
        args = parent.args;
    }

    protected void processGlobalOptions() {
        Globals.dumpTrace = dumpTrace;
    }

    protected boolean printHelp() {
        if (help || nothingToDo()) {
            printOut(help());
            return true;
        }

        return false;
    }

    protected boolean nothingToDo() {
        return false;
    }

    protected String help() {
        return KcAdmCmd.usage();
    }

    protected String composeAdminRoot(String server) {
        return normalize(server) + "admin";
    }


    protected void requireValue(Iterator<String> it, String option) {
        if (!it.hasNext()) {
            throw new IllegalArgumentException("Option " + option + " requires a value");
        }
    }

    protected String extractTypeNameFromUri(String resourceUrl) {
        String type = extractLastComponentOfUri(resourceUrl);
        if (type.endsWith("s")) {
            type = type.substring(0, type.length()-1);
        }
        return type;
    }

    protected String extractLastComponentOfUri(String resourceUrl) {
        int endPos = resourceUrl.endsWith("/") ? resourceUrl.length()-2 : resourceUrl.length()-1;
        int pos = resourceUrl.lastIndexOf("/", endPos);
        pos = pos == -1 ? 0 : pos;
        return resourceUrl.substring(pos+1, endPos+1);
    }

    protected JsonNode applyFieldFilter(ObjectMapper mapper, JsonNode rootNode, ReturnFields returnFields) {
        // construct new JsonNode that satisfies filtering specified by returnFields
        try {
            return FilterUtil.copyFilteredObject(rootNode, returnFields);
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply fields filter", e);
        }
    }
}
