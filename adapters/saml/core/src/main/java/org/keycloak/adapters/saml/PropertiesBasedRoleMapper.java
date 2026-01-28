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

package org.keycloak.adapters.saml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.keycloak.adapters.saml.config.parsers.ResourceLoader;

import org.jboss.logging.Logger;

/**
 * A {@link RoleMappingsProvider} implementation that uses a {@code properties} file to determine the mappings that should be applied
 * to the SAML principal and roles. It is always identified by the id {@code properties-based-role-mapper} in {@code keycloak-saml.xml}.
 * <p/>
 * This provider relies on two configuration properties that can be used to specify the location of the {@code properties} file
 * that will be used. First, it checks if the {@code properties.file.location} property has been specified, using the configured
 * value to locate the {@code properties} file in the filesystem. If the configured file is not located, the provider throws a
 * {@link RuntimeException}. The following snippet shows an example of provider using the {@code properties.file.configuration}
 * option to load the {@code roles.properties} file from the {@code /opt/mappers/} directory in the filesystem:
 *
 * <pre>
 *     <RoleMappingsProvider id="properties-based-role-mapper">
 *         <Property name="properties.file.location" value="/opt/mappers/roles.properties"/>
 *     </RoleMappingsProvider>
 * </pre>
 *
 * If the {@code properties.file.location} configuration property is not present, the provider checks the {@code properties.resource.location}
 * property, using the configured value to load the {@code properties} file from the WAR resource. If no value is found, it
 * finally attempts to load a file named {@code role-mappings.properties} from the {@code WEB-INF} directory of the application.
 * Failure to load the file from the resource will result in the provider throwing a {@link RuntimeException}. The following
 * snippet shows an example of provider using the {@code properties.resource.location} to load the {@code roles.properties}
 * file from the application's {@code /WEB-INF/conf/} directory:
 *
 * <pre>
 *     <RoleMappingsProvider id="properties-based-role-mapper">
 *         <Property name="properties.resource.location" value="/WEB-INF/conf/roles.properties"/>
 *     </RoleMappingsProvider>
 * </pre>
 *
 * The {@code properties} file can contain both roles and principals as keys, and a list of zero or more roles separated by comma
 * as values. When the {@code {@link #map(String, Set)}} method is called, the implementation iterates through the set of roles
 * that were extracted from the assertion and checks, for eache role, if a mapping exists. If the role maps to an empty role,
 * it is discarded. If it maps to a set of one or more different roles, then these roles are set in the result set. If no
 * mapping is found for the role then it is included as is in the result set.
 *
 * Once the roles have been processed, the implementation checks if the principal extracted from the assertion contains an entry
 * in the {@code properties} file. If a mapping for the principal exists, any roles listed as value are added to the result set. This
 * allows the assignment of extra roles to a principal.
 *
 * For example, consider the following {@code properties} file:
 *
 * <pre>
 *     # role to roles mappings
 *     samlRoleA=jeeRoleX,jeeRoleY
 *     samlRoleB=
 *
 *     # principal to roles mappings
 *     kc-user=jeeRoleZ
 * </pre>
 *
 * If the {@code {@link #map(String, Set)}} method is called with {@code kc-user} as principal and a set containing roles
 * {@code samlRoleA,samlRoleB,samlRoleC}, the result set will be formed by the roles {@code jeeRoleX,jeeRoleY,samlRoleC,jeeRoleZ}.
 * In this case, {@code samlRoleA} is mapped to two roles ({@code jeeRoleX,jeeRoleY}), {@code samlRoleB} is discarded as it is
 * mapped to an empty role, {@code samlRoleC} is used as is and the principal is also assigned {@code jeeRoleZ}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class PropertiesBasedRoleMapper implements RoleMappingsProvider {

    private static final Logger logger = Logger.getLogger(PropertiesBasedRoleMapper.class);

    public static final String PROVIDER_ID = "properties-based-role-mapper";

    private static final String PROPERTIES_FILE_LOCATION = "properties.file.location";

    private static final String PROPERTIES_RESOURCE_LOCATION = "properties.resource.location";

    private static final String DEFAULT_RESOURCE_LOCATION = "/WEB-INF/role-mappings.properties";

    private Properties roleMappings;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(final SamlDeployment deployment, final ResourceLoader loader, final Properties config) {

        this.roleMappings = new Properties();
        // try to load the properties from the filesystem first.
        String path = config.getProperty(PROPERTIES_FILE_LOCATION);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                try (FileInputStream is = new FileInputStream(file)){
                    this.roleMappings.load(is);
                    logger.debugf("Successfully loaded role mappings from %s", path);
                } catch (Exception e) {
                    logger.debugv(e, "Unable to load role mappings from %s", path);
                }
            } else {
                throw new RuntimeException("Unable to load role mappings from " + path + ": file does not exist in filesystem");
            }
        } else {
            // try to load the properties from the resource (WAR).
            path = config.getProperty(PROPERTIES_RESOURCE_LOCATION, DEFAULT_RESOURCE_LOCATION);
            InputStream is = loader.getResourceAsStream(path);
            if (is != null) {
                try {
                    this.roleMappings.load(is);
                    logger.debugf("Resource loader successfully loaded role mappings from %s", path);
                } catch (Exception e) {
                    logger.debugv(e, "Resource loader unable to load role mappings from %s", path);
                }
            } else {
                throw new RuntimeException("Unable to load role mappings from " + path + ": file does not exist in the resource");
            }
        }
    }

    @Override
    public Set<String> map(final String principalName, final Set<String> roles) {
        if (this.roleMappings == null || this.roleMappings.isEmpty())
            return roles;

        Set<String> resolvedRoles = new HashSet<>();
        // first check if we have role -> role(s) mappings.
        for (String role : roles) {
            if (this.roleMappings.containsKey(role)) {
                // role that was mapped to empty string is not considered (it is discarded from the set of specified roles).
                this.extractRolesIntoSet(role, resolvedRoles);
            } else {
                // no mapping found for role - add it as is.
                resolvedRoles.add(role);
            }
        }

        // now check if we have a principal -> role(s) mapping with additional roles to be added.
        if (this.roleMappings.containsKey(principalName)) {
            this.extractRolesIntoSet(principalName, resolvedRoles);
        }
        return resolvedRoles;
    }

    /**
     * Obtains the list of comma separated roles associated with the specified entry, trims any whitespaces from said roles
     * and adds them to the specified set.
     *
     * @param entry the entry in the properties file.
     * @param roles the {@link Set<String>} into which the extracted roles are to be added.
     */
    private void extractRolesIntoSet(final String entry, final Set<String> roles) {
        String value = this.roleMappings.getProperty(entry);
        if (!value.isEmpty()) {
            String[] mappedRoles = value.split(",");
            for (String mappedRole : mappedRoles) {
                String trimmedRole = mappedRole.trim();
                if (!trimmedRole.isEmpty()) {
                    roles.add(trimmedRole);
                }
            }
        }
    }
}
