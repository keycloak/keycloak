package org.keycloak.social;

import org.keycloak.util.ProviderLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SocialLoader {

    private SocialLoader() {
    }

    public static SocialProvider load(String id) {
        if (id == null) {
            throw new NullPointerException();
        }

        for (SocialProvider s : load()) {
            if (id.equals(s.getId())) {
                return s;
            }
        }

        return null;
    }

    public static Iterable<SocialProvider> load() {
        return ProviderLoader.load(SocialProvider.class);
    }

}
