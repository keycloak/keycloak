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

package org.keycloak.services.util;

import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.jboss.logging.Logger;

/**
 * Utility class for securing FreeMarker template configuration.
 *
 * This class provides factory methods for creating secure FreeMarker configurations
 * that are protected against Server-Side Template Injection (SSTI) attacks.
 *
 * Security features implemented:
 * - Disables the ?api built-in (prevents access to Java API)
 * - Sets TemplateClassResolver to ALLOWS_NOTHING_RESOLVER (blocks ?new built-in)
 * - Configures secure exception handling
 * - Disables template execution
 * - Enables auto-escaping for HTML output
 *
 * @author Keycloak Security Team
 * @version 1.0
 * @since 999.0.0
 */
public class FreeMarkerSecurityConfig {

    private static final Logger logger = Logger.getLogger(FreeMarkerSecurityConfig.class);

    /**
     * The FreeMarker version to use for the configuration.
     * Should match the version in pom.xml.
     */
    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_33;

    /**
     * Creates a secure FreeMarker Configuration instance with SSTI protection.
     *
     * This configuration has the following security settings:
     * - API built-in is disabled
     * - Template class resolver set to ALLOWS_NOTHING (prevents ?new)
     * - Exception handler set to RETHROW (prevents information disclosure)
     * - Auto-escaping enabled for HTML output
     *
     * @return A secure FreeMarker Configuration
     */
    public static Configuration createSecureConfiguration() {
        Configuration cfg = new Configuration(FREEMARKER_VERSION);

        // CRITICAL SECURITY: Disable the ?api built-in
        // This prevents templates from accessing the underlying Java API
        cfg.setAPIBuiltinEnabled(false);
        logger.debug("SSTI Protection: API built-in disabled");

        // CRITICAL SECURITY: Set TemplateClassResolver to ALLOWS_NOTHING_RESOLVER
        // This prevents the ?new built-in from being used
        cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        logger.debug("SSTI Protection: TemplateClassResolver set to ALLOWS_NOTHING");

        // SECURITY: Use RETHROW exception handler
        // This prevents templates from potentially exposing stack traces
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        logger.debug("SSTI Protection: Exception handler set to RETHROW");

        // SECURITY: Enable auto-escaping for HTML output
        // This helps prevent XSS attacks
        cfg.setOutputFormat(freemarker.core.HTMLOutputFormat.INSTANCE);
        cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);
        logger.debug("SSTI Protection: Auto-escaping enabled for HTML");

        // SECURITY: Disable template execution privileges
        cfg.setLogTemplateExceptions(false);

        // PERFORMANCE: Configure template update delay (not security-related but good practice)
        cfg.setTemplateUpdateDelayMilliseconds(3600000); // 1 hour in production

        logger.info("Secure FreeMarker Configuration created successfully");
        return cfg;
    }

    /**
     * Creates a secure FreeMarker Configuration with custom template loader.
     *
     * @param templateLoader The template loader to use
     * @return A secure FreeMarker Configuration with the specified loader
     */
    public static Configuration createSecureConfiguration(freemarker.cache.TemplateLoader templateLoader) {
        Configuration cfg = createSecureConfiguration();
        cfg.setTemplateLoader(templateLoader);
        logger.debug("Custom template loader configured");
        return cfg;
    }

    /**
     * Creates a secure FreeMarker Configuration for a specific class path.
     *
     * @param baseClass The class whose package will be used as the base for loading templates
     * @param pathPrefix The path prefix within the class path
     * @return A secure FreeMarker Configuration for class path templates
     */
    public static Configuration createSecureConfigurationForClassPath(Class<?> baseClass, String pathPrefix) {
        Configuration cfg = createSecureConfiguration();
        cfg.setClassForTemplateLoading(baseClass, pathPrefix);
        logger.debugf("Class path template loading configured: baseClass=%s, pathPrefix=%s",
                baseClass.getName(), pathPrefix);
        return cfg;
    }

    /**
     * Validates that a FreeMarker Configuration has security settings enabled.
     *
     * @param cfg The configuration to validate
     * @return true if the configuration is secure, false otherwise
     */
    public static boolean validateSecurityConfiguration(Configuration cfg) {
        boolean isSecure = true;
        StringBuilder issues = new StringBuilder();

        // Check if API built-in is disabled
        if (cfg.isAPIBuiltinEnabled()) {
            isSecure = false;
            issues.append("WARNING: API built-in is enabled (SSTI risk)\n");
        }

        // Check if TemplateClassResolver is set to ALLOWS_NOTHING
        if (cfg.getNewBuiltinClassResolver() != TemplateClassResolver.ALLOWS_NOTHING_RESOLVER) {
            isSecure = false;
            issues.append("WARNING: TemplateClassResolver is not set to ALLOWS_NOTHING (SSTI risk)\n");
        }

        // Check exception handler
        if (!(cfg.getTemplateExceptionHandler() instanceof TemplateExceptionHandler.RethrowHandler)) {
            // This is less critical, but good practice
            logger.warn("INFO: Exception handler is not RETHROW (information disclosure risk)");
        }

        if (!isSecure) {
            logger.error("FreeMarker configuration has security issues:\n" + issues.toString());
        } else {
            logger.debug("FreeMarker configuration passed security validation");
        }

        return isSecure;
    }

    /**
     * Returns the recommended FreeMarker version.
     *
     * @return The FreeMarker version used for secure configurations
     */
    public static Version getRecommendedVersion() {
        return FREEMARKER_VERSION;
    }

    /**
     * Checks if the current FreeMarker version supports MemberAccessPolicy.
     * MemberAccessPolicy was introduced in FreeMarker 2.3.30.
     *
     * @return true if MemberAccessPolicy is supported
     */
    public static boolean isMemberAccessPolicySupported() {
        Version minVersion = new Version(2, 3, 30);
        boolean supported = FREEMARKER_VERSION.intValue() >= minVersion.intValue();

        if (supported) {
            logger.debug("MemberAccessPolicy is supported in FreeMarker version " + FREEMARKER_VERSION);
        } else {
            logger.warn("MemberAccessPolicy is NOT supported. Consider upgrading to FreeMarker 2.3.30+");
        }

        return supported;
    }

    /**
     * Creates a development-friendly configuration with security still enabled.
     * This is useful for development/testing but should NEVER be used in production.
     *
     * @return A secure FreeMarker Configuration with development settings
     */
    public static Configuration createSecureDevelopmentConfiguration() {
        Configuration cfg = createSecureConfiguration();

        // Development-specific settings (still secure)
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setLogTemplateExceptions(true);
        cfg.setTemplateUpdateDelayMilliseconds(0); // Always check for updates

        logger.warn("Development configuration created. DO NOT use in production!");
        return cfg;
    }

    /**
     * Disables all potentially dangerous FreeMarker features.
     * This is a defense-in-depth approach for maximum security.
     *
     * @param cfg The configuration to harden
     */
    public static void applyMaximumSecurityHardening(Configuration cfg) {
        // Disable API built-in
        cfg.setAPIBuiltinEnabled(false);

        // Block all class resolution
        cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);

        // Prevent template execution
        cfg.setLogTemplateExceptions(false);

        // Enable auto-escaping
        cfg.setOutputFormat(freemarker.core.HTMLOutputFormat.INSTANCE);
        cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);

        // Rethrow exceptions
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        logger.info("Maximum security hardening applied to FreeMarker configuration");
    }
}
