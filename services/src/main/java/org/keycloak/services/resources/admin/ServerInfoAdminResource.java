package org.keycloak.services.resources.admin;

import org.keycloak.Version;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.exportimport.ApplicationImporter;
import org.keycloak.exportimport.ApplicationImporterFactory;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.social.SocialIdentityProvider;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
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
        info.version = Version.VERSION;
        info.serverTime = new Date().toString();
        setSocialProviders(info);
        setIdentityProviders(info);
        setThemes(info);
        setEventListeners(info);
        setProtocols(info);
        setApplicationImporters(info);
        setProviders(info);
        return info;
    }

    private void setProviders(ServerInfoRepresentation info) {
        Map<String, Set<String>> providers = new HashMap<String, Set<String>>();
        for (Spi spi : ServiceLoader.load(Spi.class)) {
            providers.put(spi.getName(), session.listProviderIds(spi.getProviderClass()));
        }
        info.providers = providers;
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
        info.socialProviders = new LinkedList<IdentityProviderRepresentation>();
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.socialProviders, "Social");
    }

    private void setIdentityProviders(ServerInfoRepresentation info) {
        info.identityProviders = new LinkedList<IdentityProviderRepresentation>();
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class);
        setIdentityProviders(providerFactories, info.identityProviders, "User-defined");

        providerFactories = session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.identityProviders, "Social");
    }

    public void setIdentityProviders(List<ProviderFactory> factories, List<IdentityProviderRepresentation> providers, String groupName) {
        for (ProviderFactory providerFactory : factories) {
            IdentityProviderFactory factory = (IdentityProviderFactory) providerFactory;
            IdentityProviderRepresentation rep = new IdentityProviderRepresentation();

            rep.setId(factory.getId());
            rep.setName(factory.getName());
            rep.setGroupName(groupName);

            providers.add(rep);
        }

        Collections.sort(providers, new Comparator<IdentityProviderRepresentation>() {
            @Override
            public int compare(IdentityProviderRepresentation o1, IdentityProviderRepresentation o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
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

        private String serverTime;

        private Map<String, List<String>> themes;

        private List<IdentityProviderRepresentation> socialProviders;
        public List<IdentityProviderRepresentation> identityProviders;
        private List<String> protocols;
        private List<Map<String, String>> applicationImporters;

        private Map<String, Set<String>> providers;

        private List<String> eventListeners;

        public ServerInfoRepresentation() {
        }

        public String getServerTime() {
            return serverTime;
        }

        public String getVersion() {
            return version;
        }

        public Map<String, List<String>> getThemes() {
            return themes;
        }

        public List<IdentityProviderRepresentation> getSocialProviders() {
            return socialProviders;
        }

        public List<IdentityProviderRepresentation> getIdentityProviders() {
            return this.identityProviders;
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

        public Map<String, Set<String>> getProviders() {
            return providers;
        }
    }

}
