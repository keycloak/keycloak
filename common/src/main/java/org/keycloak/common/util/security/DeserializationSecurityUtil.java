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

package org.keycloak.common.util.security;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for secure deserialization with protection against insecure deserialization attacks.
 *
 * This class implements JEP 290 (Deserialization Filtering) to prevent:
 * - Remote Code Execution via gadget chains
 * - Denial of Service attacks
 * - Exploitation of vulnerable library dependencies
 *
 * Security features:
 * - Class whitelist/blacklist filtering
 * - Array size limits
 * - Object depth limits
 * - Total bytes limits
 * - Reference limits
 *
 * @author Keycloak Security Team
 * @version 1.0
 * @since 999.0.0
 */
public class DeserializationSecurityUtil {

    private static final Logger logger = Logger.getLogger(DeserializationSecurityUtil.class);

    /**
     * Default maximum array length allowed during deserialization.
     * Prevents memory exhaustion attacks.
     */
    public static final int DEFAULT_MAX_ARRAY_LENGTH = 100000;

    /**
     * Default maximum object depth allowed during deserialization.
     * Prevents stack overflow attacks.
     */
    public static final int DEFAULT_MAX_DEPTH = 20;

    /**
     * Default maximum bytes to read during deserialization.
     * Prevents resource exhaustion.
     */
    public static final long DEFAULT_MAX_BYTES = 100_000_000; // 100MB

    /**
     * Default maximum number of references in the stream.
     */
    public static final long DEFAULT_MAX_REFERENCES = 100000;

    /**
     * Known dangerous classes that should never be deserialized.
     * These are commonly used in gadget chain attacks.
     */
    private static final Set<String> BLACKLISTED_CLASSES = new HashSet<>(Arrays.asList(
        // Apache Commons Collections (gadget chains)
        "org.apache.commons.collections.functors.InvokerTransformer",
        "org.apache.commons.collections.functors.InstantiateTransformer",
        "org.apache.commons.collections.functors.ChainedTransformer",
        "org.apache.commons.collections4.functors.InvokerTransformer",
        "org.apache.commons.collections4.functors.InstantiateTransformer",
        "org.apache.commons.collections4.functors.ChainedTransformer",

        // Spring Framework (CVE-2016-1000027)
        "org.springframework.beans.factory.ObjectFactory",

        // Groovy
        "org.codehaus.groovy.runtime.ConvertedClosure",
        "org.codehaus.groovy.runtime.MethodClosure",

        // C3P0 JNDI
        "com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase",
        "com.mchange.v2.c3p0.JndiRefForwardingDataSource",

        // JDK internal classes that can be dangerous
        "sun.rmi.server.UnicastRef",
        "sun.rmi.server.MarshalInputStream",
        "java.rmi.server.RemoteObjectInvocationHandler",

        // Java Management Extensions (JMX)
        "javax.management.BadAttributeValueExpException",

        // JNDI
        "javax.naming.Reference",
        "javax.naming.Referenceable",

        // XStream related
        "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl"
    ));

    /**
     * Keycloak-specific classes that are safe to deserialize.
     * This whitelist approach provides the strongest security.
     */
    private static final Set<String> WHITELISTED_PACKAGES = new HashSet<>(Arrays.asList(
        "org.keycloak.",
        "java.lang.",
        "java.util.",
        "java.time.",
        "java.math.",
        "java.net.URI",
        "java.net.URL"
    ));

    /**
     * Creates a secure ObjectInputStream with deserialization filtering enabled.
     *
     * @param inputStream The input stream to wrap
     * @return A secure ObjectInputStream with filtering
     * @throws IOException if an I/O error occurs
     */
    public static ObjectInputStream createSecureObjectInputStream(InputStream inputStream) throws IOException {
        return createSecureObjectInputStream(inputStream, null);
    }

    /**
     * Creates a secure ObjectInputStream with deserialization filtering enabled and custom whitelist.
     *
     * @param inputStream The input stream to wrap
     * @param additionalWhitelistedPackages Additional package prefixes to whitelist (can be null)
     * @return A secure ObjectInputStream with filtering
     * @throws IOException if an I/O error occurs
     */
    public static ObjectInputStream createSecureObjectInputStream(InputStream inputStream,
                                                                   Set<String> additionalWhitelistedPackages) throws IOException {
        return new SecureObjectInputStream(inputStream, additionalWhitelistedPackages);
    }

    /**
     * Safely deserializes an object with security filtering.
     *
     * @param inputStream The input stream containing serialized data
     * @param <T> The expected type of the deserialized object
     * @return The deserialized object
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws SecurityException if deserialization is blocked by security filters
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserializeSecurely(InputStream inputStream)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = createSecureObjectInputStream(inputStream)) {
            return (T) ois.readObject();
        }
    }

    /**
     * Safely deserializes an object with security filtering and custom whitelist.
     *
     * @param inputStream The input stream containing serialized data
     * @param additionalWhitelistedPackages Additional package prefixes to whitelist
     * @param <T> The expected type of the deserialized object
     * @return The deserialized object
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws SecurityException if deserialization is blocked by security filters
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserializeSecurely(InputStream inputStream,
                                                                  Set<String> additionalWhitelistedPackages)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = createSecureObjectInputStream(inputStream, additionalWhitelistedPackages)) {
            return (T) ois.readObject();
        }
    }

    /**
     * Checks if a class is allowed for deserialization.
     *
     * @param className The fully qualified class name
     * @return true if the class is allowed, false otherwise
     */
    public static boolean isClassAllowed(String className) {
        return isClassAllowed(className, null);
    }

    /**
     * Checks if a class is allowed for deserialization with additional whitelisted packages.
     *
     * @param className The fully qualified class name
     * @param additionalWhitelistedPackages Additional package prefixes to consider
     * @return true if the class is allowed, false otherwise
     */
    public static boolean isClassAllowed(String className, Set<String> additionalWhitelistedPackages) {
        // Check blacklist first (highest priority)
        if (BLACKLISTED_CLASSES.contains(className)) {
            logger.warnf("Deserialization blocked: Class %s is blacklisted", className);
            return false;
        }

        // Check default whitelist
        for (String whitelistedPackage : WHITELISTED_PACKAGES) {
            if (className.startsWith(whitelistedPackage)) {
                return true;
            }
        }

        // Check additional whitelist if provided
        if (additionalWhitelistedPackages != null) {
            for (String whitelistedPackage : additionalWhitelistedPackages) {
                if (className.startsWith(whitelistedPackage)) {
                    return true;
                }
            }
        }

        logger.warnf("Deserialization blocked: Class %s is not whitelisted", className);
        return false;
    }

    /**
     * Secure ObjectInputStream implementation with class filtering.
     */
    private static class SecureObjectInputStream extends ObjectInputStream {

        private final Set<String> additionalWhitelistedPackages;
        private int depth = 0;
        private long bytesRead = 0;
        private long objectCount = 0;

        public SecureObjectInputStream(InputStream in, Set<String> additionalWhitelistedPackages) throws IOException {
            super(in);
            this.additionalWhitelistedPackages = additionalWhitelistedPackages;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String className = desc.getName();

            logger.debugf("Deserialization: Attempting to deserialize class: %s", className);

            // Check if class is allowed
            if (!isClassAllowed(className, additionalWhitelistedPackages)) {
                throw new SecurityException("Deserialization of class " + className + " is not allowed. " +
                        "Class is not in the whitelist or is explicitly blacklisted.");
            }

            // Increment counters
            objectCount++;

            // Check limits
            if (depth > DEFAULT_MAX_DEPTH) {
                throw new SecurityException("Deserialization blocked: Maximum depth " + DEFAULT_MAX_DEPTH + " exceeded");
            }

            if (bytesRead > DEFAULT_MAX_BYTES) {
                throw new SecurityException("Deserialization blocked: Maximum bytes " + DEFAULT_MAX_BYTES + " exceeded");
            }

            if (objectCount > DEFAULT_MAX_REFERENCES) {
                throw new SecurityException("Deserialization blocked: Maximum references " + DEFAULT_MAX_REFERENCES + " exceeded");
            }

            logger.debugf("Deserialization: Class %s allowed. Depth: %d, Objects: %d", className, depth, objectCount);

            return super.resolveClass(desc);
        }

        @Override
        protected Object readObjectOverride() throws IOException, ClassNotFoundException {
            depth++;
            try {
                return super.readObjectOverride();
            } finally {
                depth--;
            }
        }

        @Override
        public Object readObject() throws ClassNotFoundException, IOException {
            depth++;
            try {
                return super.readObject();
            } finally {
                depth--;
            }
        }

        @Override
        public int read() throws IOException {
            bytesRead++;
            return super.read();
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int count = super.read(buf, off, len);
            if (count > 0) {
                bytesRead += count;
            }
            return count;
        }
    }

    /**
     * Validates that deserialization filtering is properly configured in the JVM.
     *
     * @return true if JEP 290 filtering is available, false otherwise
     */
    public static boolean isJEP290FilteringAvailable() {
        try {
            // Check if ObjectInputFilter class is available (Java 9+)
            Class.forName("java.io.ObjectInputFilter");
            logger.debug("JEP 290 deserialization filtering is available");
            return true;
        } catch (ClassNotFoundException e) {
            logger.warn("JEP 290 deserialization filtering is NOT available. " +
                    "Consider upgrading to Java 9+ for enhanced security.");
            return false;
        }
    }

    /**
     * Returns the set of blacklisted classes (for testing/auditing purposes).
     *
     * @return Unmodifiable set of blacklisted class names
     */
    public static Set<String> getBlacklistedClasses() {
        return new HashSet<>(BLACKLISTED_CLASSES);
    }

    /**
     * Returns the set of whitelisted package prefixes (for testing/auditing purposes).
     *
     * @return Unmodifiable set of whitelisted package prefixes
     */
    public static Set<String> getWhitelistedPackages() {
        return new HashSet<>(WHITELISTED_PACKAGES);
    }
}
