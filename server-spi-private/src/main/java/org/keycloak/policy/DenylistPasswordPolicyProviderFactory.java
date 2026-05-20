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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.jboss.logging.Logger;

/**
 * Creates {@link DenylistPasswordPolicyProvider} instances.
 * <p>
 * Password denylists are simple text files where every line is a denylisted password delimited by a newline character {@code \n}.
 * <p>Denylists can be configured via the <em>Authentication: Password Policy</em> section in the admin-console.
 * A denylist-file is referred to by its name in the policy configuration.
 *
 * <h1>Denylist location</h1>
 * <p>Users can provide custom denylists by adding a denylist password file to the configured denylist folder.
 * <p>
 * <p>The location of the password-blacklists folder is derived as follows</p>
 * <ol>
 * <li>the value of the System property {@code keycloak.password.blacklists.path} if configured - fails if folder is missing</li>
 * <li>the value of the SPI config property: {@code blacklistsPath} when explicitly configured - fails if folder is missing</li>
 * <li>otherwise {@code $KC_HOME/data/password-blacklists/} if nothing else is configured</li>
 * </ol>
 *
 * To configure the denylist folder via CLI use {@code --spi-password-policy-password-blacklist-blacklists-path=/path/to/denylistsFolder}
 *
 * <p>Note that the preferred way for configuration is to copy the password file to the {@code $KC_HOME/data/password-blacklists/} folder</p>
 * <p>A password denylist with the filename {@code 10_million_passwords.txt}
 * that is located beneath {@code $KC_HOME/data/keycloak/blacklists/} can be referred to as {@code 10_million_passwords.txt} in the <em>Authentication: Password Policy</em> configuration.
 *
 * <h1>False positives</h1>
 * <p>
 * The current implementation uses a probabilistic data-structure called {@link BloomFilter} which allows for fast and memory efficient containment checks, e.g. whether a given password is contained in a denylist,
 * with the possibility for false positives. By default a false positive probability {@link #DEFAULT_FALSE_POSITIVE_PROBABILITY} is used.
 *
 * To change the false positive probability via CLI configuration use {@code --spi-password-policy-password-blacklist-false-positive-probability=0.00001}
 * </p>
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class DenylistPasswordPolicyProviderFactory implements PasswordPolicyProviderFactory {

    private static final Logger LOG = Logger.getLogger(DenylistPasswordPolicyProviderFactory.class);

    public static final String ID = "passwordBlacklist";

    public static final String SYSTEM_PROPERTY = "keycloak.password.blacklists.path";

    public static final String BLACKLISTS_PATH_PROPERTY = "blacklistsPath";

    public static final String BLACKLISTS_FALSE_POSITIVE_PROBABILITY_PROPERTY = "falsePositiveProbability";

    public static final String CHECK_INTERVAL_SECONDS_PROPERTY = "checkIntervalSeconds";

    public static final double DEFAULT_FALSE_POSITIVE_PROBABILITY = 0.0001;

    public static final int DEFAULT_CHECK_INTERVAL_SECONDS = 60;

    public static final String JBOSS_SERVER_DATA_DIR = "jboss.server.data.dir";

    public static final String PASSWORD_BLACKLISTS_FOLDER = "password-blacklists" + File.separator;

    private final ConcurrentMap<String, FileBasedPasswordDenylist> denylistRegistry = new ConcurrentHashMap<>();

    private volatile Path denylistsBasePath;

    private Config.Scope config;

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        if (this.denylistsBasePath == null) {
            synchronized (this) {
                if (this.denylistsBasePath == null) {
                    this.denylistsBasePath = FileBasedPasswordDenylist.detectDenylistsBasePath(config, this::getDefaultDenylistsBasePath);
                }
            }
        }
        return new DenylistPasswordPolicyProvider(session.getContext(), this);
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
    public String getDefaultDenylistsBasePath() {
        return System.getProperty(JBOSS_SERVER_DATA_DIR) + File.separator + PASSWORD_BLACKLISTS_FOLDER;
    }

    /**
     * Resolves and potentially registers a {@link PasswordDenylist} for the given {@code denylistName}.
     *
     * @param denylistName
     * @return
     */
    public PasswordDenylist resolvePasswordDenylist(String denylistName) {

        Objects.requireNonNull(denylistName, "denylistName");

        String listName = denylistName.trim();
        if (listName.isEmpty()) {
            throw new IllegalArgumentException("Password denylist name must not be empty!");
        }

        return denylistRegistry.computeIfAbsent(listName, (name) -> {
            double fpp = getFalsePositiveProbability();
            return new FileBasedPasswordDenylist(this.denylistsBasePath, name, fpp, getCheckIntervalSeconds() * 1000L);
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

    protected int getCheckIntervalSeconds() {

        if (config == null) {
            return DEFAULT_CHECK_INTERVAL_SECONDS;
        }

        String checkIntervalString = config.get(CHECK_INTERVAL_SECONDS_PROPERTY);
        if (checkIntervalString == null) {
            return DEFAULT_CHECK_INTERVAL_SECONDS;
        }

        try {
            return Integer.parseInt(checkIntervalString);
        } catch (NumberFormatException nfe) {
            LOG.warnf("Could not parse check interval seconds from string %s", checkIntervalString);
            return DEFAULT_CHECK_INTERVAL_SECONDS;
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(Integer.MAX_VALUE);

        builder.property()
                .name(BLACKLISTS_FALSE_POSITIVE_PROBABILITY_PROPERTY)
                .type("string")
                .helpText("False positive probability of the bloom filter to reject a valid password.")
                .defaultValue(df.format(DEFAULT_FALSE_POSITIVE_PROBABILITY))
                .add();

        builder.property()
                .name(CHECK_INTERVAL_SECONDS_PROPERTY)
                .type("string")
                .helpText("Interval in number of seconds when the server should check the password file for changes and reload it. Set to 0 to disable reloading.")
                .defaultValue(DEFAULT_CHECK_INTERVAL_SECONDS)
                .add();

        return builder.build();
    }

    /**
     * Builds a pre-computed Bloom filter (.bloom) file from a plaintext password denylist file.
     * Each line is treated as one password (lowercased before insertion).
     *
     * @param inputFile  path to the plaintext password list (one password per line, UTF-8)
     * @param outputFile path for the generated .bloom file
     * @param fpp        desired false-positive probability (e.g. 0.0001)
     * @throws IOException if the input file cannot be read or the output file cannot be written
     */
    public static void buildBloomFile(Path inputFile, Path outputFile, double fpp) throws IOException {
        long count;
        try (var lines = Files.lines(inputFile, StandardCharsets.UTF_8)) {
            count = lines.count();
        }
        BloomFilter<String> filter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), Math.max(count, 1), fpp);
        try (var lines = Files.lines(inputFile, StandardCharsets.UTF_8)) {
            lines.map(s -> s.toLowerCase(Locale.ROOT)).forEach(filter::put);
        }
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
            filter.writeTo(out);
        }
        LOG.infof("Built pre-computed denylist: input=%s passwords=%d fpp=%f output=%s",
                inputFile, count, fpp, outputFile);
    }

    /**
     * A {@link PasswordDenylist} describes a list of too easy to guess
     * or potentially leaked passwords that users should not be able to use.
     */
    public interface PasswordDenylist {


        /**
         * @return the logical name of the {@link PasswordDenylist}
         */
        String getName();

        /**
         * Checks whether a given {@code password} is contained in this {@link PasswordDenylist}.
         *
         * @param password
         * @return
         */
        boolean contains(String password);
    }

    /**
     * A {@link FileBasedPasswordDenylist} uses password-denylist files
     * to construct a {@link PasswordDenylist}.
     * <p>
     * This implementation uses a dynamically sized {@link BloomFilter}
     * with a provided default false positive probability.
     *
     * @see BloomFilter
     */
    public static class FileBasedPasswordDenylist implements PasswordDenylist {

        private static final int BUFFER_SIZE_IN_BYTES = 512 * 1024;

        /**
         * The name of the denylist filename.
         */
        private final String name;

        /**
         * The concrete path to the password-denylist file.
         */
        private final Path path;

        private final double falsePositiveProbability;

        private volatile BloomFilter<String> denylist;

        private final long checkIntervalMillis;

        private volatile long lastCheckedMillis;

        private long lastModifiedMillis;

        private long lastSizeBytes;

        public FileBasedPasswordDenylist(Path denylistBasePath, String name, double falsePositiveProbability, long checkIntervalMillis) {

            if (name.contains("/")) {
                // disallow '/' to avoid accidental filesystem traversal
                throw new IllegalArgumentException("" + name + " must not contain slashes!");
            }

            this.name = name;
            this.path = denylistBasePath.resolve(name);
            this.falsePositiveProbability = falsePositiveProbability;
            this.checkIntervalMillis = checkIntervalMillis;

            if (!Files.exists(this.path)) {
                throw new IllegalArgumentException("Password denylist " + name + " not found!");
            }

            this.lastModifiedMillis = path.toFile().lastModified();
            this.lastSizeBytes = path.toFile().length();
            this.denylist = load();
            this.lastCheckedMillis = System.currentTimeMillis();
        }

        public String getName() {
            return name;
        }

        public double getFalsePositiveProbability() {
            return falsePositiveProbability;
        }

        public boolean contains(String password) {
            reloadIfNeeded();
            return denylist.mightContain(password);
        }

        /**
         * Check the modification time and file size and reload if it has changed since the last load.
         * Uses double-checked locking to avoid redundant reloads by concurrent threads.
         */
        private void reloadIfNeeded() {
            if (checkIntervalMillis == 0) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastCheckedMillis < checkIntervalMillis) {
                return;
            }
            synchronized (this) {
                now = System.currentTimeMillis();
                if (now - lastCheckedMillis < checkIntervalMillis) {
                    return;
                }
                try {
                    long currentModified = Files.getLastModifiedTime(path).toMillis();
                    long currentSize = Files.size(path);
                    if (currentModified != lastModifiedMillis || currentSize != lastSizeBytes) {
                        denylist = load();
                        lastModifiedMillis = currentModified;
                        lastSizeBytes = currentSize;
                    }
                } catch (Exception e) {
                    LOG.warnf("Failed to reload denylist %s, continuing with cached version: %s", name, e.getMessage());
                }
                lastCheckedMillis = now;
            }
        }

        /**
         * Loads the denylist into a {@link BloomFilter}.
         * If the configured file ends with {@code .bloom}, it is loaded as a pre-computed Bloom filter binary.
         * Otherwise, it is read as a plaintext password list.
         *
         * @return the {@link BloomFilter} backing a password denylist
         */
        private BloomFilter<String> load() {
            if (name.endsWith(".bloom")) {
                return loadFromBloom();
            }
            return loadFromPlaintext();
        }

        /**
         * Fast path: deserialise a pre-computed Bloom filter binary (.bloom).
         * Emits a warning when the stored false-positive probability differs from the configured value.
         *
         * @return the deserialised {@link BloomFilter}
         * @throws IOException if the binary file cannot be read
         */
        private BloomFilter<String> loadFromBloom() {
            try {
                LOG.infof("Loading pre-computed denylist start: name=%s path=%s", name, path);
                long loadStartMillis = System.currentTimeMillis();
                BloomFilter<String> filter;
                try (BufferedInputStream in = new BufferedInputStream(
                        Files.newInputStream(path), BUFFER_SIZE_IN_BYTES)) {
                    filter = BloomFilter.readFrom(in, Funnels.stringFunnel(StandardCharsets.UTF_8));
                }
                long loadTimeMillis = System.currentTimeMillis() - loadStartMillis;
                LOG.infof("Loading pre-computed denylist finished: name=%s path=%s expectedFpp=%s loadTime=%dms",
                        name, path, filter.expectedFpp(), loadTimeMillis);
                if (Math.abs(filter.expectedFpp() - falsePositiveProbability) > 1e-9) {
                    LOG.warnf("Pre-computed denylist '%s' has fpp=%.6f but configured fpp=%.6f. "
                            + "Regenerate the .bloom file with 'kc.sh tools build-password-denylist' if this is unintended.",
                            name, filter.expectedFpp(), falsePositiveProbability);
                }
                return filter;
            } catch (IOException e) {
                throw new RuntimeException("Loading pre-computed denylist failed: path=" + path, e);
            }
        }

        /**
         * Slow path: build a BloomFilter from the plaintext denylist file.
         * Requires two passes: one to count passwords, one to insert them.
         *
         * @return a newly constructed {@link BloomFilter} populated from the plaintext file
         */
        private BloomFilter<String> loadFromPlaintext() {
            try {
                LOG.infof("Loading denylist start: name=%s path=%s", name, path);
                long loadStartMillis = System.currentTimeMillis();

                long passwordCount = countPasswordsInDenylistFile();
                double fpp = getFalsePositiveProbability();

                BloomFilter<String> filter = BloomFilter.create(
                        Funnels.stringFunnel(StandardCharsets.UTF_8),
                        passwordCount,
                        fpp);

                insertPasswordsInto(filter);

                double expectedFfp = filter.expectedFpp();
                long loadTimeMillis = System.currentTimeMillis() - loadStartMillis;
                LOG.infof("Loading denylist finished: name=%s passwords=%s path=%s falsePositiveProbability=%s expectedFalsePositiveProbability=%s loadTime=%dms",
                        name, passwordCount, path, fpp, expectedFfp, loadTimeMillis);

                return filter;
            } catch (IOException e) {
                throw new RuntimeException("Loading denylist failed: Could not load password denylist path=" + path, e);
            }
        }

        protected void insertPasswordsInto(BloomFilter<String> filter) throws IOException {
            try (BufferedReader br = newReader(path)) {
                br.lines().map(String::toLowerCase).forEach(filter::put);
            }
        }

        /**
         * Determines password denylist size to correctly size the {@link BloomFilter} backing this denylist.
         *
         * @return number of passwords found in the denylist file
         * @throws IOException
         */
        private long countPasswordsInDenylistFile() throws IOException {

            /*
             * TODO find a more efficient way to determine the password count,
             * e.g. require a header-line in the password-denylist file
             */
            try (BufferedReader br = newReader(path)) {
                return br.lines().count();
            }
        }

        private static BufferedReader newReader(Path path) throws IOException {
            return new BufferedReader(Files.newBufferedReader(path), BUFFER_SIZE_IN_BYTES);
        }

        /**
         * Discovers password denylists location.
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
         * @return the detected denylist path
         * @throws IllegalStateException if no denylist folder could be detected
         */
        private static Path detectDenylistsBasePath(Config.Scope config, Supplier<String> defaultPathSupplier) {

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
                    LOG.errorf("Could not create folder for password denylists: %s", pathFromJbossDataPath);
                }
            }
            return ensureExists(Paths.get(pathFromJbossDataPath));
        }

        private static Path ensureExists(Path path) {

            Objects.requireNonNull(path, "path");

            if (Files.exists(path)) {
                return path;
            }

            throw new IllegalStateException("Password denylists location does not exist: " + path);
        }
    }
}
