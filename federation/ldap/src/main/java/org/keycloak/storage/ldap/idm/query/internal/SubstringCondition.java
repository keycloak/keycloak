/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.ldap.idm.query.internal;

import java.util.Arrays;

/**
 * <p>Substring condition for ldap filters, <em>attrname=*some*thing*</em> for
 * example. The filter is created <em>attrname=[start]*[middle1]*[middle2]*[middleN]*[end]</em>.
 * At least one property (start, middle or end) should contain a non-empty
 * string. The middle array should not contain any null or empty string.</p>
 *
 * @author rmartinc
 */
public class SubstringCondition extends NamedParameterCondition {

    private final String start;
    private final String[] middle;
    private final String end;

    public SubstringCondition(String name, String start, String[] middle, String end) {
        super(name);
        this.start = start;
        this.middle = middle;
        this.end = end;
    }

    @Override
    public void applyCondition(StringBuilder filter) {
        filter.append("(").append(getParameterName()).append("=");
        if (start != null && !start.isEmpty()) {
            filter.append(escapeValue(start));
        }
        filter.append("*");
        if (middle != null && middle.length > 0) {
            Arrays.stream(middle).forEach(s -> filter.append(escapeValue(s)).append("*"));
        }
        if (end != null && !end.isEmpty()) {
            filter.append(escapeValue(end));
        }
        filter.append(")");
    }

    @Override
    public String toString() {
        return "PresentCondition{"
                + "paramName=" + getParameterName()
                + ", start=" + start
                + ", middle=" + (middle == null? "null" : Arrays.asList(middle))
                + ", end=" + end
                + '}';
    }
}
