/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.userprofile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class ValidationException extends RuntimeException {

    private final Map<String, List<Error>> errors = new HashMap<>();

    public List<Error> getErrors() {
        return errors.values().stream().reduce(new ArrayList<>(),
                (l, r) -> {
                    l.addAll(r);
                    return l;
                }, (l, r) -> l);
    }

    public boolean hasError(String... types) {
        if (types.length == 0) {
            return !errors.isEmpty();
        }

        for (String type : types) {
            if (errors.containsKey(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there are validation errors related to the attribute with the given {@code name}.
     *
     * @param name
     * @return
     */
    public boolean isAttributeOnError(String... name) {
        if (name.length == 0) {
            return !errors.isEmpty();
        }

        List<String> names = Arrays.asList(name);

        return errors.values().stream().flatMap(Collection::stream)
                .anyMatch(error -> names.contains(error.attribute.getKey()));
    }

	void addError(Error error) {
		List<Error> errors = this.errors.computeIfAbsent(error.getMessage(), (k) -> new ArrayList<>());
		errors.add(error);
	}

    public static class Error {

        private final Map.Entry<String, List<String>> attribute;
        private final String message;

        public Error(Map.Entry<String, List<String>> attribute, String message) {
            this.attribute = attribute;
            this.message = message;
        }

        public  String getAttribute() {
            return attribute.getKey();
        }

        //TODO: support parameters to messsages for formatting purposes. Message key and parameters.
        public String getMessage() {
            return message;
        }
    }
}
