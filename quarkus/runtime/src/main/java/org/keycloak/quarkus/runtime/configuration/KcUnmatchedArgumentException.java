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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import picocli.CommandLine;

/**
 * Custom CommandLine.UnmatchedArgumentException with amended suggestions
 */
public class KcUnmatchedArgumentException extends CommandLine.UnmatchedArgumentException {

    public KcUnmatchedArgumentException(CommandLine commandLine, List<String> args) {
        super(commandLine, args);
    }

    public KcUnmatchedArgumentException(CommandLine.UnmatchedArgumentException ex) {
        super(ex.getCommandLine(), ex.getUnmatched());
    }

    @Override
    public List<String> getSuggestions() {
        List<String> result = super.getSuggestions();
        // command suggestions are limited to 3 results, but options are seemingly unlimited
        if (result.size() > 7) {
            try {
                Class<?> clazz = Class.forName(CommandLine.class.getName() + "$CosineSimilarity");
                Method m = clazz.getDeclaredMethod("mostSimilar", String.class, Iterable.class);
                m.setAccessible(true);
                result = (List<String>) m.invoke(null, this.getUnmatched().get(0), result);
                result = result.subList(0, Math.min(7, result.size()));
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
                    | InvocationTargetException e) {
                // do nothing
            }
        }
        return result;
    }
}
