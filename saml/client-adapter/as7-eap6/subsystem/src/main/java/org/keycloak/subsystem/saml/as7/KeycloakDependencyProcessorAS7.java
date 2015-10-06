package org.keycloak.subsystem.saml.as7;

import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class KeycloakDependencyProcessorAS7 extends KeycloakDependencyProcessor {

    private static final ModuleIdentifier KEYCLOAK_AS7_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-saml-as7-adapter");

    @Override
    protected void addPlatformSpecificModules(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        // ModuleDependency(ModuleLoader moduleLoader, ModuleIdentifier identifier, boolean optional, boolean export, boolean importServices, boolean userSpecified)
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_AS7_ADAPTER, false, false, true, false));
    }}
