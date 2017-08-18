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
package org.keycloak.client.admin.cli.common;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class AttributeKey {

    private static final int START = 0;
    private static final int QUOTED = 1;
    private static final int UNQUOTED = 2;
    private static final int END = 3;

    private List<Component> components;
    private boolean append;

    public AttributeKey() {
        components = Collections.emptyList();
    }

    public AttributeKey(String key) {
        if (key.endsWith("+")) {
            append = true;
            key = key.substring(0, key.length() - 1);
        }
        components = parse(key);
    }

    static List<Component> parse(String key) {

        if (key == null || "".equals(key)) {
            return Collections.emptyList();
        }

        List<Component> cs = new LinkedList<>();
        StringBuilder sb = new StringBuilder();
        int state = START;

        char[] buf = key.toCharArray();

        for (int pos = 0; pos < buf.length; pos++) {
            char c = buf[pos];

            if (state == START) {
                if ('\"' == c) {
                    state = QUOTED;
                } else if ('.' == c) {
                    throw new RuntimeException("Invalid attribute key: " + key + " (at position " + (pos + 1) + ")");
                } else {
                    state = UNQUOTED;
                    sb.append(c);
                }
            } else if (state == QUOTED) {
                if ('\"' == c) {
                    state = END;
                } else {
                    sb.append(c);
                }
            } else if (state == UNQUOTED || state == END) {
                if ('.' == c) {
                    state = START;
                    cs.add(new Component(sb.toString()));
                    sb.setLength(0);
                } else if (state == END || '\"' == c) {
                    throw new RuntimeException("Invalid attribute key: " + key + " (at position " + (pos + 1) + ")");
                } else {
                    sb.append(c);
                }
            }
        }

        boolean ok = false;
        if (sb.length() > 0) {
            if (state == UNQUOTED || state == END) {
                cs.add(new Component(sb.toString()));
                ok = true;
            }
        } else if (state == END) {
            ok = true;
        }

        if (!ok) {
            throw new RuntimeException("Invalid attribute key: " + key + " (at position " + (buf.length) + ")");
        }

        return Collections.unmodifiableList(cs);
    }

    public List<Component> getComponents() {
        return components;
    }

    public boolean isAppend() {
        return append;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Component c: components) {
            if (sb.length() > 0) {
                sb.append(".");
            }
            sb.append(c.toString());
        }
        return sb.toString();
    }



    public static class Component {

        private int index = -1;
        private String name;

        Component(String name) {
            if (name.endsWith("]")) {
                int pos = name.lastIndexOf("[", name.length() - 1);
                if (pos == -1) {
                    throw new RuntimeException("Invalid attribute key: " + name + " (']' not allowed here)");
                }
                String idx = name.substring(pos + 1, name.length() - 1);
                try {
                    index = Integer.parseInt(idx);
                } catch (Exception e) {
                    throw new RuntimeException("Invalid attribute key: " + name + " (Invalid array index: '[" + idx + "]')");
                }
                this.name = name.substring(0, pos);
            } else {
                this.name = name;
            }
        }

        public boolean isArray() {
            return index >= 0;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name + (index != -1 ? "[" + index + "]" : "");
        }
    }
}
