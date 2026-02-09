/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.util.ldap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;

/**
 * <p>Ranged interceptor to emulate the behavior of AD. AD has a limit in
 * the number of attributes that return (15000 by default in MaxValRange).
 * See this MS link for AD limits:</p>
 *
 * https://support.microsoft.com/en-us/help/315071/how-to-view-and-set-ldap-policy-in-active-directory-by-using-ntdsutil
 *
 * <p>And this other link to know how range attribute search works:</p>
 *
 * https://docs.microsoft.com/en-us/previous-versions/windows/desktop/ldap/searching-using-range-retrieval
 *
 * @author rmartinc
 */
public class RangedAttributeInterceptor extends BaseInterceptor {

    private static class RangedEntryFilteringCursor implements EntryFilteringCursor {

        private final EntryFilteringCursor c;
        private final String name;
        private final Integer min;
        private final Integer max;

        public RangedEntryFilteringCursor(EntryFilteringCursor c, String name, Integer min, Integer max) {
            this.c = c;
            this.name = name;
            this.min = min;
            this.max = max;
            AttributeType type = new AttributeType(name);
        }

        private Entry prepareEntry(Entry e) {
            Attribute attr = e.get(name);
            if (attr != null) {
                int start = (min != null)? min : 0;
                start = (start < attr.size())? start : attr.size() - 1;
                int end = (max != null && max < attr.size() - 1)? max : attr.size() - 1;
                if (start != 0 || end != attr.size() - 1) {
                    // some values should be stripped out
                    Iterator<Value> it = attr.iterator();
                    Set<Value> valuesToRemove = new HashSet<>(end - start + 1);
                    for (int i = 0; i < attr.size(); i++) {
                        Value v = it.next();
                        if (i < start || i > end) {
                            valuesToRemove.add(v);
                        }
                    }
                    attr.setUpId(attr.getUpId() + ";range=" + start + "-" + ((end == attr.size() - 1)? "*" : end));
                    attr.remove(valuesToRemove.toArray(new Value[0]));
                } else if (min != null) {
                    // range explicitly requested although no value stripped
                    attr.setUpId(attr.getUpId() + ";range=0-*");
                }
            }
            return e;
        }

        @Override
        public boolean addEntryFilter(EntryFilter ef) {
            return c.addEntryFilter(ef);
        }

        @Override
        public List<EntryFilter> getEntryFilters() {
            return c.getEntryFilters();
        }

        @Override
        public SearchOperationContext getOperationContext() {
            return c.getOperationContext();
        }

        @Override
        public boolean available() {
            return c.available();
        }

        @Override
        public void before(Entry e) throws LdapException, CursorException {
            c.before(e);
        }

        @Override
        public void after(Entry e) throws LdapException, CursorException {
            c.after(e);
        }

        @Override
        public void beforeFirst() throws LdapException, CursorException {
            c.beforeFirst();
        }

        @Override
        public void afterLast() throws LdapException, CursorException {
            c.afterLast();
        }

        @Override
        public boolean first() throws LdapException, CursorException {
            return c.first();
        }

        @Override
        public boolean isFirst() {
            return c.isFirst();
        }

        @Override
        public boolean isBeforeFirst() {
            return c.isBeforeFirst();
        }

        @Override
        public boolean last() throws LdapException, CursorException {
            return c.last();
        }

        @Override
        public boolean isLast() {
            return c.isLast();
        }

        @Override
        public boolean isAfterLast() {
            return c.isAfterLast();
        }

        @Override
        public boolean isClosed() {
            return c.isClosed();
        }

        @Override
        public boolean previous() throws LdapException, CursorException {
            return c.previous();
        }

        @Override
        public boolean next() throws LdapException, CursorException {
            return c.next();
        }

        @Override
        public Entry get() throws CursorException {
            return prepareEntry(c.get());
        }

        @Override
        public void close() throws IOException {
            c.close();
        }

        @Override
        public void close(Exception excptn) throws IOException {
            c.close(excptn);
        }

        @Override
        public void setClosureMonitor(ClosureMonitor cm) {
            c.setClosureMonitor(cm);
        }

        @Override
        public String toString(String string) {
            return c.toString(string);
        }

        @Override
        public Iterator<Entry> iterator() {
            return c.iterator();
        }
    }

    private final String name;
    private final int max;

    public RangedAttributeInterceptor(String name, int max) {
        this.name = name;
        this.max = max - 1;
    }

    @Override
    public EntryFilteringCursor search(SearchOperationContext sc) throws LdapException {
        Set<AttributeTypeOptions> attrs = sc.getReturningAttributes();
        Integer lmin = null, lmax = max;
        if (attrs != null) {
            for (AttributeTypeOptions attr : attrs) {
                if (attr.getAttributeType().getName().equalsIgnoreCase(name)) {
                    if (attr.getOptions() != null) {
                        for (String option : attr.getOptions()) {
                            if (option.startsWith("range=")) {
                                String[] ranges = option.substring(6).split("-");
                                if (ranges.length == 2) {
                                    try {
                                        lmin = Integer.parseInt(ranges[0]);
                                        if (lmin < 0) {
                                            lmin = 0;
                                        }
                                        if ("*".equals(ranges[1])) {
                                            lmax = lmin + max;
                                        } else {
                                            lmax = Integer.parseInt(ranges[1]);
                                            if (lmax < lmin) {
                                                lmax = lmin;
                                            } else if (lmax > lmin + max) {
                                                lmax = lmin + max;
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        lmin = null;
                                        lmax = max;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        return new RangedEntryFilteringCursor(super.next(sc), name, lmin, lmax);
    }
}
