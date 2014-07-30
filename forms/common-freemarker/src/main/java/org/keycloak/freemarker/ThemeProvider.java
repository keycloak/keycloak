package org.keycloak.freemarker;

import org.keycloak.provider.Provider;

import java.io.IOException;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ThemeProvider extends Provider {

    public int getProviderPriority();

    public Theme getTheme(String name, Theme.Type type) throws IOException;

    public Set<String> nameSet(Theme.Type type);

    public boolean hasTheme(String name, Theme.Type type);

}
