package org.keycloak.freemarker;

import java.io.IOException;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ThemeProvider {

    public Theme createTheme(String name, Theme.Type type) throws IOException;

    public Set<String> nameSet(Theme.Type type);

    public boolean hasTheme(String name, Theme.Type type);

}
