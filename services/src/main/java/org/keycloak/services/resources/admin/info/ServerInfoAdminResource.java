/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.services.resources.admin.info;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PasswordPolicyProviderFactory;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.PasswordPolicyTypeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperTypeRepresentation;
import org.keycloak.representations.info.ClientInstallationRepresentation;
import org.keycloak.representations.info.MemoryInfoRepresentation;
import org.keycloak.representations.info.ProfileInfoRepresentation;
import org.keycloak.representations.info.ProviderRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.representations.info.SpiInfoRepresentation;
import org.keycloak.representations.info.SystemInfoRepresentation;
import org.keycloak.representations.info.ThemeInfoRepresentation;
import org.keycloak.theme.Theme;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoAdminResource {

    private static final Map<String, List<String>> ENUMS = createEnumsMap(EventType.class, OperationType.class, ResourceType.class);

    @Context
    private KeycloakSession session;

    /**
     * Get themes, social providers, auth providers, and event listeners available on this server
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ServerInfoRepresentation getInfo() {
        ServerInfoRepresentation info = new ServerInfoRepresentation();
        info.setSystemInfo(SystemInfoRepresentation.create(session.getKeycloakSessionFactory().getServerStartupTimestamp()));
        info.setMemoryInfo(MemoryInfoRepresentation.create());
        info.setProfileInfo(ProfileInfoRepresentation.create());

        setSocialProviders(info);
        setIdentityProviders(info);
        setThemes(info);
        setProviders(info);
        setProtocolMapperTypes(info);
        setBuiltinProtocolMappers(info);
        setClientInstallations(info);
        setPasswordPolicies(info);
        info.setEnums(ENUMS);
        return info;
    }

    private void setProviders(ServerInfoRepresentation info) {
        info.setComponentTypes(new HashMap<>());
        LinkedHashMap<String, SpiInfoRepresentation> spiReps = new LinkedHashMap<>();

        List<Spi> spis = new LinkedList<>(session.getKeycloakSessionFactory().getSpis());
        Collections.sort(spis, new Comparator<Spi>() {
            @Override
            public int compare(Spi s1, Spi s2) {
                return s1.getName().compareTo(s2.getName());
            }
        });

        for (Spi spi : spis) {
            SpiInfoRepresentation spiRep = new SpiInfoRepresentation();
            spiRep.setInternal(spi.isInternal());

            List<String> providerIds = new LinkedList<>(session.listProviderIds(spi.getProviderClass()));
            Collections.sort(providerIds);

            Map<String, ProviderRepresentation> providers = new HashMap<>();

            if (providerIds != null) {
                for (String name : providerIds) {
                    ProviderRepresentation provider = new ProviderRepresentation();
                    ProviderFactory<?> pi = session.getKeycloakSessionFactory().getProviderFactory(spi.getProviderClass(), name);
                    provider.setOrder(pi.order());
                    if (ServerInfoAwareProviderFactory.class.isAssignableFrom(pi.getClass())) {
                        provider.setOperationalInfo(((ServerInfoAwareProviderFactory) pi).getOperationalInfo());
                    }
                    if (pi instanceof ConfiguredProvider) {
                        ComponentTypeRepresentation rep = new ComponentTypeRepresentation();
                        rep.setId(pi.getId());
                        ConfiguredProvider configured = (ConfiguredProvider)pi;
                        rep.setHelpText(configured.getHelpText());
                        List<ProviderConfigProperty> configProperties = configured.getConfigProperties();
                        if (configProperties == null) configProperties = Collections.EMPTY_LIST;
                        rep.setProperties(ModelToRepresentation.toRepresentation(configProperties));
                        if (pi instanceof ComponentFactory) {
                            rep.setMetadata(((ComponentFactory)pi).getTypeMetadata());
                        }
                        List<ComponentTypeRepresentation> reps = info.getComponentTypes().get(spi.getProviderClass().getName());
                        if (reps == null) {
                            reps = new LinkedList<>();
                            info.getComponentTypes().put(spi.getProviderClass().getName(), reps);
                        }
                        reps.add(rep);
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
        info.setThemes(new HashMap<String, List<ThemeInfoRepresentation>>());

        for (Theme.Type type : Theme.Type.values()) {
            List<String> themeNames = new LinkedList<>(session.theme().nameSet(type));
            Collections.sort(themeNames);

            if (!Profile.isFeatureEnabled(Profile.Feature.ACCOUNT2)) {
                themeNames.remove("keycloak-preview");
                themeNames.remove("rh-sso-preview");
            }

            List<ThemeInfoRepresentation> themes = new LinkedList<>();
            info.getThemes().put(type.toString().toLowerCase(), themes);

            for (String name : themeNames) {
                try {
                    Theme theme = session.theme().getTheme(name, type);
                    ThemeInfoRepresentation ti = new ThemeInfoRepresentation();
                    ti.setName(name);

                    String locales = theme.getProperties().getProperty("locales");
                    if (locales != null) {
                        ti.setLocales(locales.replaceAll(" ", "").split(","));
                    }

                    themes.add(ti);
                } catch (IOException e) {
                    throw new WebApplicationException("Failed to load themes", e);
                }
            }
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

    private void setClientInstallations(ServerInfoRepresentation info) {
        info.setClientInstallations(new HashMap<String, List<ClientInstallationRepresentation>>());
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(ClientInstallationProvider.class)) {
            ClientInstallationProvider provider = (ClientInstallationProvider)p;
            List<ClientInstallationRepresentation> types = info.getClientInstallations().get(provider.getProtocol());
            if (types == null) {
                types = new LinkedList<>();
                info.getClientInstallations().put(provider.getProtocol(), types);
            }
            ClientInstallationRepresentation rep = new ClientInstallationRepresentation();
            rep.setId(p.getId());
            rep.setHelpText(provider.getHelpText());
            rep.setDisplayType( provider.getDisplayType());
            rep.setProtocol( provider.getProtocol());
            rep.setDownloadOnly( provider.isDownloadOnly());
            rep.setFilename(provider.getFilename());
            rep.setMediaType(provider.getMediaType());
            types.add(rep);
        }
    }

    private void setProtocolMapperTypes(ServerInfoRepresentation info) {
        info.setProtocolMapperTypes(new HashMap<String, List<ProtocolMapperTypeRepresentation>>());
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(ProtocolMapper.class)) {
            ProtocolMapper mapper = (ProtocolMapper)p;
            List<ProtocolMapperTypeRepresentation> types = info.getProtocolMapperTypes().get(mapper.getProtocol());
            if (types == null) {
                types = new LinkedList<>();
                info.getProtocolMapperTypes().put(mapper.getProtocol(), types);
            }
            ProtocolMapperTypeRepresentation rep = new ProtocolMapperTypeRepresentation();
            rep.setId(mapper.getId());
            rep.setName(mapper.getDisplayType());
            rep.setHelpText(mapper.getHelpText());
            rep.setCategory(mapper.getDisplayCategory());
            rep.setPriority(mapper.getPriority());
            rep.setProperties(new LinkedList<ConfigPropertyRepresentation>());
            List<ProviderConfigProperty> configProperties = mapper.getConfigProperties();
            rep.setProperties(ModelToRepresentation.toRepresentation(configProperties));
            types.add(rep);
        }
    }

    private void setBuiltinProtocolMappers(ServerInfoRepresentation info) {
        info.setBuiltinProtocolMappers(new HashMap<String, List<ProtocolMapperRepresentation>>());
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(LoginProtocol.class)) {
            LoginProtocolFactory factory = (LoginProtocolFactory)p;
            List<ProtocolMapperRepresentation> mappers = new LinkedList<>();
            for (ProtocolMapperModel mapper : factory.getBuiltinMappers().values()) {
                mappers.add(ModelToRepresentation.toRepresentation(mapper));
            }
            info.getBuiltinProtocolMappers().put(p.getId(), mappers);
        }
    }

    private void setPasswordPolicies(ServerInfoRepresentation info) {
        info.setPasswordPolicies(new LinkedList<>());
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(PasswordPolicyProvider.class)) {
            PasswordPolicyProviderFactory factory = (PasswordPolicyProviderFactory) f;
            PasswordPolicyTypeRepresentation rep = new PasswordPolicyTypeRepresentation();
            rep.setId(factory.getId());
            rep.setDisplayName(factory.getDisplayName());
            rep.setConfigType(factory.getConfigType());
            rep.setDefaultValue(factory.getDefaultConfigValue());
            rep.setMultipleSupported(factory.isMultiplSupported());
            info.getPasswordPolicies().add(rep);
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
