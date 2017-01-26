package org.keycloak.testsuite.runonserver;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;

/**
 * Created by st on 26.01.17.
 */
public class ModuleUtil {

    private static boolean modules;

    static {
        try {
            Module.getContextModuleLoader().loadModule(ModuleIdentifier.fromString("org.wildfly.common"));
            modules = true;
        } catch (Throwable t) {
            modules = false;
        }
    }

    public static boolean isModules() {
        return modules;
    }

    public static ClassLoader getClassLoader() {
        try {
            Module m = Module.getContextModuleLoader().loadModule(ModuleIdentifier.fromString("deployment.run-on-server-classes.war"));
            return m.getClassLoader();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load 'deployment.run-on-server-classes.war', did you include RunOnServerDeployment?", e);
        }
    }

}
