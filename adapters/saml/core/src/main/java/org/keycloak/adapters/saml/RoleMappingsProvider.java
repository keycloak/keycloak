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

import java.util.Properties;
import java.util.Set;

import org.keycloak.adapters.saml.config.parsers.ResourceLoader;

/**
 * A simple SPI for mapping SAML roles into roles that exist in the SP application environment. The roles returned by an external
 * IDP might not always correspond to the roles that were defined for the application so there is a need for a mechanism that
 * allows mapping the SAML roles into different roles. It is used by the SAML adapter after it extracts the roles from the SAML
 * assertion to set up the container's security context.
 * <p/>
 * This SPI doesn't impose any restrictions on the mappings that can be performed. Implementations can not only map roles into
 * other roles but also add or remove roles (and thus augmenting/reducing the number of roles assigned to the SAML principal)
 * depending on the use case.
 * <p/>
 * To install a custom role mappings provider, a {@code META-INF/services/org.keycloak.adapters.saml.RoleMappingsProvider} file
 * containing the FQN of the custom implementation class must be added to the WAR that contains the provider implementation
 * class (or the JAR that is attached to the {@code WEB-INF/lib} or as a {@code jboss module} if one wants to share the
 * implementation among more WARs).
 * <p/>
 * The role mappings provider implementation that will be selected for the SP application is identified in the {@code keycloak-saml.xml}
 * by its id. The provider declaration can also contain one or more configuration properties that will be passed to the implementation
 * in the {@code {@link #init(SamlDeployment, ResourceLoader, Properties)}} method. For example, if an LDAP-based implementation
 * with id {@code ldap-based-role-mapper} is made available via {@code META-INF/services}, it can be selected in {@code keycloak-saml.xml}
 * as follows:
 *
 * <pre>
 *     ...
 *     <RoleIdentifiers>
 *         ...
 *     </RoleIdentifiers>
 *     <RoleMappingsProvider id="ldap-based-role-mapper">
 *         <Property name="connection.url" value="some.url"/>
 *         <Property name="username" value="some.user"/>
 *         ...
 *     </RoleMappingsProvider>
 * </pre>
 *
 * NOTE: The SPI is not yet finished and method signatures are still subject to change in future versions.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public interface RoleMappingsProvider {

    /**
     * Obtains the provider's identifier. This id is specified in {@code keycloak-saml.xml} to identify the provider implementation
     * to be used.
     *
     * @return a {@link String} representing the provider's id.
     */
    String getId();

    /**
     * Initializes the provider. This method is called by the adapter in deployment time after the contents of {@code keycloak-saml.xml}
     * have been parsed and a provider whose id matches the one in the descriptor is successfully loaded.
     *
     * @param deployment a reference to the constructed {@link SamlDeployment}.
     * @param loader a reference to a {@link ResourceLoader} that can be used to load additional resources from the WAR.
     * @param config a {@link Properties} object containing the provider config as read from {@code keycloak-saml.xml}
     */
    void init(final SamlDeployment deployment, final ResourceLoader loader, final Properties config);

    /**
     * Produces the final set of roles that should be assigned to the specified principal. This method makes the principal
     * and roles that were read from the SAML assertion available to implementations so they can apply their specific logic
     * to produce the final set of roles for the principal.
     *
     * This method imposes no restrictions on the kind of mappings that can be performed. A simple implementation may, for
     * example, just use a properties file to map some of the assertion roles into JEE roles while a more complex implementation
     * may also connect to external databases or LDAP servers to retrieve extra roles and add those roles to the set of
     * roles already extracted from the assertion.
     *
     * @param principalName the principal name as extracted from the SAML assertion.
     * @param roles the set of roles extracted from the SAML assertion.
     * @return a {@link Set<String>} containing the final set of roles that are to be assigned to the principal.
     */
    Set<String> map(final String principalName, final Set<String> roles);
}
