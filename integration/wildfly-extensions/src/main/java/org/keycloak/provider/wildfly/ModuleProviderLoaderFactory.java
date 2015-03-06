package org.keycloak.provider.wildfly;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.keycloak.provider.DefaultProviderLoader;
import org.keycloak.provider.ProviderLoader;
import org.keycloak.provider.ProviderLoaderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ModuleProviderLoaderFactory implements ProviderLoaderFactory {

    @Override
    public boolean supports(String type) {
        return "module".equals(type);
    }

    @Override
    public ProviderLoader create(ClassLoader baseClassLoader, String resource) {
        try {
            Module module = Module.getContextModuleLoader().loadModule(ModuleIdentifier.fromString(resource));
            ModuleClassLoader classLoader = module.getClassLoader();
            return new DefaultProviderLoader(classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
