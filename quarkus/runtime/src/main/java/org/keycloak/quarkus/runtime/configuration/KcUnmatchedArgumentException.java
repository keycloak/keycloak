/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import picocli.CommandLine;

/**
 * Custom CommandLine.UnmatchedArgumentException with amended suggestions
 */
public class KcUnmatchedArgumentException extends CommandLine.UnmatchedArgumentException {

    private static final int MAX_OPTION_SUGGESTIONS = 7;
    private static final int MAX_COMMAND_SUGGESTIONS = 3;

    public KcUnmatchedArgumentException(CommandLine commandLine, List<String> args) {
        super(commandLine, args);
    }

    public KcUnmatchedArgumentException(CommandLine.UnmatchedArgumentException ex) {
        super(ex.getCommandLine(), ex.getUnmatched());
    }

    /**
     * see https://github.com/remkop/picocli/issues/2510 for issues with the
     * default picocli logic
     */
    @Override
    public List<String> getSuggestions() {
        String unmatched = this.getUnmatched().get(0);
        List<String> candidates;
        int maxSuggestions;
        if (isUnknownOption()) {
            candidates = super.getSuggestions(); // can be a lengthy list of all options
            maxSuggestions = MAX_OPTION_SUGGESTIONS;
        } else {
            candidates = new ArrayList<String>();
            for (Map.Entry<String, CommandLine> entry : commandLine.getCommandSpec().subcommands().entrySet()) {
                if (!entry.getValue().getCommandSpec().usageMessage().hidden()) {
                    candidates.add(entry.getKey());
                }
            }
            maxSuggestions = MAX_COMMAND_SUGGESTIONS;
        }

        return SimilarityUtil.findSimilar(unmatched, candidates, maxSuggestions, 0);
    }
}
