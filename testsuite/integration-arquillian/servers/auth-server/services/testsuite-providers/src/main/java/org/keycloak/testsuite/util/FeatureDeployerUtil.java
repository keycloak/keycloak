/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.provider.DefaultProviderLoader;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.ProviderManagerRegistry;
import org.keycloak.provider.Spi;
import org.keycloak.services.DefaultKeycloakSession;

import org.jboss.logging.Logger;

/**
 * Used to dynamically reload EnvironmentDependentProviderFactories after some feature is enabled/disabled
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FeatureDeployerUtil {

    private final static Map<Profile.Feature, Map<ProviderFactory, Spi>> initializer = new ConcurrentHashMap<>();

    private final static Map<Profile.Feature, ProviderManager> deployersCache = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(FeatureDeployerUtil.class);

    public static void initBeforeChangeFeature(Profile.Feature feature) {
        if (deployersCache.containsKey(feature)) {
            return;
        }

        // Compute which provider factories are enabled before feature is enabled (disabled)
        Map<ProviderFactory, Spi>  factoriesBefore = loadEnabledEnvironmentFactories();
        initializer.put(feature, factoriesBefore);
    }

    public static void deployFactoriesAfterFeatureEnabled(Profile.Feature feature) {
        ProviderManager manager = deployersCache.get(feature);
        if (manager == null) {
            // Need to figure which provider factories were enabled after feature was enabled. Create deployer based on it and save it to the cache
            Map<ProviderFactory, Spi> factoriesBeforeEnable = initializer.remove(feature);
            Map<ProviderFactory, Spi> factoriesAfterEnable = loadEnabledEnvironmentFactories();
            Map<ProviderFactory, Spi>  factories = getFactoriesDependentOnFeature(factoriesBeforeEnable, factoriesAfterEnable);

            logger.infof("New factories when enabling feature '%s': %s", feature, factories.keySet());

            KeycloakDeploymentInfo di = createDeploymentInfo(factories);

            manager = new ProviderManager(di, FeatureDeployerUtil.class.getClassLoader(), Collections.singleton(new TestsuiteProviderLoader(di)));
            deployersCache.put(feature, manager);
        }
        ProviderManagerRegistry.SINGLETON.deploy(manager);
    }

    public static void undeployFactoriesAfterFeatureDisabled(Profile.Feature feature) {
        ProviderManager manager = deployersCache.get(feature);
        if (manager == null) {
            // This is used if some feature is enabled by default and then disabled
            // Need to figure which provider factories were enabled after feature was enabled. Create deployer based on it and save it to the cache
            Map<ProviderFactory, Spi> factoriesBeforeDisable = initializer.remove(feature);
            Map<ProviderFactory, Spi> factoriesAfterDisable = loadEnabledEnvironmentFactories();
            Map<ProviderFactory, Spi>  factories = getFactoriesDependentOnFeature(factoriesAfterDisable, factoriesBeforeDisable);

            KeycloakDeploymentInfo di = createDeploymentInfo(factories);

            manager = new ProviderManager(di, FeatureDeployerUtil.class.getClassLoader());
            loadFactories(manager);
            deployersCache.put(feature, manager);
        }
        ProviderManagerRegistry.SINGLETON.undeploy(manager);
    }

    private static Map<ProviderFactory, Spi> getFactoriesDependentOnFeature(Map<ProviderFactory, Spi> factoriesDisabled, Map<ProviderFactory, Spi> factoriesEnabled) {
        Set<Class<? extends ProviderFactory>> disabledFactoriesClasses = factoriesDisabled.keySet().stream()
                .map(ProviderFactory::getClass)
                .collect(Collectors.toSet());

        Set<Class<? extends ProviderFactory>> enabledFactoriesClasses = factoriesEnabled.keySet().stream()
                .map(ProviderFactory::getClass)
                .collect(Collectors.toSet());

        enabledFactoriesClasses.removeAll(disabledFactoriesClasses);

        Map<ProviderFactory, Spi> newFactories = factoriesEnabled.entrySet().stream()
                .filter(entry -> enabledFactoriesClasses.contains(entry.getKey().getClass()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return newFactories;
    }

    private static KeycloakDeploymentInfo createDeploymentInfo(Map<ProviderFactory, Spi> factories) {
        KeycloakDeploymentInfo di = KeycloakDeploymentInfo.create();
        for (Map.Entry<ProviderFactory, Spi> factory : factories.entrySet()) {
            ProviderFactory pf = factory.getKey();
            Class<? extends Spi> spiClass = factory.getValue().getClass();
            di.addProvider(spiClass, pf);
        }
        return di;
    }

    private static Map<ProviderFactory, Spi> loadEnabledEnvironmentFactories() {
        KeycloakDeploymentInfo di = KeycloakDeploymentInfo.create().services();
        ClassLoader classLoader = DefaultKeycloakSession.class.getClassLoader();
        DefaultProviderLoader loader = new DefaultProviderLoader(di, classLoader);

        Map<ProviderFactory, Spi> providerFactories = new HashMap<>();
        for (Spi spi : loader.loadSpis()) {
            Config.Scope scope = Config.scope(spi.getName(), Config.getProvider(spi.getName()));
            List<ProviderFactory> currentFactories = loader.load(spi);
            for (ProviderFactory factory : currentFactories) {
                if (factory instanceof EnvironmentDependentProviderFactory) {
                    if (((EnvironmentDependentProviderFactory) factory).isSupported(scope)) {
                        providerFactories.put(factory, spi);
                    }
                }

            }
        }

        return providerFactories;
    }

    private static void loadFactories(ProviderManager pm) {
        KeycloakDeploymentInfo di = KeycloakDeploymentInfo.create().services();
        ClassLoader classLoader = DefaultKeycloakSession.class.getClassLoader();
        DefaultProviderLoader loader = new DefaultProviderLoader(di, classLoader);
        loader.loadSpis().forEach(pm::load);
    }
}
