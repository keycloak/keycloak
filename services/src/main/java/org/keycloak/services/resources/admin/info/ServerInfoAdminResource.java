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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentFactory;
import org.keycloak.crypto.ClientSignatureVerifierProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
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
import org.keycloak.representations.idm.PasswordPolicyTypeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperTypeRepresentation;
import org.keycloak.representations.info.ClientInstallationRepresentation;
import org.keycloak.representations.info.CryptoInfoRepresentation;
import org.keycloak.representations.info.FeatureRepresentation;
import org.keycloak.representations.info.MemoryInfoRepresentation;
import org.keycloak.representations.info.ProfileInfoRepresentation;
import org.keycloak.representations.info.ProviderRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.representations.info.SpiInfoRepresentation;
import org.keycloak.representations.info.SystemInfoRepresentation;
import org.keycloak.representations.info.ThemeInfoRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.theme.Theme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN , value = "")
public class ServerInfoAdminResource {

    private static final Map<String, List<String>> ENUMS = createEnumsMap(EventType.class, OperationType.class, ResourceType.class);

    private final KeycloakSession session;

    public ServerInfoAdminResource(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Get themes, social providers, auth providers, and event listeners available on this server
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ROOT)
    @Operation( summary = "Get themes, social providers, auth providers, and event listeners available on this server")
    public ServerInfoRepresentation getInfo() {
        ServerInfoRepresentation info = new ServerInfoRepresentation();
        info.setSystemInfo(SystemInfoRepresentation.create(session.getKeycloakSessionFactory().getServerStartupTimestamp()));
        info.setMemoryInfo(MemoryInfoRepresentation.create());
        info.setProfileInfo(ProfileInfoRepresentation.create());
        info.setFeatures(FeatureRepresentation.create());

        // True - asymmetric algorithms, false - symmetric algorithms
        Map<Boolean, List<String>> algorithms = session.getAllProviders(ClientSignatureVerifierProvider.class).stream()
                        .collect(
                                Collectors.toMap(
                                        ClientSignatureVerifierProvider::isAsymmetricAlgorithm,
                                        clientSignatureVerifier -> Collections.singletonList(clientSignatureVerifier.getAlgorithm()),
                                        (l1, l2) -> listCombiner(l1, l2)
                                                .stream()
                                                .sorted()
                                                .collect(Collectors.toList()),
                                        HashMap::new
                                )
                        );
        info.setCryptoInfo(CryptoInfoRepresentation.create(algorithms.get(false), algorithms.get(true)));

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
        info.setThemes(new HashMap<>());

        for (Theme.Type type : Theme.Type.values()) {
            List<String> themeNames = filterThemes(type, new LinkedList<>(session.theme().nameSet(type)));
            Collections.sort(themeNames);

            List<ThemeInfoRepresentation> themes = new LinkedList<>();
            info.getThemes().put(type.toString().toLowerCase(), themes);

            for (String name : themeNames) {
                try {
                    Theme theme = session.theme().getTheme(name, type);
                    // Different name means the theme itself was not found and fallback to default theme was needed
                    if (theme != null && name.equals(theme.getName())) {
                        ThemeInfoRepresentation ti = new ThemeInfoRepresentation();
                        ti.setName(name);

                        String locales = theme.getProperties().getProperty("locales");
                        if (locales != null) {
                            ti.setLocales(locales.replaceAll(" ", "").split(","));
                        }

                        themes.add(ti);
                    }
                } catch (IOException e) {
                    throw new WebApplicationException("Failed to load themes", e);
                }
            }
        }
    }
    
    private LinkedList<String> filterThemes(Theme.Type type, LinkedList<String> themeNames) {
        LinkedList<String> filteredNames = new LinkedList<>(themeNames);
        
        boolean filterAccountV2 = (type == Theme.Type.ACCOUNT) && 
                !Profile.isFeatureEnabled(Profile.Feature.ACCOUNT2);
        boolean filterAdminV2 = (type == Theme.Type.ADMIN) && 
                !Profile.isFeatureEnabled(Profile.Feature.ADMIN2);
        
        if (filterAccountV2 || filterAdminV2) {
            filteredNames.remove("keycloak.v2");
            filteredNames.remove("rh-sso.v2");
        }

        boolean filterAccountV3 = (type == Theme.Type.ACCOUNT) && 
            !Profile.isFeatureEnabled(Profile.Feature.ACCOUNT3);

        if (filterAccountV3) {
            filteredNames.remove("keycloak.v3");
        }
        
        return filteredNames;
    }

    private void setSocialProviders(ServerInfoRepresentation info) {
        info.setSocialProviders(new LinkedList<>());
        Stream<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.getSocialProviders(), "Social");
    }

    private void setIdentityProviders(ServerInfoRepresentation info) {
        info.setIdentityProviders(new LinkedList<>());
        Stream<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class);
        setIdentityProviders(providerFactories, info.getIdentityProviders(), "User-defined");

        providerFactories = session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.getIdentityProviders(), "Social");
    }

    public void setIdentityProviders(Stream<ProviderFactory> factories, List<Map<String, String>> providers, String groupName) {
        List<Map<String, String>> providerMaps = factories
                .map(IdentityProviderFactory.class::cast)
                .map(factory -> {
                    Map<String, String> data = new HashMap<>();
                    data.put("groupName", groupName);
                    data.put("name", factory.getName());
                    data.put("id", factory.getId());
                    return data;
                })
                .collect(Collectors.toList());

        providers.addAll(providerMaps);
    }

    private void setClientInstallations(ServerInfoRepresentation info) {
        HashMap<String, List<ClientInstallationRepresentation>> clientInstallations = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(ClientInstallationProvider.class)
                .map(ClientInstallationProvider.class::cast)
                .collect(
                        Collectors.toMap(
                                ClientInstallationProvider::getProtocol,
                                this::toClientInstallationRepresentation,
                                (l1, l2) -> listCombiner(l1, l2),
                                HashMap::new
                        )
                );
        info.setClientInstallations(clientInstallations);

    }

    private void setProtocolMapperTypes(ServerInfoRepresentation info) {
        HashMap<String, List<ProtocolMapperTypeRepresentation>> protocolMappers = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(ProtocolMapper.class)
                .map(ProtocolMapper.class::cast)
                .collect(
                        Collectors.toMap(
                                ProtocolMapper::getProtocol,
                                this::toProtocolMapperTypeRepresentation,
                                (l1, l2) -> listCombiner(l1, l2),
                                HashMap::new
                        )
                );
        info.setProtocolMapperTypes(protocolMappers);
    }

    private void setBuiltinProtocolMappers(ServerInfoRepresentation info) {
        Map<String, List<ProtocolMapperRepresentation>> protocolMappers = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(LoginProtocol.class)
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> {
                            LoginProtocolFactory factory = (LoginProtocolFactory) p;
                            return factory.getBuiltinMappers().values().stream()
                                    .map(ModelToRepresentation::toRepresentation)
                                    .collect(Collectors.toList());
                        })
                );
        info.setBuiltinProtocolMappers(protocolMappers);
    }

    private void setPasswordPolicies(ServerInfoRepresentation info) {
        List<PasswordPolicyTypeRepresentation> passwordPolicyTypes= session.getKeycloakSessionFactory().getProviderFactoriesStream(PasswordPolicyProvider.class)
                .map(PasswordPolicyProviderFactory.class::cast)
                .map(factory -> {
                    PasswordPolicyTypeRepresentation rep = new PasswordPolicyTypeRepresentation();
                    rep.setId(factory.getId());
                    rep.setDisplayName(factory.getDisplayName());
                    rep.setConfigType(factory.getConfigType());
                    rep.setDefaultValue(factory.getDefaultConfigValue());
                    rep.setMultipleSupported(factory.isMultiplSupported());
                    return rep;
                })
                .collect(Collectors.toList());
        info.setPasswordPolicies(passwordPolicyTypes);
    }

    private List<ClientInstallationRepresentation> toClientInstallationRepresentation(ClientInstallationProvider provider) {
        ClientInstallationRepresentation rep = new ClientInstallationRepresentation();
        rep.setId(provider.getId());
        rep.setHelpText(provider.getHelpText());
        rep.setDisplayType( provider.getDisplayType());
        rep.setProtocol( provider.getProtocol());
        rep.setDownloadOnly( provider.isDownloadOnly());
        rep.setFilename(provider.getFilename());
        rep.setMediaType(provider.getMediaType());
        return Arrays.asList(rep);
    }

    private List<ProtocolMapperTypeRepresentation> toProtocolMapperTypeRepresentation(ProtocolMapper mapper) {
        ProtocolMapperTypeRepresentation rep = new ProtocolMapperTypeRepresentation();
        rep.setId(mapper.getId());
        rep.setName(mapper.getDisplayType());
        rep.setHelpText(mapper.getHelpText());
        rep.setCategory(mapper.getDisplayCategory());
        rep.setPriority(mapper.getPriority());
        List<ProviderConfigProperty> configProperties = mapper.getConfigProperties();
        rep.setProperties(ModelToRepresentation.toRepresentation(configProperties));
        return Arrays.asList(rep);
    }

    private static <T> List<T> listCombiner(List<T> list1, List<T> list2) {
        return Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toList());
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
