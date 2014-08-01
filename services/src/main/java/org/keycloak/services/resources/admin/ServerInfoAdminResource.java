package org.keycloak.services.resources.admin;

import org.keycloak.audit.AuditListener;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.social.SocialProvider;
import org.keycloak.util.ProviderLoader;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoAdminResource {

    @Context
    private KeycloakSession session;

    /**
     * Returns a list of themes, social providers, auth providers, and audit listeners available on this server
     *
     * @return
     */
    @GET
    public ServerInfoRepresentation getInfo() {
        ServerInfoRepresentation info = new ServerInfoRepresentation();
        setSocialProviders(info);
        setThemes(info);
        setAuditListeners(info);
        return info;
    }

    private void setThemes(ServerInfoRepresentation info) {
        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        info.themes = new HashMap<String, List<String>>();

        for (Theme.Type type : Theme.Type.values()) {
            List<String> themes = new LinkedList<String>(themeProvider.nameSet(type));
            Collections.sort(themes);

            info.themes.put(type.toString().toLowerCase(), themes);
        }
    }

    private void setSocialProviders(ServerInfoRepresentation info) {
        info.socialProviders = new LinkedList<String>();
        for (SocialProvider p : ProviderLoader.load(SocialProvider.class)) {
            info.socialProviders.add(p.getId());
        }
        Collections.sort(info.socialProviders);
    }

    private void setAuditListeners(ServerInfoRepresentation info) {
        info.auditListeners = new LinkedList<String>();

        Set<String> providers = session.listProviderIds(AuditListener.class);
        if (providers != null) {
            info.auditListeners.addAll(providers);
        }
    }

    public static class ServerInfoRepresentation {

        private Map<String, List<String>> themes;

        private List<String> socialProviders;


        private List<String> auditListeners;

        public ServerInfoRepresentation() {
        }

        public Map<String, List<String>> getThemes() {
            return themes;
        }

        public List<String> getSocialProviders() {
            return socialProviders;
        }

        public List<String> getAuditListeners() {
            return auditListeners;
        }
    }

}
