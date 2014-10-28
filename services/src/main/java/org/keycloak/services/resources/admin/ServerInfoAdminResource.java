package org.keycloak.services.resources.admin;

import org.keycloak.Version;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.exportimport.ApplicationImporter;
import org.keycloak.exportimport.ApplicationImporterFactory;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.provider.ProviderFactory;
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
     * Returns a list of themes, social providers, auth providers, and event listeners available on this server
     *
     * @return
     */
    @GET
    public ServerInfoRepresentation getInfo() {
        ServerInfoRepresentation info = new ServerInfoRepresentation();
        info.setVersion(Version.VERSION);
        setSocialProviders(info);
        setThemes(info);
        setEventListeners(info);
        setProtocols(info);
        setApplicationImporters(info);
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

    private void setEventListeners(ServerInfoRepresentation info) {
        info.eventListeners = new LinkedList<String>();

        Set<String> providers = session.listProviderIds(EventListenerProvider.class);
        if (providers != null) {
            info.eventListeners.addAll(providers);
        }
    }


    private void setProtocols(ServerInfoRepresentation info) {
        info.protocols = new LinkedList<String>();
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(LoginProtocol.class)) {
            info.protocols.add(p.getId());
        }
        Collections.sort(info.protocols);
    }

    private void setApplicationImporters(ServerInfoRepresentation info) {
        info.applicationImporters = new LinkedList<Map<String, String>>();
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(ApplicationImporter.class)) {
            ApplicationImporterFactory factory = (ApplicationImporterFactory)p;
            Map<String, String> data = new HashMap<String, String>();
            data.put("id", factory.getId());
            data.put("name", factory.getDisplayName());
            info.applicationImporters.add(data);
        }
    }

    public static class ServerInfoRepresentation {

        private String version;

        private Map<String, List<String>> themes;

        private List<String> socialProviders;
        private List<String> protocols;
        private List<Map<String, String>> applicationImporters;


        private List<String> eventListeners;

        public ServerInfoRepresentation() {
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Map<String, List<String>> getThemes() {
            return themes;
        }

        public List<String> getSocialProviders() {
            return socialProviders;
        }

        public List<String> getEventListeners() {
            return eventListeners;
        }

        public List<String> getProtocols() {
            return protocols;
        }

        public List<Map<String, String>> getApplicationImporters() {
            return applicationImporters;
        }
    }

}
