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
package org.keycloak.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * A utility class for replacing properties in strings.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="Scott.Stark@jboss.org">Scott Stark</a>
 * @author <a href="claudio.vesco@previnet.it">Claudio Vesco</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * @version <tt>$Revision: 2898 $</tt>
 */
public final class StringPropertyReplacer
{

    private static final Logger logger = Logger.getLogger(StringPropertyReplacer.class);

    /** File separator value */
    private static final String FILE_SEPARATOR = File.separator;

    /** Path separator value */
    private static final String PATH_SEPARATOR = File.pathSeparator;

    /** File separator alias */
    private static final String FILE_SEPARATOR_ALIAS = "/";

    /** Path separator alias */
    private static final String PATH_SEPARATOR_ALIAS = ":";

    private static final PropertyResolver NULL_RESOLVER = property -> null;
    private static PropertyResolver DEFAULT_PROPERTY_RESOLVER;

    private static final int MAX_KEY_LENGTH = 1<<22;

    public static void setDefaultPropertyResolver(PropertyResolver systemVariables) {
        DEFAULT_PROPERTY_RESOLVER = systemVariables;
    }

    /**
     * Go through the input string and replace any occurrence of ${p} with
     * the System.getProperty(p) value. If there is no such property p defined,
     * then the ${p} reference will remain unchanged.
     *
     * If the property reference is of the form ${p:v} and there is no such property p,
     * then the default value v will be returned.
     *
     * If the property reference is of the form ${p1,p2} or ${p1,p2:v} then
     * the primary and the secondary properties will be tried in turn, before
     * returning either the unchanged input, or the default value.
     *
     * The property ${/} is replaced with System.getProperty("file.separator")
     * value and the property ${:} is replaced with System.getProperty("path.separator").
     *
     * @param string - the string with possible ${} references
     * @return the input string with all property references replaced if any.
     *    If there are no valid references the input string will be returned.
     */
    public static String replaceProperties(final String string) {
        return replaceProperties(string, getDefaultPropertyResolver());
    }

    /**
     * Go through the input string and replace any occurrence of ${p} with
     * the value resolves from {@code resolver}. If there is no such property p defined,
     * then the ${p} reference will remain unchanged.
     *
     * If the property reference is of the form ${p:v} and there is no such property p,
     * then the default value v will be returned.
     *
     * If the property reference is of the form ${p1,p2} or ${p1,p2:v} then
     * the primary and the secondary properties will be tried in turn, before
     * returning either the unchanged input, or the default value.
     *
     * The property ${/} is replaced with System.getProperty("file.separator")
     * value and the property ${:} is replaced with System.getProperty("path.separator").
     *
     * @param string - the string with possible ${} references
     * @param resolver - the property resolver
     * @return the input string with all property references replaced if any.
     *    If there are no valid references the input string will be returned.
     */
    public static String replaceProperties(final String string, PropertyResolver resolver) {
        if (string == null) {
            return null;
        }
        int index = string.indexOf("${");
        if (index == -1) {
            return string;
        }
        try {
            return string.substring(0, index).concat(StreamUtil
                    .readString(replaceProperties(new ByteArrayInputStream(string.substring(index).getBytes(StandardCharsets.UTF_8)),
                            resolver), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e); // not expected
        }
    }

    public static InputStream replaceProperties(final InputStream source, PropertyResolver resolver) {
        return replaceProperties(source, false, resolver);
    }

    private static InputStream replaceProperties(final InputStream source, boolean readUntilCurlyBrace, PropertyResolver resolver) {
        return new InputStream() {
            private ByteArrayInputStream buffer;
            private boolean closed;

            @Override
            public int read() throws IOException {
                if (closed) {
                    throw new IOException("Stream closed");
                }
                // read off of the buffer first if possible
                if (buffer != null) {
                    int c = buffer.read();
                    if (c != -1) {
                        return c;
                    }
                    buffer = null;
                }
                // if no buffer, scan for } or ${
                int c = source.read();
                if (c == '}' && readUntilCurlyBrace) {
                    return -2;
                }
                if (c != '$') {
                    return c;
                }
                int next = source.read();
                if (next != '{') {
                    buffer = new ByteArrayInputStream(new byte[] {(byte)c, (byte)next});
                    return read();
                }
                // determine the key
                int keyChar = -1;
                InputStream keyStream = replaceProperties(source, true, resolver);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while ((keyChar = keyStream.read()) > -1) {
                    bytes.write((byte)keyChar);
                    if (bytes.size() == MAX_KEY_LENGTH) {
                        logger.log(Level.WARN, "Detected an unclosed ${, replacement will not be performed");
                        keyChar = -1;
                        break;
                    }
                }
                String keyString = bytes.toString(StandardCharsets.UTF_8.name());
                String replacement = null;
                if (keyChar == -1) {
                    // eof before } - prepend ${ and output directly
                    replacement = "${" + keyString;
                } else {
                    replacement = replaceProperty(resolver, keyString);
                    if (replacement == null) {
                        replacement = "${" + keyString + "}";
                    } else {
                        try {
                            replacement = replaceProperties(replacement, resolver);
                        } catch (StackOverflowError ex) {
                            throw new IllegalStateException("Infinite recursion happening when replacing properties on '" + replacement + "'");
                        }
                    }
                }
                buffer = new ByteArrayInputStream(replacement.getBytes(StandardCharsets.UTF_8));
                return read();
            }

            @Override
            public void close() throws IOException {
                closed = true;
                source.close();
            }
        };
    }

    private static String replaceProperty(PropertyResolver resolver, String key) {
        String value = null;

        // check for alias
        if (FILE_SEPARATOR_ALIAS.equals(key))
        {
            value = FILE_SEPARATOR;
        }
        else if (PATH_SEPARATOR_ALIAS.equals(key))
        {
            value = PATH_SEPARATOR;
        }
        else
        {
            // check from the properties
            value = resolveValue(resolver, key);

            if (value == null)
            {
                // Check for a default value ${key:default}
                int colon = key.indexOf(':');
                if (colon > 0)
                {
                    String realKey = key.substring(0, colon);
                    value = resolveValue(resolver, realKey);

                    if (value == null)
                    {
                        // Check for a composite key, "key1,key2"
                        value = resolveCompositeKey(realKey, resolver);

                        // Not a composite key either, use the specified default
                        if (value == null) {
                            value = key.substring(colon+1);
                        }
                    }
                }
                else
                {
                    // No default, check for a composite key, "key1,key2"
                    value = resolveCompositeKey(key, resolver);
                }
            }
        }

        return value;
    }

    private static String resolveCompositeKey(String key, PropertyResolver resolver)
    {
        String value = null;

        // Look for the comma
        int comma = key.indexOf(',');
        if (comma > -1)
        {
            // If we have a first part, try resolve it
            if (comma > 0)
            {
                // Check the first part
                String key1 = key.substring(0, comma);
                value = resolveValue(resolver, key1);
            }
            // Check the second part, if there is one and first lookup failed
            if (value == null && comma < key.length() - 1)
            {
                String key2 = key.substring(comma + 1);
                value = resolveValue(resolver, key2);
            }
        }
        // Return whatever we've found or null
        return value;
    }

    public interface PropertyResolver {
        String resolve(String property);
    }

    private static String resolveValue(PropertyResolver resolver, String key) {
        if (resolver == null) {
            return getDefaultPropertyResolver().resolve(key);
        }

        return resolver.resolve(key);
    }

    private static PropertyResolver getDefaultPropertyResolver() {
        return Optional.ofNullable(DEFAULT_PROPERTY_RESOLVER).orElse(NULL_RESOLVER);
    }
}
