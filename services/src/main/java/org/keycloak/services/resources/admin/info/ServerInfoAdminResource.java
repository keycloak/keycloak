package org.keycloak.services.resources.admin.info;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.exportimport.ClientImporter;
import org.keycloak.exportimport.ClientImporterFactory;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperTypeRepresentation;
import org.keycloak.social.SocialIdentityProvider;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import java.util.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoAdminResource {

    private static final Map<String, List<String>> ENUMS = createEnumsMap(EventType.class, OperationType.class);

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
        info.setSystemInfo(SystemInfoRepresentation.create(session));
        info.setMemoryInfo(MemoryInfoRepresentation.create());

        setSocialProviders(info);
        setIdentityProviders(info);
        setThemes(info);
        setClientImporters(info);
        setProviders(info);
        setProtocolMapperTypes(info);
        setBuiltinProtocolMappers(info);
        info.setEnums(ENUMS);
        return info;
    }

    private void setProviders(ServerInfoRepresentation info) {
        LinkedHashMap<String, SpiInfoRepresentation> spiReps = new LinkedHashMap<>();

        List<Spi> spis = new LinkedList<>();
        for (Spi spi : ServiceLoader.load(Spi.class)) {
            spis.add(spi);
        }
        Collections.sort(spis, new Comparator<Spi>() {
            @Override
            public int compare(Spi s1, Spi s2) {
                return s1.getName().compareTo(s2.getName());
            }
        });

        for (Spi spi : spis) {
            SpiInfoRepresentation spiRep = new SpiInfoRepresentation();
            spiRep.setInternal(spi.isInternal());
            spiRep.setSystemInfo(ServerInfoAwareProviderFactory.class.isAssignableFrom(spi.getProviderFactoryClass()));

            List<String> providerIds = new LinkedList<>(session.listProviderIds(spi.getProviderClass()));
            Collections.sort(providerIds);

            Map<String, ProviderRepresentation> providers = new HashMap<>();

            if (providerIds != null) {
                for (String name : providerIds) {
                    ProviderRepresentation provider = new ProviderRepresentation();
                    if (spiRep.isSystemInfo()) {
                        provider.setOperationalInfo(((ServerInfoAwareProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(spi.getProviderClass(), name)).getOperationalInfo());
                    }
                    providers.put(name, provider);
                }
            }
            spiRep.setProviders(providers);

            spiReps.put(spi.getName(), spiRep);
        }
        info.setProviders(spiReps);
    }

    private void setThemes(ServerInfoRepresentation info) {
        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        info.setThemes(new HashMap<String, List<String>>());

        for (Theme.Type type : Theme.Type.values()) {
            List<String> themes = new LinkedList<String>(themeProvider.nameSet(type));
            Collections.sort(themes);

            info.getThemes().put(type.toString().toLowerCase(), themes);
        }
    }

    private void setSocialProviders(ServerInfoRepresentation info) {
        info.setSocialProviders(new LinkedList<Map<String, String>>());
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.getSocialProviders(), "Social");
    }

    private void setIdentityProviders(ServerInfoRepresentation info) {
        info.setIdentityProviders(new LinkedList<Map<String, String>>());
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class);
        setIdentityProviders(providerFactories, info.getIdentityProviders(), "User-defined");

        providerFactories = session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.getIdentityProviders(), "Social");
    }

    public void setIdentityProviders(List<ProviderFactory> factories, List<Map<String, String>> providers, String groupName) {
        for (ProviderFactory providerFactory : factories) {
            IdentityProviderFactory factory = (IdentityProviderFactory) providerFactory;
            Map<String, String> data = new HashMap<>();
            data.put("groupName", groupName);
            data.put("name", factory.getName());
            data.put("id", factory.getId());

            providers.add(data);
        }
    }

    private void setProtocolMapperTypes(ServerInfoRepresentation info) {
        info.setProtocolMapperTypes(new HashMap<String, List<ProtocolMapperTypeRepresentation>>());
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(ProtocolMapper.class)) {
            ProtocolMapper mapper = (ProtocolMapper)p;
            List<ProtocolMapperTypeRepresentation> types = info.getProtocolMapperTypes().get(mapper.getProtocol());
            if (types == null) {
                types = new LinkedList<ProtocolMapperTypeRepresentation>();
                info.getProtocolMapperTypes().put(mapper.getProtocol(), types);
            }
            ProtocolMapperTypeRepresentation rep = new ProtocolMapperTypeRepresentation();
            rep.setId(mapper.getId());
            rep.setName(mapper.getDisplayType());
            rep.setHelpText(mapper.getHelpText());
            rep.setCategory(mapper.getDisplayCategory());
            rep.setProperties(new LinkedList<ConfigPropertyRepresentation>());
            List<ProviderConfigProperty> configProperties = mapper.getConfigProperties();
            for (ProviderConfigProperty prop : configProperties) {
                ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
                propRep.setName(prop.getName());
                propRep.setLabel(prop.getLabel());
                propRep.setType(prop.getType());
                propRep.setDefaultValue(prop.getDefaultValue());
                propRep.setHelpText(prop.getHelpText());
                rep.getProperties().add(propRep);
            }
            types.add(rep);
        }
    }

    private void setBuiltinProtocolMappers(ServerInfoRepresentation info) {
        info.setBuiltinProtocolMappers(new HashMap<String, List<ProtocolMapperRepresentation>>());
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(LoginProtocol.class)) {
            LoginProtocolFactory factory = (LoginProtocolFactory)p;
            List<ProtocolMapperRepresentation> mappers = new LinkedList<>();
            for (ProtocolMapperModel mapper : factory.getBuiltinMappers()) {
                mappers.add(ModelToRepresentation.toRepresentation(mapper));
            }
            info.getBuiltinProtocolMappers().put(p.getId(), mappers);
        }
    }

    private void setClientImporters(ServerInfoRepresentation info) {
        info.setClientImporters(new LinkedList<Map<String, String>>());
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(ClientImporter.class)) {
            ClientImporterFactory factory = (ClientImporterFactory)p;
            Map<String, String> data = new HashMap<String, String>();
            data.put("id", factory.getId());
            data.put("name", factory.getDisplayName());
            info.getClientImporters().add(data);
        }
    }

    private static Map<String, List<String>> createEnumsMap(Class... enums) {
        Map<String, List<String>> m = new HashMap<>();
        for (Class e : enums) {
            String n = e.getSimpleName();
            n = Character.toLowerCase(n.charAt(0)) + n.substring(1);

            List<String> l = new LinkedList<>();
            for (Object c :  e.getEnumConstants()) {
                l.add(c.toString());
            }
            Collections.sort(l);

            m.put(n, l);
        }
        return m;
    }

}
