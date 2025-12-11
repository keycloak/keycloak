package org.keycloak.testsuite.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.arquillian.containers.AbstractQuarkusDeployableContainer;
import org.keycloak.utils.StringUtil;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.logging.Logger;

public class SpiProvidersSwitchingUtils {

    private static final String KEYCLOAKX_ARG_SPI_PREFIX = "--spi-";
    private static final Map<String, String> originalSettingsBackup = new ConcurrentHashMap<>();
    protected static final Logger log = Logger.getLogger(SpiProvidersSwitchingUtils.class);

    private enum SpiSwitcher {
        UNDERTOW {
            @Override
            public Optional<String> getCurrentDefaultProvider(Container container, String spiName,
                    SetDefaultProvider annotation) {
                return Optional.ofNullable(System.getProperty(getProviderPropertyName(spiName)));
            }

            @Override
            public void setDefaultProvider(Container container, String spiName, String providerId, String... config) {
                System.setProperty(getProviderPropertyName(spiName), providerId);
                if (config != null) {
                    String optionName = null;
                    for (String c : config) {
                        if (optionName == null) {
                            optionName = c;
                        } else {
                            System.setProperty(getProviderPropertyNameConfig(spiName, providerId, optionName), c);
                            optionName = null;
                        }
                    }
                }
            }

            @Override
            public void removeProviderConfig(Container container, String spiName) {
                List<String> toRemove = System.getProperties().stringPropertyNames().stream()
                        .filter(p -> p.startsWith(getProviderPropertyNamePrefix(spiName)))
                        .collect(Collectors.toList());
                toRemove.forEach(p -> System.clearProperty(p));
            }

            private String getProviderPropertyNamePrefix(String spiName) {
                return  "keycloak." + spiName + ".";
            }

            private String getProviderPropertyName(String spiName) {
                return getProviderPropertyNamePrefix(spiName) + "provider";
            }

            private String getProviderPropertyNameConfig(String spiName, String providerId, String configName) {
                return getProviderPropertyNamePrefix(spiName) + providerId + "." + configName;
            }
        },
        QUARKUS {
            @Override
            public void setDefaultProvider(Container container, String spiName, String providerId, String... config) {
                List<String> args = new LinkedList<>();
                args.add(KEYCLOAKX_ARG_SPI_PREFIX + toDashCase(spiName) + "--provider=" + providerId);
                if (config != null) {
                    String optionName = null;
                    for (String c : config) {
                        if (optionName == null) {
                            optionName = c;
                        } else {
                            args.add(KEYCLOAKX_ARG_SPI_PREFIX + toDashCase(spiName) + "--" + providerId + "--" + optionName + "=" + c);
                            optionName = null;
                        }
                    }
                }
                getQuarkusContainer(container).setSpiConfig(spiName, args);
            }

            @Override
            public void removeProviderConfig(Container container, String spiName) {
                getQuarkusContainer(container).removeSpiConfig(spiName);
            }

            private AbstractQuarkusDeployableContainer getQuarkusContainer(Container container) {
                return (AbstractQuarkusDeployableContainer) container.getDeployableContainer();
            }

            /**
             * Parses the non-standard SPI-Name format to the standardized format
             * we use in the Keycloak.X Configuration
             *
             * @param s possibly non-standard spi name
             * @return standardized spi name in dash-case. e.g. userProfile -> user-profile
             */
            private String toDashCase(String s) {
                StringBuilder sb = new StringBuilder(s.length());
                boolean l = false;

                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (l && Character.isUpperCase(c)) {
                        sb.append('-');
                        c = Character.toLowerCase(c);
                        l = false;
                    } else {
                        l = Character.isLowerCase(c);
                    }
                    sb.append(c);
                }
                return sb.toString();
            }
        };

        public Optional<String> getCurrentDefaultProvider(Container container, String spiName,
                SetDefaultProvider annotation) {
            String defaultProvider = annotation.defaultProvider();
            if (StringUtil.isNotBlank(defaultProvider)) {
                return Optional.of(defaultProvider);
            }
            return Optional.empty();
        }

        public abstract void setDefaultProvider(Container container, String spiName, String providerId, String... config);

        public void updateDefaultProvider(Container container, String spiName, String providerId, String... config) {
            setDefaultProvider(container, spiName, providerId, config);
        }

        public void unsetDefaultProvider(Container container, String spiName) {
            removeProviderConfig(container, spiName);
        }

        public abstract void removeProviderConfig(Container container, String spiName);

        public static SpiSwitcher getSpiSwitcherFor(ContainerInfo containerInfo) {
            if (containerInfo.isUndertow()) {
                return SpiSwitcher.UNDERTOW;
            }
            return SpiSwitcher.QUARKUS;
        }
    }

    private SpiProvidersSwitchingUtils() {
    }

    public static void addProviderDefaultValue(SuiteContext suiteContext, SetDefaultProvider annotation) {
        ContainerInfo authServerInfo = suiteContext.getAuthServerInfo();
        SpiSwitcher spiSwitcher = SpiSwitcher.getSpiSwitcherFor(authServerInfo);
        String spi = annotation.spi();
        Container container = authServerInfo.getArquillianContainer();

        log.infof("Setting default provider for %s to %s", spi, annotation.providerId());

        if (annotation.onlyUpdateDefault()) {
            spiSwitcher.getCurrentDefaultProvider(container, spi, annotation).ifPresent(v -> originalSettingsBackup.put(spi, v));
            spiSwitcher.updateDefaultProvider(container, spi, annotation.providerId(), annotation.config());
        } else {
            spiSwitcher.setDefaultProvider(container, spi, annotation.providerId(), annotation.config());
        }
    }

    public static void resetProvider(SuiteContext suiteContext, SetDefaultProvider annotation) {
        ContainerInfo authServerInfo = suiteContext.getAuthServerInfo();
        SpiSwitcher spiSwitcher = SpiSwitcher.getSpiSwitcherFor(authServerInfo);
        String spi = annotation.spi();
        Container container = authServerInfo.getArquillianContainer();

        if (annotation.onlyUpdateDefault()) {
            String originalValue = originalSettingsBackup.get(spi);

            log.infof("Resetting default provider for %s to %s", spi,
                    originalValue == null ? "<null>" : originalValue);

            if (originalValue != null) {
                spiSwitcher.updateDefaultProvider(container, spi, originalValue);
            } else {
                spiSwitcher.unsetDefaultProvider(container, spi);
            }
        } else {
            log.infof("Removing default provider for %s to %s", spi);
            spiSwitcher.removeProviderConfig(container, spi);
        }
    }

    public static void removeProvider(SuiteContext suiteContext, SetDefaultProvider annotation) {
        ContainerInfo authServerInfo = suiteContext.getAuthServerInfo();
        SpiSwitcher spiSwitcher = SpiSwitcher.getSpiSwitcherFor(authServerInfo);
        String spi = annotation.spi();
        Container container = authServerInfo.getArquillianContainer();

        log.infof("Removing default provider setting for %s", spi);

        if (annotation.onlyUpdateDefault()) {
            spiSwitcher.unsetDefaultProvider(container, spi);
        } else {
            spiSwitcher.removeProviderConfig(container, spi);
        }
    }
}
