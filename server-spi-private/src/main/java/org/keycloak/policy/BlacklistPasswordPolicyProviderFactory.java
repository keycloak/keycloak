/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.policy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.jboss.logging.Logger;

/**
 * Creates {@link BlacklistPasswordPolicyProvider} instances.
 * <p>
 * Password blacklists are simple text files where every line is a blacklisted password delimited by a newline character {@code \n}.
 * <p>Blacklists can be configured via the <em>Authentication: Password Policy</em> section in the admin-console.
 * A blacklist-file is referred to by its name in the policy configuration.
 *
 * <h1>Blacklist location</h1>
 * <p>Users can provide custom blacklists by adding a blacklist password file to the configured blacklist folder.
 * <p>
 * <p>The location of the password-blacklists folder is derived as follows</p>
 * <ol>
 * <li>the value of the System property {@code keycloak.password.blacklists.path} if configured - fails if folder is missing</li>
 * <li>the value of the SPI config property: {@code blacklistsPath} when explicitly configured - fails if folder is missing</li>
 * <li>otherwise {@code $KC_HOME/data/password-blacklists/} if nothing else is configured</li>
 * </ol>
 *
 * To configure the blacklist folder via CLI use {@code --spi-password-policy-password-blacklist-blacklists-path=/path/to/blacklistsFolder}
 *
 * <p>Note that the preferred way for configuration is to copy the password file to the {@code $KC_HOME/data/password-blacklists/} folder</p>
 * <p>A password blacklist with the filename {@code 10_million_passwords.txt}
 * that is located beneath {@code $KC_HOME/data/keycloak/blacklists/} can be referred to as {@code 10_million_passwords.txt} in the <em>Authentication: Password Policy</em> configuration.
 *
 * <h1>False positives</h1>
 * <p>
 * The current implementation uses a probabilistic data-structure called {@link BloomFilter} which allows for fast and memory efficient containment checks, e.g. whether a given password is contained in a blacklist,
 * with the possibility for false positives. By default a false positive probability {@link #DEFAULT_FALSE_POSITIVE_PROBABILITY} is used.
 *
 * To change the false positive probability via CLI configuration use {@code --spi-password-policy-password-blacklist-false-positive-probability=0.00001}
 * </p>
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class BlacklistPasswordPolicyProviderFactory implements PasswordPolicyProviderFactory {

    private static final Logger LOG = Logger.getLogger(BlacklistPasswordPolicyProviderFactory.class);

    public static final String ID = "passwordBlacklist";

    public static final String SYSTEM_PROPERTY = "keycloak.password.blacklists.path";

    public static final String BLACKLISTS_PATH_PROPERTY = "blacklistsPath";

    public static final String BLACKLISTS_FALSE_POSITIVE_PROBABILITY_PROPERTY = "falsePositiveProbability";

    public static final double DEFAULT_FALSE_POSITIVE_PROBABILITY = 0.0001;

    public static final String JBOSS_SERVER_DATA_DIR = "jboss.server.data.dir";

    public static final String PASSWORD_BLACKLISTS_FOLDER = "password-blacklists" + File.separator;

    private final ConcurrentMap<String, FileBasedPasswordBlacklist> blacklistRegistry = new ConcurrentHashMap<>();

    private volatile Path blacklistsBasePath;

    private Config.Scope config;

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        if (this.blacklistsBasePath == null) {
            synchronized (this) {
                if (this.blacklistsBasePath == null) {
                    this.blacklistsBasePath = FileBasedPasswordBlacklist.detectBlacklistsBasePath(config, this::getDefaultBlacklistsBasePath);
                }
            }
        }
        return new BlacklistPasswordPolicyProvider(session.getContext(), this);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getDisplayName() {
        return "Password Blacklist";
    }

    @Override
    public String getConfigType() {
        return PasswordPolicyProvider.STRING_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return "";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public String getId() {
        return ID;
    }

    /**
     * Method to obtain the default location for the list folder. The method
     * will return the <em>data</em> directory of the Keycloak instance concatenated
     * with <em>/password-blacklists/</em>.
     *
     * @return The default path used by the provider to lookup the lists
     * when no other configuration is in place.
     */
    public String getDefaultBlacklistsBasePath() {
        return System.getProperty(JBOSS_SERVER_DATA_DIR) + File.separator + PASSWORD_BLACKLISTS_FOLDER;
    }

    /**
     * Resolves and potentially registers a {@link PasswordBlacklist} for the given {@code blacklistName}.
     *
     * @param blacklistName
     * @return
     */
    public PasswordBlacklist resolvePasswordBlacklist(String blacklistName) {

        Objects.requireNonNull(blacklistName, "blacklistName");

        String listName = blacklistName.trim();
        if (listName.isEmpty()) {
            throw new IllegalArgumentException("Password blacklist name must not be empty!");
        }

        return blacklistRegistry.computeIfAbsent(listName, (name) -> {
            double fpp = getFalsePositiveProbability();
            FileBasedPasswordBlacklist pbl = new FileBasedPasswordBlacklist(this.blacklistsBasePath, name, fpp);
            pbl.lazyInit();
            return pbl;
        });
    }

    protected double getFalsePositiveProbability() {

        if (config == null) {
            return DEFAULT_FALSE_POSITIVE_PROBABILITY;
        }

        String falsePositiveProbString = config.get(BLACKLISTS_FALSE_POSITIVE_PROBABILITY_PROPERTY);
        if (falsePositiveProbString == null) {
            return DEFAULT_FALSE_POSITIVE_PROBABILITY;
        }

        try {
            return Double.parseDouble(falsePositiveProbString);
        } catch (NumberFormatException nfe) {
            LOG.warnf("Could not parse false positive probability from string %s", falsePositiveProbString);
            return DEFAULT_FALSE_POSITIVE_PROBABILITY;
        }
    }

    /**
     * A {@link PasswordBlacklist} describes a list of too easy to guess
     * or potentially leaked passwords that users should not be able to use.
     */
    public interface PasswordBlacklist {


        /**
         * @return the logical name of the {@link PasswordBlacklist}
         */
        String getName();

        /**
         * Checks whether a given {@code password} is contained in this {@link PasswordBlacklist}.
         *
         * @param password
         * @return
         */
        boolean contains(String password);
    }

    /**
     * A {@link FileBasedPasswordBlacklist} uses password-blacklist files as
     * to construct a {@link PasswordBlacklist}.
     * <p>
     * This implementation uses a dynamically sized {@link BloomFilter}
     * with a provided default false positive probability.
     *
     * @see BloomFilter
     */
    public static class FileBasedPasswordBlacklist implements PasswordBlacklist {

        private static final int BUFFER_SIZE_IN_BYTES = 512 * 1024;

        /**
         * The name of the blacklist filename.
         */
        private final String name;

        /**
         * The concrete path to the password-blacklist file.
         */
        private final Path path;

        private final double falsePositiveProbability;

        /**
         * Initialized lazily via {@link #lazyInit()}
         */
        private BloomFilter<String> blacklist;

        /**
         * Creates a new {@link FileBasedPasswordBlacklist} with {@link #DEFAULT_FALSE_POSITIVE_PROBABILITY}.
         *
         * @param blacklistBasePath folder containing the blacklists
         * @param name name of blacklist file
         */
        public FileBasedPasswordBlacklist(Path blacklistBasePath, String name) {
            this(blacklistBasePath, name, DEFAULT_FALSE_POSITIVE_PROBABILITY);
        }

        public FileBasedPasswordBlacklist(Path blacklistBasePath, String name, double falsePositiveProbability) {

            if (name.contains("/")) {
                // disallow '/' to avoid accidental filesystem traversal
                throw new IllegalArgumentException("" + name + " must not contain slashes!");
            }

            this.name = name;
            this.path = blacklistBasePath.resolve(name);
            this.falsePositiveProbability = falsePositiveProbability;

            if (!Files.exists(this.path)) {
                throw new IllegalArgumentException("Password blacklist " + name + " not found!");
            }
        }

        public String getName() {
            return name;
        }

        public double getFalsePositiveProbability() {
            return falsePositiveProbability;
        }

        public boolean contains(String password) {
            return blacklist != null && blacklist.mightContain(password);
        }

        void lazyInit() {

            if (blacklist != null) {
                return;
            }

            this.blacklist = load();
        }

        /**
         * Loads the referenced blacklist into a {@link BloomFilter}.
         *
         * @return the {@link BloomFilter} backing a password blacklist
         */
        private BloomFilter<String> load() {

            try {
                LOG.infof("Loading blacklist start: name=%s path=%s", name, path);

                long passwordCount = countPasswordsInBlacklistFile();
                double fpp = getFalsePositiveProbability();

                BloomFilter<String> filter = BloomFilter.create(
                        Funnels.stringFunnel(StandardCharsets.UTF_8),
                        passwordCount,
                        fpp);

                insertPasswordsInto(filter);

                double expectedFfp = filter.expectedFpp();
                LOG.infof("Loading blacklist finished: name=%s passwords=%s path=%s falsePositiveProbability=%s expectedFalsePositiveProbability=%s",
                        name, passwordCount, path, fpp, expectedFfp);

                return filter;
            } catch (IOException e) {
                throw new RuntimeException("Loading blacklist failed: Could not load password blacklist path=" + path, e);
            }
        }

        protected void insertPasswordsInto(BloomFilter<String> filter) throws IOException {
            try (BufferedReader br = newReader(path)) {
                br.lines().map(String::toLowerCase).forEach(filter::put);
            }
        }

        /**
         * Determines password blacklist size to correctly size the {@link BloomFilter} backing this blacklist.
         *
         * @return number of passwords found in the blacklist file
         * @throws IOException
         */
        private long countPasswordsInBlacklistFile() throws IOException {

            /*
             * TODO find a more efficient way to determine the password count,
             * e.g. require a header-line in the password-blacklist file
             */
            try (BufferedReader br = newReader(path)) {
                return br.lines().count();
            }
        }

        private static BufferedReader newReader(Path path) throws IOException {
            return new BufferedReader(Files.newBufferedReader(path), BUFFER_SIZE_IN_BYTES);
        }

        /**
         * Discovers password blacklists location.
         * <p>
         * The following discovery options are currently implemented:
         * <p>
         * <ol>
         *   <li>system property {@code keycloak.password.blacklists.path} if present</li>
         *   <li>SPI config property {@code blacklistsPath}</li>
         *   <li>fallback to the {@code /data/password-blacklists} folder of the currently running Keycloak instance</li>
         * </ol>
         *
         * @param config spi config
         * @param defaultPathSupplier default path to use if not specified in a system prop or configuration
         * @return the detected blacklist path
         * @throws IllegalStateException if no blacklist folder could be detected
         */
        private static Path detectBlacklistsBasePath(Config.Scope config, Supplier<String> defaultPathSupplier) {

            String pathFromSysProperty = System.getProperty(SYSTEM_PROPERTY);
            if (pathFromSysProperty != null) {
                return ensureExists(Paths.get(pathFromSysProperty));
            }

            String pathFromSpiConfig = config.get(BLACKLISTS_PATH_PROPERTY);
            if (pathFromSpiConfig != null) {
                return ensureExists(Paths.get(pathFromSpiConfig));
            }

            String pathFromJbossDataPath = defaultPathSupplier.get();
            if (pathFromJbossDataPath == null) {
                throw new IllegalStateException("Default path for the blacklist folder was null");
            }

            if (!Files.exists(Paths.get(pathFromJbossDataPath))) {
                if (!Paths.get(pathFromJbossDataPath).toFile().mkdirs()) {
                    LOG.errorf("Could not create folder for password blacklists: %s", pathFromJbossDataPath);
                }
            }
            return ensureExists(Paths.get(pathFromJbossDataPath));
        }

        private static Path ensureExists(Path path) {

            Objects.requireNonNull(path, "path");

            if (Files.exists(path)) {
                return path;
            }

            throw new IllegalStateException("Password blacklists location does not exist: " + path);
        }
    }
}
