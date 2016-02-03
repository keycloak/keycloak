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

package org.keycloak.subsystem.saml.as7.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class Spliterator implements Iterator<String> {
    private final String subject;
    private final char delimiter;
    private int i;

    Spliterator(final String subject, final char delimiter) {
        this.subject = subject;
        this.delimiter = delimiter;
        i = 0;
    }

    static Spliterator over(String subject, char delimiter) {
        return new Spliterator(subject, delimiter);
    }

    public boolean hasNext() {
        return i != -1;
    }

    public String next() {
        final int i = this.i;
        if (i == -1) {
            throw new NoSuchElementException();
        }
        int n = subject.indexOf(delimiter, i);
        try {
            return n == -1 ? subject.substring(i) : subject.substring(i, n);
        } finally {
            this.i = n == -1 ? -1 : n + 1;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
