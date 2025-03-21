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
package org.keycloak.client.cli.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class ReturnFields implements Iterable<String> {

    public static ReturnFields ALL = new ReturnFields() {
        @Override
        public ReturnFields child(String field) {
            return NONE;
        }

        @Override
        public boolean included(String... pathSegments) {
            return true;
        }

        @Override
        public boolean excluded(String field) {
            return false;
        }

        @Override
        public Iterator<String> iterator() {
            return Collections.singletonList("*").iterator();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        public boolean isAll() {
            return true;
        }

        @Override
        public String toString() {
            return "[ReturnFields ALL]";
        }
    };

    public static ReturnFields NONE = new ReturnFields() {
        @Override
        public ReturnFields child(String field) {
            return this;
        }

        @Override
        public boolean included(String... pathSegments) {
            return false;
        }

        @Override
        public boolean excluded(String field) {
            return false;
        }

        @Override
        public Iterator<String> iterator() {
            List<String> emptyList = Collections.emptyList();
            return emptyList.iterator();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean isAll() {
            return false;
        }

        @Override
        public String toString() {
            return "[ReturnFields NONE]";
        }
    };

    public static ReturnFields ALL_RECURSIVELY = new ReturnFields() {
        @Override
        public ReturnFields child(String field) {
            return this;
        }

        @Override
        public boolean included(String... pathSegments) {
            return true;
        }

        @Override
        public boolean excluded(String field) {
            return false;
        }

        @Override
        public Iterator<String> iterator() {
            List<String> emptyList = Collections.emptyList();
            return emptyList.iterator();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean isAll() {
            return true;
        }
    };
    
    private enum TargetState {
        IdentCommaOpen,
        Ident,
        Comma,
        Anything
    }

    private enum FieldState {
        start,
        name,
        end
    }


    private HashMap<String, ReturnFields> fields = new LinkedHashMap<>();
    
    
    
    public ReturnFields() {}
    
    public ReturnFields(String spec) {

        if (spec == null || spec.trim().length() == 0) {
            throw new IllegalArgumentException("Fields spec is null or empty!");
        }
        // parse the spec, building up the tree for nested children
        char[] buf = spec.toCharArray();
        StringBuilder token = new StringBuilder(buf.length);

        // stack for handling depth
        LinkedList<HashMap<String, ReturnFields>> specs = new LinkedList<>();
        specs.add(fields);

        // parser state
        FieldState fldState = FieldState.start;
        TargetState state = TargetState.Ident;

        int i;
        for (i = 0; i < buf.length; i++) {
            char c = buf[i];

            if (c == ',') {
                if (state == TargetState.Ident) {
                    error(spec, i);
                }
                if (fldState == FieldState.name) {
                    specs.getLast().put(token.toString(), null);
                    token.setLength(0);
                }
                state = TargetState.Ident;
                fldState = FieldState.start;
            } else if (c == '(') {
                if (state != TargetState.IdentCommaOpen && state != TargetState.Anything) {
                    error(spec, i);
                }
                ReturnFields sub = new ReturnFields();
                specs.getLast().put(token.toString(), sub);
                specs.add(sub.fields);
                token.setLength(0);

                state = TargetState.Ident;
                fldState = FieldState.start;
            } else if (c == ')') {
                if (state != TargetState.Anything) {
                    error(spec, i);
                }
                if (fldState == FieldState.name) {
                    specs.getLast().put(token.toString(), null);
                    token.setLength(0);

                }
                specs.removeLast();

                fldState = FieldState.end;
                state = specs.size() > 1 ? TargetState.Anything : TargetState.Comma;
            } else {
                token.append(c);
                if (fldState == FieldState.start) {
                    fldState = FieldState.name;
                    state = specs.size() > 1 ? TargetState.Anything : TargetState.IdentCommaOpen;
                }
            }
        }

        if (specs.size() > 1) {
            error(spec, i);
        }

        if (token.length() > 0) {
            specs.getLast().put(token.toString(), null);
        } else if (!(state == TargetState.Anything || state == TargetState.Comma)) {
            error(spec, i);
        }
    }

    private void error(String spec, int i) {
        throw new RuntimeException("Invalid fields specification at position " + i + ": " + spec);
    }

    
    
    /**
     * Get ReturnFields for a child field of JSONObject type.
     *
     * <p>For basic-typed fields this always returns null. Use included() for those.</p>
     *
     * @param field The child field name for nested returns.
     * @return ReturnFields for a child field
     */
    public ReturnFields child(String field) {
        ReturnFields returnFields = fields.get(field);
        if (returnFields == null) {
            returnFields = fields.get("*");
            if (returnFields == null) {
                returnFields = ReturnFields.NONE;
            }
        }
        return returnFields;
    }

    /**
     * Check to see if the field should be included in JSON response.
     *
     * <p>The check can be performed for any level of depth relative to current nesting level, by specifying multiple path segments.</p>
     *
     * @param pathSegments Segments to test in the tree of return fields.
     * @return true if the specified path should be part of JSON response or not
     */
    public boolean included(String... pathSegments) {

        if (pathSegments == null || pathSegments.length == 0) {
            throw new IllegalArgumentException("No path specified!");
        }
        ReturnFields current = this;

        for (String path : pathSegments) {
            if (current == null) {
                return false;
            }

            if (current.fields.containsKey("-" + path)) {
                return false;
            }
            if (current.fields.containsKey("*")) {
                return true;
            }
            if (!current.fields.containsKey(path)) {
                return false;
            }
            current = current.fields.get(path);
        }
        return true;
    }

    /**
     * Check to see if the field specified is set to be explicitly excluded.
     * @param field The field name to check
     * @return If the field was explicitly set to be excluded
     */
    public boolean excluded(String field) {
        if (fields.containsKey("-" + field)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Iterate over child fields to be included in response.
     *
     * <p>To get nested field specifier use child(name) passing the field name this iterator returns.</p>
     *
     * @return iterator over child fields to be included in response.
     */
    public Iterator<String> iterator() {
        return fields.keySet().iterator();
    }

    /**
     * Determine if zero fields should be returned.
     *
     * @return <code>true</code> if the list is empty, else, <code>false</code>
     */
    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    public boolean isAll() {
        return this.fields.keySet().contains("*");
    }

    @Override
    public String toString() {
        return "[ReturnFieldsImpl: fields=" + this.fields + "]";
    }
}
