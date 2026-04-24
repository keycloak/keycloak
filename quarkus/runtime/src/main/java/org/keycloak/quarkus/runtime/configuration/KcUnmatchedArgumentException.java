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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        String unmatched = this.getUnmatched().get(0).toLowerCase();
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
        
        return candidates.stream().map(c -> Map.entry(cosineSimilarity(unmatched, c.toLowerCase()), c))
                .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey())).map(Map.Entry::getValue).limit(maxSuggestions).toList();
    }

    static double cosineSimilarity(String a, String b) {
        Map<String, Integer> aFreq = bigramFrequency(a);
        Map<String, Integer> bFreq = bigramFrequency(b);
        double dot = dotProduct(aFreq, bFreq);
        double normA = dotProduct(aFreq, aFreq);
        double normB = dotProduct(bFreq, bFreq);
        double denominator = Math.sqrt(normA * normB);
        return denominator == 0 ? 0 : dot / denominator;
    }

    private static Map<String, Integer> bigramFrequency(String s) {
        Map<String, Integer> freq = new HashMap<>();
        for (int i = 0; i < s.length() - 1; i++) {
            freq.merge(s.substring(i, i + 2), 1, Integer::sum);
        }
        return freq;
    }

    private static double dotProduct(Map<String, Integer> m1, Map<String, Integer> m2) {
        return m1.entrySet().stream()
                .collect(Collectors.summingDouble(e -> e.getValue() * (m2.getOrDefault(e.getKey(), 0))));
    }
}
