/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
