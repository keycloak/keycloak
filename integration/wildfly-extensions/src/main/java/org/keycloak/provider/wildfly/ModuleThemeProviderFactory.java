package org.keycloak.provider.wildfly;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.keycloak.Config;
import org.keycloak.theme.JarThemeProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ModuleThemeProviderFactory extends JarThemeProviderFactory {

    @Override
    public void init(Config.Scope config) {
        String[] modules = config.getArray("modules");
        if (modules != null) {
            try {
                for (String moduleSpec : modules) {
                    Module module = Module.getContextModuleLoader().loadModule(ModuleIdentifier.fromString(moduleSpec));
                    ModuleClassLoader classLoader = module.getClassLoader();
                    loadThemes(classLoader, classLoader.getResourceAsStream(KEYCLOAK_THEMES_JSON));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load themes", e);
            }
        }
    }

    @Override
    public String getId() {
        return "module";
    }

}
